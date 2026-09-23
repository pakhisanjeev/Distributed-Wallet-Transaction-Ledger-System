package com.walletledger.controller;

import com.walletledger.dto.CreateWalletRequest;
import com.walletledger.dto.TransferRequest;
import com.walletledger.dto.TransferResponse;
import com.walletledger.dto.WalletResponse;
import com.walletledger.model.LedgerEntry;
import com.walletledger.repository.LedgerEntryRepository;
import com.walletledger.service.TransferService;
import com.walletledger.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class WalletController {

    private static final Logger log = LoggerFactory.getLogger(WalletController.class);

    private final WalletService walletService;
    private final TransferService transferService;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    public WalletController(WalletService walletService, TransferService transferService, LedgerEntryRepository ledgerEntryRepository) {
        this.walletService = walletService;
        this.transferService = transferService;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    /**
     * Create a new wallet.
     */
    @PostMapping("/wallets")
    public ResponseEntity<WalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        log.info("Received request to create wallet");
        WalletResponse response = walletService.createWallet(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get a wallet by ID.
     */
    @GetMapping("/wallets/{id}")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable Long id) {
        log.info("Received request to get wallet with ID: {}", id);
        WalletResponse response = walletService.getWallet(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all wallets.
     */
    @GetMapping("/wallets")
    public ResponseEntity<List<WalletResponse>> getAllWallets() {
        log.info("Received request to get all wallets");
        List<WalletResponse> response = walletService.getAllWallets();
        return ResponseEntity.ok(response);
    }

    /**
     * Execute a transfer between two wallets.
     * Idempotency is handled by the IdempotencyInterceptor.
     */
    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> executeTransfer(
            @Valid @RequestBody TransferRequest transferRequest,
            HttpServletRequest request) {
        
        log.info("Received transfer request");
        
        // Execute transfer via TransferService
        TransferResponse transferResponse = transferService.executeTransfer(transferRequest);
        
        // Store the TransferResponse in request attribute "transferResponse"
        // This allows the IdempotencyInterceptor's afterCompletion method to read it
        // and cache the response payload for future idempotent requests.
        request.setAttribute("transferResponse", transferResponse);
        
        return ResponseEntity.ok(transferResponse);
    }

    /**
     * Get the ledger entries (transaction history) for a specific wallet.
     */
    @GetMapping("/wallets/{id}/ledger")
    public ResponseEntity<List<LedgerEntry>> getWalletLedger(@PathVariable Long id) {
        log.info("Received request to get ledger for wallet ID: {}", id);
        List<LedgerEntry> entries = ledgerEntryRepository.findByWalletIdOrderByCreatedAtDesc(id);
        return ResponseEntity.ok(entries);
    }
}
