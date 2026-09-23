package com.walletledger.model.enums;

/**
 * Represents the status of an idempotent request.
 */
public enum IdempotencyStatus {
    /**
     * The request is currently being processed.
     */
    PROCESSING,

    /**
     * The request has been successfully processed.
     */
    COMPLETED,

    /**
     * The request failed during processing.
     */
    FAILED
}
