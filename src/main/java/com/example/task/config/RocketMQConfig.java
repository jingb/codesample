package com.example.task.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ Producer Configuration
 * Uses official RocketMQ SDK (no Spring abstractions)
 */
@Slf4j
@Configuration
public class RocketMQConfig {

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Value("${rocketmq.producer.group:task-producer-group}")
    private String producerGroup;

    private DefaultMQProducer producer;

    /**
     * Create and start RocketMQ producer
     */
    @Bean
    public DefaultMQProducer rocketMQProducer() throws Exception {
        log.info("Initializing RocketMQ producer: group={}, nameserver={}", producerGroup, nameServer);

        // 1. Create producer instance
        producer = new DefaultMQProducer(producerGroup);

        // 2. Configure name server address
        producer.setNamesrvAddr(nameServer);

        // 3. Start producer
        producer.start();

        log.info("RocketMQ producer started successfully");

        return producer;
    }

    /**
     * Shutdown producer gracefully
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down RocketMQ producer...");

        if (producer != null) {
            producer.shutdown();
            log.info("RocketMQ producer shut down successfully");
        }
    }
}
