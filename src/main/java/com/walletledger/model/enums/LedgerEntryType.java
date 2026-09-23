package com.walletledger.model.enums;

/**
 * Defines the type of ledger entry in a double-entry accounting system.
 */
public enum LedgerEntryType {
    /**
     * Represents a deduction of funds from a wallet.
     */
    DEBIT,

    /**
     * Represents an addition of funds to a wallet.
     */
    CREDIT
}
