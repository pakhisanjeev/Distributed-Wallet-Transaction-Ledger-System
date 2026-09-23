-- Schema creation script for Wallet Ledger System

-- 1. Wallets table to store wallet balances
CREATE TABLE IF NOT EXISTS wallets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_name VARCHAR(255) NOT NULL,
    balance DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0, -- For JPA @Version optimistic locking
    CHECK (balance >= 0),
    INDEX idx_wallets_owner_name (owner_name)
);

-- 2. Ledger Entries table for double-entry accounting records
CREATE TABLE IF NOT EXISTS ledger_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_reference VARCHAR(64) NOT NULL, -- Groups the corresponding DEBIT and CREDIT entries
    wallet_id BIGINT NOT NULL, -- The wallet this entry applies to (Foreign Key to wallets.id)
    entry_type ENUM('DEBIT','CREDIT') NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    balance_before DECIMAL(19,4) NOT NULL, -- Snapshot of balance before the entry
    balance_after DECIMAL(19,4) NOT NULL,  -- Snapshot of balance after the entry
    counterpart_wallet_id BIGINT NOT NULL, -- The other wallet involved in the transaction
    description VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CHECK (amount > 0),
    INDEX idx_ledger_entries_tx_ref (transaction_reference),
    INDEX idx_ledger_entries_wallet_id (wallet_id)
);

-- 3. Idempotency Keys table to handle duplicate requests safely
CREATE TABLE IF NOT EXISTS idempotency_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    status ENUM('PROCESSING','COMPLETED','FAILED') NOT NULL DEFAULT 'PROCESSING',
    request_hash VARCHAR(256),
    response_payload TEXT,
    http_status_code INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_idempotency_keys_key (idempotency_key),
    INDEX idx_idempotency_keys_status_date (status, created_at)
);
