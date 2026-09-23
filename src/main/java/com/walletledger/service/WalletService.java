package com.walletledger.service;

import com.walletledger.dto.CreateWalletRequest;
import com.walletledger.dto.WalletResponse;
import com.walletledger.exception.WalletNotFoundException;
import com.walletledger.model.Wallet;
import com.walletledger.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for wallet lifecycle operations: creation, retrieval, and listing.
 * Transfer logic is deliberately separated into TransferService for single-responsibility.
 */
@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;

    @Autowired
    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    /**
     * Creates a new wallet with the specified owner name and initial balance.
     * The initial balance is set from the request; if not provided, defaults to zero via @PrePersist.
     */
    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request) {
        log.info("Creating new wallet for owner: {}", request.getOwnerName());

        Wallet wallet = Wallet.builder()
                .ownerName(request.getOwnerName())
                .balance(request.getInitialBalance())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .build();

        Wallet saved = walletRepository.save(wallet);
        log.info("Wallet created successfully with ID: {}", saved.getId());

        return WalletResponse.from(saved);
    }

    /**
     * Retrieves a single wallet by ID. Throws WalletNotFoundException if not found.
     */
    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long walletId) {
        log.debug("Fetching wallet with ID: {}", walletId);
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with ID: " + walletId));
        return WalletResponse.from(wallet);
    }

    /**
     * Returns all wallets as response DTOs. Intended for admin/dashboard views.
     */
    @Transactional(readOnly = true)
    public List<WalletResponse> getAllWallets() {
        log.debug("Fetching all wallets");
        return walletRepository.findAll().stream()
                .map(WalletResponse::from)
                .collect(Collectors.toList());
    }
}
