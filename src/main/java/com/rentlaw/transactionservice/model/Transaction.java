package com.rentlaw.transactionservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Generated
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;
    private String sender;
    private String receiver;
    private Timestamp timestamp;
    private TransactionStatus status;
}
