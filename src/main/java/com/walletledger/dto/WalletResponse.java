package com.walletledger.dto;

import com.walletledger.model.Wallet;
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
public class WalletResponse {
    private Long id;
    private String ownerName;
    private BigDecimal balance;
    private String currency;
    private LocalDateTime createdAt;

    public static WalletResponse from(Wallet wallet) {
        if (wallet == null) {
            return null;
        }
        return WalletResponse.builder()
                .id(wallet.getId())
                .ownerName(wallet.getOwnerName())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .createdAt(wallet.getCreatedAt())
                .build();
    }
}
