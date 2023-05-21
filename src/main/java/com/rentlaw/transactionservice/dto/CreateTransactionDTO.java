package com.rentlaw.transactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransactionDTO {
    public String token;
    public String receiver;
    public Long amount;
    public Long productId;
}
