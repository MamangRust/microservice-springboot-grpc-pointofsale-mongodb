package com.sanedge.pointofsale.common.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.kafka.core.KafkaTemplate;

/**
 * Enables Spring's {@code @Scheduled} processing so the {@link OutboxPublisher} relay actually
 * drains the outbox collection into Kafka.
 *
 * <p>Only producer modules (those that depend on {@code spring-kafka}, hence have
 * {@link KafkaTemplate} on the classpath) need this; gated so modules without Kafka stay clean.
 * The {@link OutboxPublisher} itself is also {@code @ConditionalOnClass(KafkaTemplate.class)}.
 */
@Configuration
@EnableScheduling
@ConditionalOnClass(KafkaTemplate.class)
public class OutboxSchedulingConfig {
}
