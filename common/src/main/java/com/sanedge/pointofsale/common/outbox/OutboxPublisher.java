package com.sanedge.pointofsale.common.outbox;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sanedge.pointofsale.common.chaos.ChaosManager;
import com.sanedge.pointofsale.common.chaos.ChaosPolicy;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Drains the shared {@code outbox} collection into Kafka.
 *
 * <p>The relay is claim-based: every due row is first claimed
 * ({@link OutboxRepository#claim(Outbox)}) — the claim stamps a token and a lease on the row —
 * and only the relay owning that token may mark it sent or retry it. Two replicas of the same
 * service therefore never publish the same event twice, and a relay that dies mid-send releases
 * its rows automatically once the lease expires (at-least-once delivery).
 *
 * <p>Each service publishes only the domains it owns
 * ({@code outbox.publisher.domains}), so the order service never publishes transaction events
 * and vice versa.
 *
 * <p>The scheduler runs on Spring's default single-threaded scheduler; a boolean guard prevents
 * overlapping ticks.
 *
 * <p>Gated on {@link KafkaTemplate} so non-producer modules do not load this bean.
 * Its {@code KafkaTemplate} field would otherwise be introspected by Spring on
 * every scan of this package, failing the whole context with
 * {@code NoClassDefFoundError} in modules that lack spring-kafka.
 */
@Component
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(name = "outbox.publisher.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ChaosManager chaosManager;

    @Value("${outbox.publisher.batch-size:50}")
    private int batchSize;

    @Value("${outbox.publisher.max-attempts:5}")
    private int maxAttempts;

    @Value("${outbox.publisher.retry-delay-seconds:30}")
    private long retryDelaySeconds;

    private final AtomicLong published = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private volatile Counter publishedCounter;
    private volatile Counter failedCounter;

    private volatile boolean running;

    public OutboxPublisher(OutboxRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            ChaosManager chaosManager,
            ObjectProvider<MeterRegistry> meterRegistry) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.chaosManager = chaosManager;
        registerMetrics(meterRegistry.getIfAvailable());
    }

    /** Default interval matches the blueprint's 5000 ms. */
    @Scheduled(fixedDelayString = "${outbox.publisher.interval-ms:5000}",
            initialDelayString = "${outbox.publisher.initial-delay-ms:5000}")
    public void poll() {
        if (running) {
            return; // previous tick still running
        }
        running = true;
        try {
            drain();
        } catch (RuntimeException e) {
            log.error("❌ Outbox publisher tick failed: {}", e.getMessage(), e);
        } finally {
            running = false;
        }
    }

    /** Claims the due rows and publishes them sequentially. */
    public void drain() {
        List<Outbox> due = repository.findDue(Math.max(1, batchSize));
        for (Outbox candidate : due) {
            Outbox claimed = repository.claim(candidate);
            if (claimed == null) {
                // another relay instance owns this row
                continue;
            }
            publishOne(claimed);
        }
    }

    private void publishOne(Outbox event) {
        try {
            send(event);
            repository.markSent(event);
            published.incrementAndGet();
            countPublished();
            log.debug("📤 Outbox published | id={} topic={} key={}",
                    event.getId(), event.getTopic(), keyOf(event));
        } catch (RuntimeException error) {
            log.warn("⚠️ Outbox publish failed | id={} topic={} error={}",
                    event.getId(), event.getTopic(), error.getMessage());

            int attempts = event.getAttempts() == null ? 1 : event.getAttempts();
            if (attempts >= Math.max(1, maxAttempts)) {
                repository.markFailed(event, error.getMessage());
                failed.incrementAndGet();
                countFailed();
                log.error("❌ Outbox moved to FAILED (dead letter) | id={} topic={} attempts={}",
                        event.getId(), event.getTopic(), attempts);
            } else {
                repository.markRetry(event, error.getMessage(), Math.max(0, retryDelaySeconds));
            }
        }
    }

    /** Publishes the payload, honouring {@code kafka}-type chaos policies. */
    private void send(Outbox event) {
        ChaosPolicy policy = chaosManager == null ? null : chaosManager.evaluate("kafka", event.getTopic());
        if (policy != null && policy.isEnabled() && Math.random() < policy.getErrorChance()) {
            log.info("Injecting Kafka chaos policy={} topic={}", policy.getName(), event.getTopic());

            if (policy.getLatencyMs() > 0) {
                try {
                    Thread.sleep(policy.getLatencyMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (policy.isDropMessage()) {
                throw new IllegalStateException("Simulated Kafka drop for topic " + event.getTopic());
            }
            if (policy.isRejectMessage()) {
                throw new IllegalStateException(policy.getErrorMessage() != null
                        ? policy.getErrorMessage()
                        : "Simulated Kafka rejection for topic " + event.getTopic());
            }
        }

        kafkaTemplate.send(event.getTopic(), keyOf(event), event.getPayload()).join();
    }

    private static String keyOf(Outbox event) {
        return event.getEventKey() != null ? event.getEventKey() : event.getAggregateId();
    }

    private void registerMetrics(MeterRegistry registry) {
        if (registry == null) {
            return;
        }
        Gauge.builder("outbox.events.pending", repository, repo -> repo.countPending())
                .description("Outbox rows waiting for publication")
                .tag("domains", repository.domains().isEmpty() ? "all" : String.join(",", repository.domains()))
                .register(registry);

        publishedCounter = Counter.builder("outbox.events.published")
                .description("Outbox rows successfully published to Kafka")
                .register(registry);
        failedCounter = Counter.builder("outbox.events.failed")
                .description("Outbox rows parked as dead letters")
                .register(registry);
    }

    private void countPublished() {
        Counter counter = publishedCounter;
        if (counter != null) {
            counter.increment();
        }
    }

    private void countFailed() {
        Counter counter = failedCounter;
        if (counter != null) {
            counter.increment();
        }
    }
}
