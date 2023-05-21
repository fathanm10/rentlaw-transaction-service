package com.rentlaw.transactionservice.dto;

import com.rentlaw.transactionservice.model.TransactionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditTransactionStatusDTO {
    public String token;
    public Long id;
    public TransactionStatus status;
}
