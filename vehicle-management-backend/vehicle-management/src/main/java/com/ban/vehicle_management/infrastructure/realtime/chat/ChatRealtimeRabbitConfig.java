package com.ban.vehicle_management.infrastructure.realtime.chat;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.util.StringUtils;

@Configuration
@EnableRabbit
public class ChatRealtimeRabbitConfig {

    @Bean
    public MessageConverter chatRabbitMessageConverter() {
        return new JacksonJsonMessageConverter(
                "com.ban.vehicle_management.application.operations.chatconversation.model"
        );
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter chatRabbitMessageConverter
    ) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(chatRabbitMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter chatRabbitMessageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(chatRabbitMessageConverter);
        return factory;
    }

    @Bean
    public TopicExchange chatRealtimeExchange(
            @Value("${app.chat.realtime.exchange:chat.realtime.exchange}") String exchangeName
    ) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue chatRealtimeQueue(
            @Value("${app.chat.realtime.queue:}") String queueName
    ) {
        if (StringUtils.hasText(queueName)) {
            return new Queue(queueName, true);
        }
        return new AnonymousQueue();
    }

    @Bean
    public Binding chatRealtimeBinding(
            TopicExchange chatRealtimeExchange,
            Queue chatRealtimeQueue,
            @Value("${app.chat.realtime.routing-key:chat.realtime}") String routingKey
    ) {
        return BindingBuilder.bind(chatRealtimeQueue).to(chatRealtimeExchange).with(routingKey);
    }
}
