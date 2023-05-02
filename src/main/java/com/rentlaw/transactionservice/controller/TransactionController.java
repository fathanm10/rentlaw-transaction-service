package com.rentlaw.transactionservice.controller;

import com.rentlaw.transactionservice.model.Transaction;
import com.rentlaw.transactionservice.model.TransactionStatus;
import com.rentlaw.transactionservice.repository.TransactionRepository;
import com.rentlaw.transactionservice.service.CloudinaryService;
import com.rentlaw.transactionservice.service.EmailService;
import com.rentlaw.transactionservice.service.RabbitMQService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * Serves endpoints for using services regarding transactions.
 */
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class TransactionController {
    @Autowired
    private EmailService emailService;
    @Autowired
    private RabbitMQService rabbitMQService;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private CloudinaryService cloudinaryService;

    @GetMapping("/send-email/{to}")
    public ResponseEntity<String> testSendEmailMessage(@PathVariable String to) throws MalformedURLException, MessagingException {
        String subject = "RENTLAW - New Incoming Transaction";
        String domainUrl = "http://localhost:8080";
        String confirmUrl = domainUrl + "/confirm/1";
        String body =
                "<p>You have a new incoming transaction. Click the link down below to confirm the transaction.</p>" +
                        "<a href=\"" + confirmUrl + "\">Click here</a> ";
        String imageUrl = "https://i.pinimg.com/564x/b8/cb/bf/b8cbbf59e72d2bf2770ff827b0990bc8.jpg";
        emailService.sendEmail(to, subject, body, imageUrl);
        return ResponseEntity.ok("test message sent");
    }

    @GetMapping("/{user}")
    @Operation(
            description = "List Transactions"
    )
    public List<Transaction> getListUserTransactions(@PathVariable String user) {
        return transactionRepository.findTransactionsByReceiver(user);
    }

    @GetMapping("/all")
    @Operation(
            description = "All Transactions"
    )
    public List<Transaction> getListAllTransactions() {
        return transactionRepository.findAll();
    }

    @PostMapping(value = "/create", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @Operation(
            description = "Create Transaction"
    )
    public ResponseEntity<Transaction> createTransaction(@RequestParam String sender,
                                                         @RequestParam String receiver,
                                                         @RequestPart MultipartFile imageProof) {
        // TODO: Should verify user before continuing
        String imageId = cloudinaryService.uploadImage(imageProof);
        TransactionStatus status = TransactionStatus.PENDING;
        Transaction transaction = Transaction.builder()
                .sender(sender)
                .receiver(receiver)
                .status(status)
                .timestamp(new Timestamp(System.currentTimeMillis()))
                .imageId(imageId)
                .build();
        transaction = transactionRepository.save(transaction);
        return ResponseEntity.ok(transaction);
    }

    @PutMapping("/verify")
    @Operation(
            description = "Verify Transaction"
    )
    public ResponseEntity<Transaction> verifyTransaction(@RequestParam long id) {
        // TODO: Should verify user before continuing
        Transaction transaction = transactionRepository.getReferenceById(id);
        // TODO: if transaction.receiver == authorized user, continue
        transaction.setStatus(TransactionStatus.CONFIRMED);
        transaction = transactionRepository.save(transaction);
        return ResponseEntity.ok(transaction);
    }

    @PutMapping("/reject")
    @Operation(
            description = "Reject Transaction"
    )
    public ResponseEntity<Transaction> rejectTransaction(@RequestParam long id) {
        // TODO: Should verify user before continuing
        Transaction transaction = transactionRepository.getReferenceById(id);
        // TODO: if transaction.receiver == authorized user, continue
        transaction.setStatus(TransactionStatus.REJECTED);
        transaction = transactionRepository.save(transaction);
        return ResponseEntity.ok(transaction);
    }

    @DeleteMapping("/delete")
    @Operation(
            description = "Delete Transaction"
    )
    public ResponseEntity<String> deleteTransaction(@RequestParam long id) {
        // TODO: Should verify user before continuing
        Transaction transaction = transactionRepository.getReferenceById(id);
        // TODO: if transaction.receiver == authorized user, continue
        cloudinaryService.deleteImage(transaction.getImageId());
        transactionRepository.deleteById(id);
        return ResponseEntity.ok("OK");
    }
}
