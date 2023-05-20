package com.rentlaw.transactionservice.repository;

import com.rentlaw.transactionservice.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Interface for CRUD Transaction entity.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findTransactionsByReceiver(String receiver);
    List<Transaction> findTransactionsBySender(String receiver);
    List<Transaction> findTransactionsBySenderOrReceiver(String sender, String receiver);
}
