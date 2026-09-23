package com.walletledger.model;

import com.walletledger.model.enums.LedgerEntryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a single entry (Debit or Credit) in the ledger.
 * A complete transaction consists of at least two LedgerEntries (one DEBIT, one CREDIT)
 * grouped by the same transactionReference, adhering to double-entry accounting principles.
 */
@Entity
@Table(name = "ledger_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * A unique reference linking corresponding DEBIT and CREDIT entries of a single transaction.
     */
    @Column(name = "transaction_reference", nullable = false, length = 64)
    private String transactionReference;

    /**
     * The ID of the wallet this entry belongs to.
     */
    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    /**
     * Whether this entry is a DEBIT or CREDIT.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private LedgerEntryType entryType;

    /**
     * The transaction amount. Should always be positive.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /**
     * Snapshot of the wallet's balance before this entry was applied.
     */
    @Column(name = "balance_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceBefore;

    /**
     * Snapshot of the wallet's balance after this entry was applied.
     */
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    /**
     * The ID of the other wallet involved in the transaction.
     */
    @Column(name = "counterpart_wallet_id", nullable = false)
    private Long counterpartWalletId;

    /**
     * Optional description or reason for the transaction.
     */
    @Column(length = 512)
    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
