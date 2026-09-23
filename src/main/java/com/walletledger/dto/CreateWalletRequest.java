package com.walletledger.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
public class CreateWalletRequest {
    @NotBlank
    private String ownerName;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal initialBalance;

    @Builder.Default
    private String currency = "USD";
}
