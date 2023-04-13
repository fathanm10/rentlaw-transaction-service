package com.rentlaw.transactionservice.controller;

import com.rentlaw.transactionservice.model.Transaction;
import com.rentlaw.transactionservice.model.TransactionStatus;
import com.rentlaw.transactionservice.repository.TransactionRepository;
import com.rentlaw.transactionservice.service.EmailService;
import com.rentlaw.transactionservice.service.RabbitMQService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.sql.Timestamp;
import java.util.List;

@RestController
@RequestMapping("/")
public class EmailController {
    @Autowired
    private EmailService emailService;
    @Autowired
    private RabbitMQService rabbitMQService;
    @Autowired
    private TransactionRepository transactionRepository;

    @GetMapping("confirm/{orderId}")
    public ResponseEntity<String> confirmOrder(@PathVariable int orderId) {
        return ResponseEntity.ok(String.format("Order with order id %d has been successfully confirmed.", orderId));
    }

    @GetMapping("test")
    public ResponseEntity<String> testSendEmailMessage() throws MalformedURLException, MessagingException {
        String to = "example@gmail.com";
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

    @GetMapping("list/{user}")
    public List<Transaction> getListTransactions(@PathVariable String user) {
        return transactionRepository.findTransactionsByReceiver(user);
    }

    @GetMapping("send")
    public ResponseEntity<String> testSendEmailToBrokerAndReceive() {
        String sender = "Fathan";
        String receiver = "DummyReceiver";
        TransactionStatus status = TransactionStatus.PENDING;
        Transaction transaction = Transaction.builder()
                .sender(sender)
                .receiver(receiver)
                .status(status)
                .timestamp(new Timestamp(System.currentTimeMillis()))
                .build();
        transactionRepository.save(transaction);
        rabbitMQService.sendJsonMessage(transaction);
        return ResponseEntity.ok("transaction sent and saved to database");
    }
}
