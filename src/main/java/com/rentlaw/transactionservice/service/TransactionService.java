package com.rentlaw.transactionservice.service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentlaw.transactionservice.dto.AuthorizationDTO;
import com.rentlaw.transactionservice.dto.CreateTransactionDTO;
import com.rentlaw.transactionservice.dto.EditTransactionStatusDTO;
import com.rentlaw.transactionservice.model.Transaction;
import com.rentlaw.transactionservice.model.TransactionStatus;
import com.rentlaw.transactionservice.repository.TransactionRepository;

@Service
public class TransactionService {
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CloudinaryService cloudinaryService;
    @Value("${endpoint.auth.verify}")
    private String verifyUrl;
    @Value("${endpoint.host}")
    private String hostUrl;

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQService.class);
    private final SessionFactory sessionFactory;

    @Autowired
    public TransactionService(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Transaction createTransaction(CreateTransactionDTO createTransactionDTO) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            var user = verifyUser("Bearer " + createTransactionDTO.token);
            String sender;
            if (user == null) {
                sender = "ANONYMOUS";
            } else {
                sender = user.username;
            }
            TransactionStatus status = TransactionStatus.PENDING;
            transaction = Transaction.builder()
                    .productId(createTransactionDTO.productId)
                    .sender(sender)
                    .receiver(createTransactionDTO.receiver)
                    .status(status)
                    .timestamp(new Timestamp(System.currentTimeMillis()))
                    .amount(createTransactionDTO.amount)
                    .build();
            session.beginTransaction();
            session.persist(transaction);
            session.getTransaction().commit();
        }
        LOGGER.info(transaction.toString());
        return transaction;
    }

    public Transaction editTransactionStatus(EditTransactionStatusDTO editTransactionStatusDTO) {
        try (Session session = sessionFactory.openSession()) {
            var user = verifyUser("Bearer " + editTransactionStatusDTO.token);
            // Transaction transaction = transactionRepository.getReferenceById(editTransactionStatusDTO.id);
            var transaction = session.get(Transaction.class, editTransactionStatusDTO.id);
            if (transaction.getReceiver().equals(user.username) || user.username.equals("admin")) {
                transaction.setStatus(editTransactionStatusDTO.status);
            }
            LOGGER.info(transaction.toString());
            return transactionRepository.save(transaction);
        }
    }

    public void deleteTransaction(Long id) {
        try (Session session = sessionFactory.openSession()) {
            var transaction = session.get(Transaction.class, id);
            cloudinaryService.deleteImage(transaction.getImageId());
            session.beginTransaction();
            session.remove(transaction);
            session.getTransaction().commit();
            LOGGER.info("Removed transaction with id "+id);
        }
    }

    public AuthorizationDTO verifyUser(String Authorization) {
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
            var authorizationDTO = objectMapper.readValue(responseEntity.getBody(), AuthorizationDTO.class);
            LOGGER.info(authorizationDTO.toString());
            return authorizationDTO;
        } catch (Exception e) {
            return null;
        }
    }
}
