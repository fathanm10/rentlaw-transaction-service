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
public class TransactionDTO {
    public long id;
    public TransactionStatus status;
}
