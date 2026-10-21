package com.sanedge.pointofsale.common.outbox;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Transactional outbox document (collection {@code outbox} in each service's own Mongo DB).
 *
 * <p>All modules write the same collection and are distinguished by {@code aggregateType}; each
 * service publishes only the domains it owns (see {@code outbox.publisher.domains}). Stored as a
 * Mongo document (the project is MongoDB — no JPA/datasource is configured), so {@code id} is a
 * String ObjectId and timestamps are {@link Instant}.
 *
 * <p>Delivery is claim-based: a relay takes a {@link #claimToken} on a row before publishing it,
 * so two replicas of the same service never publish the same event twice. If the relay dies
 * mid-send the lease ({@link #claimedAt}) expires and the row becomes claimable again — delivery
 * is at-least-once.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "outbox")
public class Outbox {

    @Id
    private String id;

    /** Logical producer, e.g. {@code USER} or {@code TRANSACTION}. */
    private String aggregateType;

    /** Aggregate instance the event belongs to (transaction / order id). */
    private String aggregateId;

    /** Kafka record key; falls back to {@link #aggregateId} when not set. */
    private String eventKey;

    private String topic;

    private String payload;

    private OutboxStatus status = OutboxStatus.PENDING;

    private Integer attempts = 0;

    private Instant createdAt = Instant.now();

    private Instant processedAt;

    /** Set while a relay holds the lease on this row. */
    private Instant claimedAt;

    /** Token identifying the relay instance that currently owns the row. */
    private String claimToken;

    /** Earliest moment the row may be retried; {@code null} means "right away". */
    private Instant nextAttemptAt;

    private String lastError;
}
