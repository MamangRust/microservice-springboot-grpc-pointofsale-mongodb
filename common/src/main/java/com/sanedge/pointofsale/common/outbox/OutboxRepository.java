package com.sanedge.pointofsale.common.outbox;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * Persistence for the shared {@code outbox} collection (MongoDB).
 *
 * <p>The relay never flips a status directly: it <em>claims</em> a row first
 * ({@link #claim(Outbox)}) which stamps a {@code claimToken} and {@code claimedAt} lease on it.
 * The claim is a single atomic {@code findAndModify}, so of two replicas polling the same
 * collection exactly one wins the row. {@link #markSent(Outbox)} /
 * {@link #markRetry(Outbox, String, long)} only apply when the caller still owns the token, so a
 * relay whose lease already expired cannot overwrite the work of the relay that took over.
 *
 * <p>Scope: every service publishes only the domains it owns
 * ({@code outbox.publisher.domains}); an empty list means "all domains".
 *
 * <p>The business write and the outbox insert are joined by the caller's MongoDB transaction
 * (see {@code MongoTransactionConfig}), so an event exists if and only if the business row
 * exists.
 */
@Repository
public class OutboxRepository {

    private final MongoTemplate mongoTemplate;
    private final Set<String> domains;
    private final long leaseSeconds;

    public OutboxRepository(MongoTemplate mongoTemplate,
            @Value("${outbox.publisher.domains:}") String domains,
            @Value("${outbox.publisher.lease-seconds:300}") long leaseSeconds) {
        this.mongoTemplate = mongoTemplate;
        this.domains = parseDomains(domains);
        this.leaseSeconds = Math.max(1, leaseSeconds);
    }

    /** Persists a new PENDING event; call inside the business transaction. */
    public void enqueue(Outbox event) {
        if (event.getStatus() == null) {
            event.setStatus(OutboxStatus.PENDING);
        }
        mongoTemplate.save(event);
    }

    /**
     * Returns the rows that may be published right now: still {@code PENDING}, past their retry
     * backoff and either unclaimed or with an expired lease.
     */
    public List<Outbox> findDue(int limit) {
        Query query = new Query(dueCriteria(Instant.now()))
                .with(Sort.by(Sort.Direction.ASC, "createdAt"))
                .limit(Math.max(1, limit));
        return mongoTemplate.find(query, Outbox.class);
    }

    /**
     * Atomically claims a row for publishing.
     *
     * @return the claimed row (with a fresh {@code claimToken}) or {@code null} when another
     *         relay instance won the race
     */
    public Outbox claim(Outbox candidate) {
        if (candidate == null || candidate.getId() == null) {
            return null;
        }
        Instant now = Instant.now();
        String token = UUID.randomUUID().toString();

        Query query = new Query(dueCriteria(now).and("_id").is(candidate.getId()));
        Update update = new Update()
                .set("claimedAt", now)
                .set("claimToken", token)
                .inc("attempts", 1);

        return mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), Outbox.class);
    }

    /** Marks a claimed row as delivered. */
    public void markSent(Outbox claimed) {
        Query query = ownedQuery(claimed);
        Update update = new Update()
                .set("status", OutboxStatus.PROCESSED)
                .set("processedAt", Instant.now())
                .set("claimedAt", null)
                .set("nextAttemptAt", null)
                .set("lastError", null);
        mongoTemplate.updateFirst(query, update, Outbox.class);
    }

    /**
     * Releases a claimed row back to PENDING for a later retry. The row is only retried after
     * {@code delaySeconds}.
     */
    public void markRetry(Outbox claimed, String error, long delaySeconds) {
        Query query = ownedQuery(claimed);
        Update update = new Update()
                .set("status", OutboxStatus.PENDING)
                .set("claimedAt", null)
                .set("lastError", error)
                .set("nextAttemptAt", Instant.now().plusSeconds(Math.max(0, delaySeconds)));
        mongoTemplate.updateFirst(query, update, Outbox.class);
    }

    /** Parks a claimed row in {@code FAILED} (dead letter) — it is never retried again. */
    public void markFailed(Outbox claimed, String error) {
        Query query = ownedQuery(claimed);
        Update update = new Update()
                .set("status", OutboxStatus.FAILED)
                .set("processedAt", Instant.now())
                .set("claimedAt", null)
                .set("lastError", error);
        mongoTemplate.updateFirst(query, update, Outbox.class);
    }

    /** Backlog depth (PENDING rows) for the backlog gauge / tests. */
    public long countPending() {
        return mongoTemplate.count(new Query(pendingCriteria()), Outbox.class);
    }

    /** The domains this repository publishes; empty means "all". */
    public Set<String> domains() {
        return domains;
    }

    private Criteria dueCriteria(Instant now) {
        Instant leaseCutoff = now.minusSeconds(leaseSeconds);
        return pendingCriteria()
                .andOperator(
                        new Criteria().orOperator(
                                Criteria.where("nextAttemptAt").is(null),
                                Criteria.where("nextAttemptAt").lte(now)),
                        new Criteria().orOperator(
                                Criteria.where("claimedAt").is(null),
                                Criteria.where("claimedAt").lte(leaseCutoff)));
    }

    private Criteria pendingCriteria() {
        Criteria criteria = Criteria.where("status").is(OutboxStatus.PENDING);
        if (!domains.isEmpty()) {
            criteria = criteria.and("aggregateType").in(domains);
        }
        return criteria;
    }

    /** A row may only be updated by the relay that currently owns its claim token. */
    private Query ownedQuery(Outbox claimed) {
        Criteria criteria = Criteria.where("_id").is(claimed.getId())
                .and("status").is(OutboxStatus.PENDING);
        if (claimed.getClaimToken() != null) {
            criteria = criteria.and("claimToken").is(claimed.getClaimToken());
        }
        return new Query(criteria);
    }

    private static Set<String> parseDomains(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
