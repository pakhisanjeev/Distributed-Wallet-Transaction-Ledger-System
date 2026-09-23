package com.walletledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {
    private String transactionReference;
    private Long sourceWalletId;
    private Long destinationWalletId;
    private BigDecimal amount;
    private BigDecimal sourceBalanceAfter;
    private BigDecimal destinationBalanceAfter;
    private String status;
    private LocalDateTime timestamp;
}
