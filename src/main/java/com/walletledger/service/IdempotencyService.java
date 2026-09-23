package com.walletledger.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walletledger.model.IdempotencyKey;
import com.walletledger.model.enums.IdempotencyStatus;
import com.walletledger.repository.IdempotencyKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    @Autowired
    public IdempotencyService(IdempotencyKeyRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Registers a new idempotency key with PROCESSING status.
     * 
     * REQUIRES_NEW is critical here because this transaction must commit immediately
     * and independently of the main transfer transaction. If the main transaction rolls back
     * (e.g., due to insufficient balance), we still want to record that this idempotency key
     * was processed/failed so that we don't accidentally retry a failed business constraint.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyKey registerKey(String key, String requestHash) {
        try {
            IdempotencyKey idempotencyKey = new IdempotencyKey();
            idempotencyKey.setIdempotencyKey(key);
            idempotencyKey.setRequestHash(requestHash);
            idempotencyKey.setStatus(IdempotencyStatus.PROCESSING);
            idempotencyKey.setCreatedAt(LocalDateTime.now());
            idempotencyKey.setUpdatedAt(LocalDateTime.now());
            return repository.saveAndFlush(idempotencyKey);
        } catch (DataIntegrityViolationException e) {
            // Another thread might have inserted the same key concurrently
            return repository.findByIdempotencyKey(key)
                    .orElseThrow(() -> new IllegalStateException("Key constraint violation but key not found", e));
        }
    }

    /**
     * Marks an idempotency key as COMPLETED and stores the response payload.
     * Uses REQUIRES_NEW to ensure the status update commits independently.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(String key, Object responsePayload, int httpStatus) {
        repository.findByIdempotencyKey(key).ifPresent(idempotencyKey -> {
            try {
                String jsonResponse = objectMapper.writeValueAsString(responsePayload);
                idempotencyKey.setResponsePayload(jsonResponse);
                idempotencyKey.setHttpStatusCode(httpStatus);
                idempotencyKey.setStatus(IdempotencyStatus.COMPLETED);
                idempotencyKey.setUpdatedAt(LocalDateTime.now());
                repository.save(idempotencyKey);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize response payload", e);
            }
        });
    }

    /**
     * Marks an idempotency key as FAILED.
     * Uses REQUIRES_NEW to ensure the status update commits independently.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String key) {
        repository.findByIdempotencyKey(key).ifPresent(idempotencyKey -> {
            idempotencyKey.setStatus(IdempotencyStatus.FAILED);
            idempotencyKey.setUpdatedAt(LocalDateTime.now());
            repository.save(idempotencyKey);
        });
    }

    public Optional<IdempotencyKey> findByKey(String key) {
        return repository.findByIdempotencyKey(key);
    }
}
