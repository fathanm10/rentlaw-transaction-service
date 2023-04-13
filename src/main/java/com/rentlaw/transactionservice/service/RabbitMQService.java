package com.rentlaw.transactionservice.service;

import com.rentlaw.transactionservice.model.Transaction;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQService.class);

    /**
     * @param transaction Consumes message from message broker into a predefined model.
     */
    @RabbitListener(queues = {"${rabbitmq.queue.name}"})
    public void consume(Transaction transaction) {
        // consumed doesnt have to be in transaction model
        if (transaction != null) {
            // create transaction based on consumed message
            LOGGER.info(String.valueOf(transaction));
        }
    }

    /**
     * @param transaction Sends an Entity class as a Json Message to message broker.
     */
    public void sendJsonMessage(Transaction transaction) {
        rabbitTemplate.convertAndSend(exchange, routingKey, transaction);
    }
}
