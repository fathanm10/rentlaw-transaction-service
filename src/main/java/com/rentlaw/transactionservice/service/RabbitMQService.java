package com.rentlaw.transactionservice.service;

import com.rentlaw.transactionservice.model.Transaction;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQService {
    // for sending
    @Value("${rabbitmq.exchange.name}")
    private String exchange;
    @Value("${rabbitmq.routing.key}")
    private String routingKey;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = {"${rabbitmq.queue.name}"})
    public void consume(Transaction transaction) {
        // consumed doesnt have to be in transaction model
        if (transaction != null) {
            // create transaction based on consumed message
        }
    }

    public void sendJsonMessage(Transaction transaction) {
        rabbitTemplate.convertAndSend(exchange, routingKey, transaction);
    }
}
