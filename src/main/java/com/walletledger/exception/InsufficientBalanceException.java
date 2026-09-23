package com.walletledger.exception;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(Long walletId) {
        super(String.format("Wallet %d has insufficient balance for this transfer", walletId));
    }
}
