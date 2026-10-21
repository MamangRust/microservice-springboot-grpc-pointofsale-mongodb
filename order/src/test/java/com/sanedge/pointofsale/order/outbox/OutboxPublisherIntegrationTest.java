package com.sanedge.pointofsale.order.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sanedge.pointofsale.BaseIntegrationTest;
import com.sanedge.pointofsale.common.outbox.Outbox;
import com.sanedge.pointofsale.common.outbox.OutboxPublisher;
import com.sanedge.pointofsale.common.outbox.OutboxRepository;
import com.sanedge.pointofsale.common.outbox.OutboxStatus;

/**
 * Relay outbox mem-publish row ke Kafka lalu menandainya {@code PROCESSED}; row yang gagal
 * dipublish terus-menerus diparkir sebagai dead letter ({@code FAILED}).
 *
 * <p>Scheduler bawaan dimatikan (interval besar) supaya hanya tick manual yang berjalan —
 * tidak ada relay lain yang "mcuri" row saat test berjalan.
 */
@SpringBootTest(properties = {
        "outbox.publisher.interval-ms=3600000",
        "outbox.publisher.initial-delay-ms=3600000",
        "outbox.publisher.retry-delay-seconds=0",
        "outbox.publisher.max-attempts=2"
})
@ActiveProfiles("test")
class OutboxPublisherIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private OutboxRepository repository;

    @Autowired
    private OutboxPublisher publisher;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    private Outbox event() {
        Outbox outbox = new Outbox();
        outbox.setAggregateType("ORDER");
        outbox.setAggregateId("42");
        outbox.setEventKey("42");
        outbox.setTopic("stats.pointofsale.order.event");
        outbox.setPayload("{}");
        return outbox;
    }

    @Test
    void publishesAndMarksTheRowSent() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        repository.enqueue(event());

        publisher.drain();

        Outbox stored = mongoTemplate.findOne(new Query(), Outbox.class);
        assertThat(stored).isNotNull();
        assertThat(stored.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
        assertThat(stored.getAttempts()).isEqualTo(1);
    }

    @Test
    void parksTheRowAsDeadLetterAfterMaxAttempts() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")));
        repository.enqueue(event());

        publisher.drain();
        publisher.drain();

        Outbox stored = mongoTemplate.findOne(new Query(), Outbox.class);
        assertThat(stored).isNotNull();
        assertThat(stored.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(stored.getAttempts()).isEqualTo(2);
        assertThat(stored.getLastError()).contains("broker down");
    }

    @Test
    void retriesTheRowUntilTheBudgetIsExhausted() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")));
        repository.enqueue(event());

        publisher.drain();

        Outbox stored = mongoTemplate.findOne(new Query(), Outbox.class);
        assertThat(stored).isNotNull();
        assertThat(stored.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(stored.getAttempts()).isEqualTo(1);
        assertThat(stored.getLastError()).contains("broker down");
    }
}
