package com.walletledger.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    @NotNull
    private Long sourceWalletId;

    @NotNull
    private Long destinationWalletId;

    // The transfer amount must be strictly positive
    @NotNull
    @DecimalMin(value="0.01", message="Transfer amount must be positive")
    private BigDecimal amount;

    private String description;
}
