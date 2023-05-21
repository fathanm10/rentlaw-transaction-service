package com.rentlaw.transactionservice.controller;

import com.rentlaw.transactionservice.dto.CreateTransactionDTO;
import com.rentlaw.transactionservice.dto.EditTransactionStatusDTO;
import com.rentlaw.transactionservice.model.Transaction;
import com.rentlaw.transactionservice.model.TransactionStatus;
import com.rentlaw.transactionservice.repository.TransactionRepository;
import com.rentlaw.transactionservice.service.CloudinaryService;
import com.rentlaw.transactionservice.service.EmailService;
import com.rentlaw.transactionservice.service.RabbitMQService;
import com.rentlaw.transactionservice.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Serves endpoints for using services regarding transactions.
 */
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/transaction")
public class TransactionController {
    @Autowired
    private EmailService emailService;
    @Autowired
    private RabbitMQService rabbitMQService;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private CloudinaryService cloudinaryService;
    @Autowired
    private TransactionService transactionService;
    @Value("${rabbitmq.orchestrator.exchange.name}")
    private String exchangeOrchestrator;
    @Value("${rabbitmq.orchestrator.routing.key}")
    private String routingKeyOrchestrator;

    @GetMapping("/sent/")
    @Operation(summary = "Lists user transactions sent", security = { @SecurityRequirement(name = "bearer-key") })
    public List<Transaction> getListSentUserTransactions(
            @Parameter(hidden = true) @RequestHeader String Authorization) {
        var user = transactionService.verifyUser(Authorization);
        if (user.username.equals("admin")) {
            return transactionRepository.findAll();
        }
        return transactionRepository.findTransactionsBySender(user.username);
    }

    @GetMapping("/received/")
    @Operation(summary = "Lists user transactions received", security = { @SecurityRequirement(name = "bearer-key") })
    public List<Transaction> getListReceivedUserTransactions(
            @Parameter(hidden = true) @RequestHeader String Authorization) {
        var user = transactionService.verifyUser(Authorization);
        if (user.username.equals("admin")) {
            return transactionRepository.findAll();
        }
        return transactionRepository.findTransactionsByReceiver(user.username);
    }

    @GetMapping("/")
    @Operation(summary = "Lists all / user transactions", security = { @SecurityRequirement(name = "bearer-key") })
    public List<Transaction> getListAllTransactions(@Parameter(hidden = true) @RequestHeader String Authorization) {
        var user = transactionService.verifyUser(Authorization);
        if (user.username.equals("admin")) {
            return transactionRepository.findAll();
        }
        return transactionRepository.findTransactionsBySenderOrReceiver(user.username, user.username);
    }

    @PostMapping(value = "/", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    @Operation(summary = "Creates new transaction", security = { @SecurityRequirement(name = "bearer-key") })
    public ResponseEntity createTransaction(@Parameter(hidden = true) @RequestHeader String Authorization,
            @RequestParam String receiver,
            @RequestParam long amount,
            @RequestParam long productId) {
        var user = transactionService.verifyUser(Authorization);
        CreateTransactionDTO createTransactionDTO = CreateTransactionDTO.builder()
                .token(Authorization.substring(7))
                .receiver(receiver)
                .amount(amount)
                .productId(productId)
                .build();
        return ResponseEntity.ok(transactionService.createTransaction(createTransactionDTO));
    }

    @PutMapping(value = "/proof/", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    @Operation(summary = "Adds payment proof to a transaction", security = {
            @SecurityRequirement(name = "bearer-key") })
    public ResponseEntity addTransactionProof(@Parameter(hidden = true) @RequestHeader String Authorization,
            @RequestParam long id,
            @RequestPart MultipartFile imageProof) {
        
        @Data
        @NoArgsConstructor
        class Message {
            String status = "success";
        }
        
        var user = transactionService.verifyUser(Authorization);
        var transaction = transactionRepository.getReferenceById(id);
        if (transaction.getSender().equals(user.username) || user.username.equals("admin")) {
            String imageUrl = cloudinaryService.uploadImage(imageProof);
            transaction.setImageUrl(imageUrl);
            rabbitMQService.sendAnyObject(new Message(), exchangeOrchestrator, routingKeyOrchestrator);
            return ResponseEntity.ok(transactionRepository.save(transaction));
        }
        return ResponseEntity.badRequest().body("Upload proof failed");
    }

    @PutMapping("/confirm")
    @Operation(summary = "Confirms a transaction", security = { @SecurityRequirement(name = "bearer-key") })
    public ResponseEntity verifyTransaction(@Parameter(hidden = true) @RequestHeader String Authorization,
            @RequestParam long id) {
        var user = transactionService.verifyUser(Authorization);
        Transaction transaction = transactionRepository.getReferenceById(id);
        if (transaction.getReceiver().equals(user.username) || user.username.equals("admin")) {
            var editTransactionStatusDTO = EditTransactionStatusDTO.builder().id(id).status(TransactionStatus.CONFIRMED)
                    .build();
            return ResponseEntity.ok(transactionService.editTransactionStatus(editTransactionStatusDTO));
        }
        return new ResponseEntity<>("Authenticated user is not privileged", HttpStatus.BAD_REQUEST);
    }

    @PutMapping("/reject")
    @Operation(summary = "Rejects a transaction", security = { @SecurityRequirement(name = "bearer-key") })
    public ResponseEntity rejectTransaction(@Parameter(hidden = true) @RequestHeader String Authorization,
            @RequestParam long id) {
        var user = transactionService.verifyUser(Authorization);
        Transaction transaction = transactionRepository.getReferenceById(id);
        if (transaction.getReceiver().equals(user.username) || user.username.equals("admin")) {
            var editTransactionStatusDTO = EditTransactionStatusDTO.builder().id(id).status(TransactionStatus.REJECTED)
                    .build();
            return ResponseEntity.ok(transactionService.editTransactionStatus(editTransactionStatusDTO));
        }
        return new ResponseEntity<>("Authenticated user is not privileged", HttpStatus.BAD_REQUEST);
    }

    @DeleteMapping("/")
    @Operation(summary = "Deletes a transaction", security = { @SecurityRequirement(name = "bearer-key") })
    public ResponseEntity<String> deleteTransaction(@Parameter(hidden = true) @RequestHeader String Authorization,
            @RequestParam long id) {
        var user = transactionService.verifyUser(Authorization);
        Transaction transaction = transactionRepository.getReferenceById(id);
        if (transaction.getReceiver().equals(user.username) || user.username.equals("admin")) {
            cloudinaryService.deleteImage(transaction.getImageUrl());
            transactionRepository.deleteById(id);
            return ResponseEntity.ok("Transaction Deleted");
        }
        return new ResponseEntity<>("Authenticated user is not privileged", HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/test/mq")
    @Operation(summary = "For testing message queue")
    public ResponseEntity<String> testMessageQueue(@RequestParam String exchange,
            @RequestParam String routingKey,
            @RequestParam String message,
            @RequestParam String queue) {
        rabbitMQService.sendAnyObject(message, exchange, routingKey, queue);
        return ResponseEntity.ok("Message sent");
    }
}
