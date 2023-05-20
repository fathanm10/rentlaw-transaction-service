package com.rentlaw.transactionservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentlaw.transactionservice.model.Transaction;
import com.rentlaw.transactionservice.model.TransactionDTO;
import com.rentlaw.transactionservice.model.TransactionStatus;
import com.rentlaw.transactionservice.model.User;
import com.rentlaw.transactionservice.repository.TransactionRepository;
import com.rentlaw.transactionservice.service.CloudinaryService;
import com.rentlaw.transactionservice.service.EmailService;
import com.rentlaw.transactionservice.service.RabbitMQService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private ObjectMapper objectMapper;
    @Value("${endpoint.auth.verify}")
    private String verifyUrl;
    @Value("${endpoint.host}")
    private String hostUrl;

    @GetMapping("/send-email/{to}")
    @Operation(
            summary = "Sends emails to specified email"
    )
    public ResponseEntity<String> testSendEmailMessage(@PathVariable String to) throws MalformedURLException, MessagingException {
        String subject = "RENTLAW - New Incoming Transaction";
        String confirmUrl = hostUrl + "/confirm/1";
        String body =
                "<p>You have a new incoming transaction. Click the link down below to confirm the transaction.</p>" +
                        "<a href=\"" + confirmUrl + "\">Click here</a> ";
        String imageUrl = "https://i.pinimg.com/564x/b8/cb/bf/b8cbbf59e72d2bf2770ff827b0990bc8.jpg";
        emailService.sendEmail(to, subject, body, imageUrl);
        return ResponseEntity.ok("test message sent");
    }

    @GetMapping("/sent/{user}")
    @Operation(
            summary = "Lists user transactions sent"
    )
    public List<Transaction> getListSentUserTransactions(@PathVariable String user) {
        return transactionRepository.findTransactionsBySender(user);
    }

    @GetMapping("/received/{user}")
    @Operation(
            summary = "Lists user transactions received"
    )
    public List<Transaction> getListReceivedUserTransactions(@PathVariable String user) {
        return transactionRepository.findTransactionsByReceiver(user);
    }

    @GetMapping("/")
    @Operation(
            summary = "Lists all transactions"
    )
    public List<Transaction> getListAllTransactions() {
        return transactionRepository.findAll();
    }

    @PostMapping(value = "/", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @Operation(
            summary = "Creates new transaction",
            security = { @SecurityRequirement(name = "bearer-key")}
    )
    public ResponseEntity createTransaction(@Parameter(hidden = true) @RequestHeader String Authorization,
                                            @RequestParam String receiver,
                                            @RequestParam long amount,
                                            @RequestPart MultipartFile imageProof) {
        User user = verifyUser(Authorization);
        String sender;
        if (user == null) {
            //     return new ResponseEntity<>("Request not authenticated" ,HttpStatus.BAD_REQUEST);
            sender = "ANONYMOUS";
        } else {
            sender = user.getUsername();
        }
        String imageUrl = cloudinaryService.uploadImage(imageProof);
        TransactionStatus status = TransactionStatus.PENDING;
        Transaction transaction = Transaction.builder()
                .sender(sender)
                .receiver(receiver)
                .status(status)
                .timestamp(new Timestamp(System.currentTimeMillis()))
                .amount(amount)
                .imageUrl(imageUrl)
                .build();
        transaction = transactionRepository.save(transaction);
        rabbitMQService.sendAnyObject("{\"status\": \"success\"}", "transaction", "upload-payment");
        return ResponseEntity.ok(transaction);
    }

    @PutMapping("/confirm")
    @Operation(
            summary = "Confirms a transaction",
            security = { @SecurityRequirement(name = "bearer-key")}
    )
    public ResponseEntity verifyTransaction(@Parameter(hidden = true) @RequestHeader String Authorization,
                                            @RequestParam long id) {
        // User user = verifyUser(Authorization);
        // if (!transaction.getReceiver().equals(user.getUsername()) || !user.getUsername().equals("admin")) {
        //     return new ResponseEntity<>("Authenticated user is not privileged", HttpStatus.BAD_REQUEST);
        // }
        TransactionDTO transactionDTO = TransactionDTO.builder().id(id).status(TransactionStatus.CONFIRMED).build();
        rabbitMQService.sendAnyObject(transactionDTO);
        return ResponseEntity.ok(transactionDTO);
    }

    @PutMapping("/reject")
    @Operation(
            summary = "Rejects a transaction",
            security = { @SecurityRequirement(name = "bearer-key")}
    )
    public ResponseEntity rejectTransaction(@Parameter(hidden = true) @RequestHeader String Authorization,
                                            @RequestParam long id) {
        // User user = verifyUser(Authorization);
        // if (!transaction.getReceiver().equals(user.getUsername()) || !user.getUsername().equals("admin")) {
        //     return new ResponseEntity<>("Authenticated user is not privileged", HttpStatus.BAD_REQUEST);
        // }
        TransactionDTO transactionDTO = TransactionDTO.builder().id(id).status(TransactionStatus.REJECTED).build();
        rabbitMQService.sendAnyObject(transactionDTO);
        return ResponseEntity.ok(transactionDTO);
    }

    @DeleteMapping("/")
    @Operation(
            summary = "Deletes a transaction",
            security = { @SecurityRequirement(name = "bearer-key")}
    )
    public ResponseEntity<String> deleteTransaction(@Parameter(hidden = true) @RequestHeader String Authorization,
                                                    @RequestParam long id) {
        User user = verifyUser(Authorization);
        Transaction transaction = transactionRepository.getReferenceById(id);
        if (!transaction.getReceiver().equals(user.getUsername()) || !user.getUsername().equals("admin")) {
            return new ResponseEntity<>("Authenticated user is not privileged", HttpStatus.BAD_REQUEST);
        }
        cloudinaryService.deleteImage(transaction.getImageUrl());
        transactionRepository.deleteById(id);
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/test/mq")
    @Operation(
            summary = "For testing message queue"
    )
    public ResponseEntity<String> testMessageQueue(@RequestParam String exchange,
                                                   @RequestParam String routingKey,
                                                   @RequestParam String message) {
        rabbitMQService.sendAnyObject(message, exchange, routingKey);
        return ResponseEntity.ok("Message sent");
    }

    public User verifyUser(String Authorization) {
        String token = Authorization.substring(7);
        RestTemplate restTemplate = new RestTemplate();

        // Set the request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Set the request body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("token", token);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        // Make the API request
        ResponseEntity<String> responseEntity;
        try {
            responseEntity = restTemplate.postForEntity(verifyUrl, requestEntity, String.class);
            // Body should be json that is mappable to User class
            // TODO: Make sure mapping is right
            return objectMapper.readValue(responseEntity.getBody(), User.class);
        } catch (Exception e) {
            return null;
        }
    }
}
