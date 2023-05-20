package com.rentlaw.transactionservice.model;

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
