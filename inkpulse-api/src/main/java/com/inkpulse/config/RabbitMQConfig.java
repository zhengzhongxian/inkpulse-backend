package com.inkpulse.config;

import com.inkpulse.constants.QueueConstants;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue otpEmailQueue() {
        return new Queue(QueueConstants.SEND_OTP_EMAIL, true);
    }

    @Bean
    public Queue challengeEmailQueue() {
        return new Queue(QueueConstants.SEND_CHALLENGE_EMAIL, true);
    }

    @Bean
    public Queue deviceAlertEmailQueue() {
        return new Queue(QueueConstants.SEND_DEVICE_ALERT_EMAIL, true);
    }
}
