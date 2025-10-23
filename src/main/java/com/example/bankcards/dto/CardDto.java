package com.example.bankcards.dto;
import com.example.bankcards.entity.enums.CardStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CardDto {
    private Long id;
    private String number;
    private LocalDateTime expirationDate;
    private CardStatus status;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private String ownerUsername;
}

