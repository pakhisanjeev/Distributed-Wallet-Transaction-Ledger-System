package com.walletledger.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walletledger.model.IdempotencyKey;
import com.walletledger.model.enums.IdempotencyStatus;
import com.walletledger.service.IdempotencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;

@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Autowired
    public IdempotencyInterceptor(IdempotencyService idempotencyService, ObjectMapper objectMapper) {
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    /**
     * Intercepts incoming requests to enforce idempotency semantics.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Only intercept POST requests to paths containing "/transfers"
        if (!"POST".equalsIgnoreCase(request.getMethod()) || !request.getRequestURI().contains("/transfers")) {
            return true;
        }

        String idempotencyKeyHeader = request.getHeader("X-Idempotency-Key");
        if (idempotencyKeyHeader == null || idempotencyKeyHeader.trim().isEmpty()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.getWriter().write("Missing X-Idempotency-Key header");
            return false;
        }

        // Exactly-once semantics flow
        Optional<IdempotencyKey> existingKey = idempotencyService.findByKey(idempotencyKeyHeader);
        if (existingKey.isPresent()) {
            IdempotencyKey key = existingKey.get();
            if (key.getStatus() == IdempotencyStatus.COMPLETED) {
                // Return cached response directly
                response.setStatus(key.getHttpStatusCode() != null ? key.getHttpStatusCode() : HttpStatus.OK.value());
                response.setContentType("application/json");
                response.getWriter().write(key.getResponsePayload());
                return false; // Short-circuit, don't execute controller
            } else if (key.getStatus() == IdempotencyStatus.PROCESSING) {
                // Conflict, another request is already processing
                response.setStatus(HttpStatus.CONFLICT.value());
                response.getWriter().write("Request is already being processed");
                return false;
            }
            // If FAILED, we allow retry by proceeding to register/overwrite (or we can just proceed and the controller handles it)
        }

        // Register new key
        // In a real application, request hash should be computed from the payload to ensure identical requests
        String requestHash = "hash-placeholder"; 
        idempotencyService.registerKey(idempotencyKeyHeader, requestHash);
        
        // Store key in request attributes for afterCompletion hook
        request.setAttribute("idempotencyKey", idempotencyKeyHeader);
        return true;
    }

    /**
     * The afterCompletion hook ensures idempotency state is updated regardless of success or failure.
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String idempotencyKey = (String) request.getAttribute("idempotencyKey");
        if (idempotencyKey != null) {
            if (response.getStatus() >= 200 && response.getStatus() < 300 && ex == null) {
                // Success: update state to COMPLETED and save response payload
                Object transferResponse = request.getAttribute("transferResponse");
                idempotencyService.markCompleted(idempotencyKey, transferResponse, response.getStatus());
            } else {
                // Failure: update state to FAILED
                idempotencyService.markFailed(idempotencyKey);
            }
        }
    }
}
