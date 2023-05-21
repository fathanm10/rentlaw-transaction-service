package com.rentlaw.transactionservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentlaw.transactionservice.config.RabbitMQConfig;
import com.rentlaw.transactionservice.dto.CreateTransactionDTO;
import com.rentlaw.transactionservice.dto.EditTransactionStatusDTO;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Service;

/**
 * Handles all functionalities using message broker RabbitMQ.
 */
@Service
public class RabbitMQService {
    // for sending
    @Value("${rabbitmq.exchange.transaction}")
    private String exchange;
    @Value("${rabbitmq.routingkey.status.transaction}")
    private String routingKey;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RabbitMQConfig rabbitMQConfig;
    @Autowired
    private TransactionService transactionService;

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQService.class);
    private final SessionFactory sessionFactory;

    @Autowired
    public RabbitMQService(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * @param transaction Consumes message from message broker into a predefined
     *                    model.
     */
    @RabbitListener(queues = { "${rabbitmq.queue.status.transaction}" })
    public void consumeStatusTransaction(EditTransactionStatusDTO editTransactionStatusDTO,
            @Headers Map<String, Object> headers) {
        try {
            if (editTransactionStatusDTO != null) {
                transactionService.editTransactionStatus(editTransactionStatusDTO);
            }
        } catch (Exception e) {
            LOGGER.info(e.toString());
        }
    }

    /**
     * @param transaction Consumes message from message broker into a predefined
     *                    model.
     */
    @RabbitListener(queues = { "${rabbitmq.queue.create.transaction}" })
    public void consumeCreateTransaction(CreateTransactionDTO createTransactionDTO,
            @Headers Map<String, Object> headers) {
        try {
            if (createTransactionDTO != null) {
                transactionService.createTransaction(createTransactionDTO);
            }
        } catch (Exception e) {
            LOGGER.info(e.toString());
        }
    }

    /**
     * @param transaction Consumes message from message broker into a predefined
     *                    model.
     */
    @RabbitListener(queues = { "${rabbitmq.queue.delete.transaction}" })
    public void consumeDeleteTransaction(DeleteTransactionDTO deleteTransactionDTO,
            @Headers Map<String, Object> headers) {
        try {
            if (deleteTransactionDTO != null) {
                transactionService.deleteTransaction(deleteTransactionDTO.id);
            }
        } catch (Exception e) {
            LOGGER.info(e.toString());
        }
    }

    @Data
    @NoArgsConstructor
    static class DeleteTransactionDTO {
        Long id;
    }

    // @RabbitListener(queues = {"${rabbitmq.queue.status.transaction}", })
    // public void consumeGeneral(String message, @Headers Map<String, Object>
    // headers) {
    // // consumed doesnt have to be in transaction model
    // ObjectMapper objectMapper = new ObjectMapper();
    // try (Session session = sessionFactory.openSession()) {
    // if (message != null) {
    // try {
    // EditTransactionStatusDTO editTransactionStatusDTO =
    // objectMapper.readValue(message, EditTransactionStatusDTO.class);
    // if (editTransactionStatusDTO != null) {
    // return;
    // }
    // } catch (Exception e) {
    // LOGGER.info("is not "+EditTransactionStatusDTO.class.getSimpleName());
    // }
    // }
    // } catch (Exception e) {
    // LOGGER.error("Failed to process RabbitMQ message: " + e.getMessage());
    // }
    // }

    public void sendAnyObject(Object object, String exchange, String routingKey, String queue) {
        rabbitMQConfig.declareBinding(queue, exchange, routingKey);
        rabbitTemplate.convertAndSend(exchange, routingKey, object);
    }
}
