package com.walletledger.service;

import com.walletledger.dto.TransferRequest;
import com.walletledger.dto.TransferResponse;
import com.walletledger.exception.InsufficientBalanceException;
import com.walletledger.exception.InvalidTransferException;
import com.walletledger.exception.WalletNotFoundException;
import com.walletledger.model.LedgerEntry;
import com.walletledger.model.Wallet;
import com.walletledger.model.enums.LedgerEntryType;
import com.walletledger.repository.LedgerEntryRepository;
import com.walletledger.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Core transfer engine implementing double-entry atomic ledger semantics.
 *
 * <h3>Concurrency Strategy</h3>
 * <ul>
 *   <li>Uses DB-level pessimistic locking (SELECT ... FOR UPDATE) to prevent race conditions
 *       and lost updates when concurrent threads modify the same wallet balances.</li>
 *   <li>Always acquires locks in deterministic wallet ID order (smaller ID first) to
 *       categorically prevent deadlocks.</li>
 * </ul>
 *
 * <h3>Double-Entry Accounting</h3>
 * Every transfer atomically creates exactly two {@link LedgerEntry} records — a DEBIT
 * (money leaving the source) and a CREDIT (money arriving at the destination). This
 * guarantees an auditable trail where the sum of all ledger movements for any wallet
 * equals its current balance.
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    public TransferService(WalletRepository walletRepository, LedgerEntryRepository ledgerEntryRepository) {
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    /**
     * Executes a peer-to-peer fund transfer between two wallets inside a single
     * transactional boundary with pessimistic locking.
     *
     * @param request contains sourceWalletId, destinationWalletId, amount, and optional description
     * @return TransferResponse with transaction reference, updated balances, and status
     * @throws InvalidTransferException    if source == destination or amount is non-positive
     * @throws WalletNotFoundException     if either wallet does not exist
     * @throws InsufficientBalanceException if the source wallet lacks sufficient funds
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransferResponse executeTransfer(TransferRequest request) {
        Long sourceId = request.getSourceWalletId();
        Long destinationId = request.getDestinationWalletId();
        BigDecimal amount = request.getAmount();

        log.debug("Initiating transfer of {} from wallet {} to wallet {}", amount, sourceId, destinationId);

        // ──────────────────────────────────────────────────────────────────────
        // Step 1: Validate the transfer request
        // ──────────────────────────────────────────────────────────────────────
        if (sourceId.equals(destinationId)) {
            throw new InvalidTransferException("Source and destination wallets must be different. Self-transfers are not allowed.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Transfer amount must be strictly positive.");
        }

        // ──────────────────────────────────────────────────────────────────────
        // Step 2: Deterministic Lock Ordering
        // ──────────────────────────────────────────────────────────────────────
        // CRITICAL: Deterministic lock ordering prevents deadlock cycles.
        // If Thread A transfers Wallet 1→2 and Thread B transfers Wallet 2→1,
        // both threads will lock Wallet 1 first, then Wallet 2, serializing
        // access instead of deadlocking.
        Long firstLockId = sourceId < destinationId ? sourceId : destinationId;
        Long secondLockId = sourceId < destinationId ? destinationId : sourceId;

        // ──────────────────────────────────────────────────────────────────────
        // Step 3: Acquire Pessimistic Write Locks (SELECT ... FOR UPDATE)
        // ──────────────────────────────────────────────────────────────────────
        // Each call issues SELECT ... FOR UPDATE, blocking any concurrent
        // transaction that tries to read-and-lock the same row until we commit.
        log.debug("Acquiring pessimistic locks in deterministic order: first={}, second={}", firstLockId, secondLockId);

        Wallet firstLockedWallet = walletRepository.findByIdWithPessimisticLock(firstLockId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with ID: " + firstLockId));
        Wallet secondLockedWallet = walletRepository.findByIdWithPessimisticLock(secondLockId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with ID: " + secondLockId));

        // Map the locked entities back to their logical roles (source vs. destination).
        Wallet source = firstLockId.equals(sourceId) ? firstLockedWallet : secondLockedWallet;
        Wallet destination = firstLockId.equals(destinationId) ? firstLockedWallet : secondLockedWallet;

        // ──────────────────────────────────────────────────────────────────────
        // Step 4: Balance Sufficiency Check
        // ──────────────────────────────────────────────────────────────────────
        // This check happens after acquiring the lock, so we are guaranteed to
        // read the latest committed balance — no phantom reads.
        if (source.getBalance().compareTo(amount) < 0) {
            log.warn("Insufficient balance in wallet {}. Available: {}, Requested: {}",
                    sourceId, source.getBalance(), amount);
            throw new InsufficientBalanceException(
                    "Wallet " + sourceId + " has insufficient balance. Available: "
                    + source.getBalance() + ", Requested: " + amount);
        }

        // ──────────────────────────────────────────────────────────────────────
        // Step 5: Generate a unique transaction reference (groups the DEBIT+CREDIT pair)
        // ──────────────────────────────────────────────────────────────────────
        String transactionReference = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        log.debug("Generated transaction reference: {}", transactionReference);

        // ──────────────────────────────────────────────────────────────────────
        // Step 6: Execute the Double-Entry Transfer
        // ──────────────────────────────────────────────────────────────────────
        // DOUBLE-ENTRY ACCOUNTING: Every transfer creates exactly two ledger
        // entries — a DEBIT (money leaving) on the source wallet and a CREDIT
        // (money arriving) on the destination wallet. The sum of all ledger
        // entry amounts for any wallet must always equal its current balance.

        // Snapshot balances before mutation
        BigDecimal sourceBalanceBefore = source.getBalance();
        BigDecimal destinationBalanceBefore = destination.getBalance();

        // Mutate balances atomically within this transaction
        source.setBalance(source.getBalance().subtract(amount));
        destination.setBalance(destination.getBalance().add(amount));

        // Persist updated wallet balances (flush happens at commit)
        walletRepository.save(source);
        walletRepository.save(destination);

        // Create DEBIT ledger entry for the source wallet (money leaving)
        LedgerEntry debitEntry = LedgerEntry.builder()
                .transactionReference(transactionReference)
                .walletId(sourceId)
                .entryType(LedgerEntryType.DEBIT)
                .amount(amount)
                .balanceBefore(sourceBalanceBefore)
                .balanceAfter(source.getBalance())
                .counterpartWalletId(destinationId)
                .description(request.getDescription())
                .build();

        // Create CREDIT ledger entry for the destination wallet (money arriving)
        LedgerEntry creditEntry = LedgerEntry.builder()
                .transactionReference(transactionReference)
                .walletId(destinationId)
                .entryType(LedgerEntryType.CREDIT)
                .amount(amount)
                .balanceBefore(destinationBalanceBefore)
                .balanceAfter(destination.getBalance())
                .counterpartWalletId(sourceId)
                .description(request.getDescription())
                .build();

        // Persist both ledger entries — they share the same transactionReference
        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);

        log.info("Transfer {} completed: {} units from wallet {} → wallet {}",
                transactionReference, amount, sourceId, destinationId);

        // ──────────────────────────────────────────────────────────────────────
        // Step 7: Build and return the TransferResponse
        // ──────────────────────────────────────────────────────────────────────
        return TransferResponse.builder()
                .transactionReference(transactionReference)
                .sourceWalletId(sourceId)
                .destinationWalletId(destinationId)
                .amount(amount)
                .sourceBalanceAfter(source.getBalance())
                .destinationBalanceAfter(destination.getBalance())
                .status("COMPLETED")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
