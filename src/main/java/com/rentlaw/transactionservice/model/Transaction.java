package com.rentlaw.transactionservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

/**
 * Main Entity class that saves transaction details and will be saved to the database.
 */
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
    private Long productId;
    private String sender;
    private String receiver;
    private Timestamp timestamp;
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
    private Long amount;
    private String imageUrl;
    private String imageId;
}
