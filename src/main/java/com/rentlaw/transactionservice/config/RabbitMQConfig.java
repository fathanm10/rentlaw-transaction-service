package com.rentlaw.transactionservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures RabbitMQ message binding for receiving and sending messages.
 */
@Configuration
public class RabbitMQConfig {
    // for receiving
    @Value("${rabbitmq.queue.name}")
    private String queue;

    @Value("${rabbitmq.exchange.name}")
    private String exchange;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;
    // for receiving
    @Value("${rabbitmq.orchestrator.queue.name}")
    private String queue2;

    @Value("${rabbitmq.orchestrator.exchange.name}")
    private String exchange2;

    @Value("${rabbitmq.orchestrator.routing.key}")
    private String routingKey2;
    @Autowired
    private AmqpAdmin amqpAdmin;

    @Bean
    public Queue queue() {
        return new Queue(queue);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public Binding binding() {
        return BindingBuilder
                .bind(queue())
                .to(exchange())
                .with(routingKey);
    }

    @Bean
    public Queue queue2() {
        return new Queue(queue2);
    }

    @Bean
    public TopicExchange exchange2() {
        return new TopicExchange(exchange2);
    }

    @Bean
    public Binding binding2() {
        return BindingBuilder
                .bind(queue2())
                .to(exchange2())
                .with(routingKey2);
    }

    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * @param connectionFactory Serves both as a template and an entity converter for sending message through message broker.
     * @return
     */
    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }

    public void declareBinding(String queueName, String exchangeName, String routingKey) {
        Queue queue = new Queue(queueName);
        TopicExchange exchange = new TopicExchange(exchangeName);
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(routingKey);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareBinding(binding);
    }
}
