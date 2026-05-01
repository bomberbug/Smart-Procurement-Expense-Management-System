package com.procurement.expense;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableCaching
public class ExpenseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpenseServiceApplication.class, args);
    }

    // RabbitMQ Configuration
    @Bean
    public TopicExchange expenseExchange() {
        return new TopicExchange("expense.exchange");
    }

    @Bean
    public Queue expenseQueue() {
        return new Queue("expense.queue", true);
    }

    @Bean
    public Binding binding(Queue expenseQueue, TopicExchange expenseExchange) {
        return BindingBuilder.bind(expenseQueue)
                .to(expenseExchange)
                .with("expense.#");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
