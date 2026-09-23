package com.walletledger.repository;

import com.walletledger.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /**
     * Finds a Wallet by ID and acquires a pessimistic write lock on the database row.
     * 
     * Why pessimistic locking is used here:
     * This prevents race conditions in concurrent transfers. When multiple threads
     * attempt to modify the same wallet balance concurrently, we must ensure they
     * read the most up-to-date balance and lock the row so no other transaction
     * can modify it until the current transaction commits.
     * 
     * This translates to a 'SELECT ... FOR UPDATE' statement in MySQL and PostgreSQL.
     * 
     * IMPORTANT: Callers must always acquire locks in a deterministic order
     * (e.g., sorting wallet IDs and locking the smallest ID first) when locking
     * multiple wallets to prevent deadlocks.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdWithPessimisticLock(@Param("id") Long id);
}
