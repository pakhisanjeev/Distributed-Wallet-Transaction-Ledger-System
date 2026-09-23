package com.walletledger.exception;

public class DuplicateTransactionException extends RuntimeException {
    public DuplicateTransactionException(String idempotencyKey) {
        super(String.format("Duplicate transaction detected for idempotency key: %s", idempotencyKey));
    }
}
