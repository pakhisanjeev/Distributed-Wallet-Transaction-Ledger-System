package com.walletledger.exception;

public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException(Long walletId) {
        super(String.format("Wallet not found with ID: %d", walletId));
    }
}
