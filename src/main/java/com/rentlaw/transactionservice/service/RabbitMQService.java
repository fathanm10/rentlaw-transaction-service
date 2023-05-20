package com.rentlaw.transactionservice.service;

import com.rentlaw.transactionservice.config.RabbitMQConfig;
import com.rentlaw.transactionservice.dto.TransactionDTO;
import com.rentlaw.transactionservice.model.Transaction;
import com.rentlaw.transactionservice.model.TransactionStatus;
import com.rentlaw.transactionservice.repository.TransactionRepository;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Handles all functionalities using message broker RabbitMQ.
 */
@Service
public class RabbitMQService {
    // for sending
    @Value("${rabbitmq.exchange.name}")
    private String exchange;
    @Value("${rabbitmq.routing.key}")
    private String routingKey;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private RabbitMQConfig rabbitMQConfig;

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQService.class);
    private final SessionFactory sessionFactory;

    @Autowired
    public RabbitMQService(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    /**
     * @param transaction Consumes message from message broker into a predefined model.
     */
    @RabbitListener(queues = {"${rabbitmq.queue.name}"})
    public void consume(Object message) {
        // consumed doesnt have to be in transaction model
        try (Session session = sessionFactory.openSession()) {
            if (message instanceof TransactionDTO transactionDTO) {
                LOGGER.info(transactionDTO.toString());
                // create transaction based on consumed transactionDTO
                Transaction transaction = session.get(Transaction.class, transactionDTO.id);
                transaction.setStatus(transactionDTO.status);
                transactionRepository.save(transaction);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to process RabbitMQ message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * @param transaction Sends an Entity class as a Json Message to message broker.
     */
    public void sendAnyObject(Object object) {
        System.out.println(object.toString());
        rabbitTemplate.convertAndSend(exchange, routingKey, object);
    }
    
    public void sendAnyObject(Object object, String exchange, String routingKey) {
        rabbitTemplate.convertAndSend(exchange, routingKey, object);
    }
    
    public void sendAnyObject(Object object, String exchange, String routingKey, String queue) {
        rabbitMQConfig.declareBinding(queue, exchange, routingKey);
        rabbitTemplate.convertAndSend(exchange, routingKey, object);
    }
}
