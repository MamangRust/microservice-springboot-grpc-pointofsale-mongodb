package com.sanedge.pointofsale.order.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.sanedge.pointofsale.BaseIntegrationTest;
import com.sanedge.pointofsale.common.outbox.Outbox;
import com.sanedge.pointofsale.common.outbox.OutboxRepository;
import com.sanedge.pointofsale.common.outbox.OutboxStatus;

/**
 * Relay outbox: claim hanya boleh diambil satu relay (lease + token), retry punya backoff, dan
 * row yang berhasil dipublish ditandai {@code PROCESSED}.
 *
 * <p>Semua test memakai MongoDB replica set sungguhan (testcontainers) supaya perilaku atomic
 * {@code findAndModify} benar-benar teruji.
 */
class OutboxRelayIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private OutboxRepository repository;

    /** Relay kedua — meniru replica lain dari service yang sama. */
    private OutboxRepository otherReplica() {
        return new OutboxRepository(mongoTemplate, "ORDER", 300);
    }

    private Outbox event(String domain, String topic) {
        Outbox outbox = new Outbox();
        outbox.setAggregateType(domain);
        outbox.setAggregateId("42");
        outbox.setEventKey("42");
        outbox.setTopic(topic);
        outbox.setPayload("{}");
        return outbox;
    }

    @Test
    void relayOnlySeesTheDomainsItOwns() {
        repository.enqueue(event("ORDER", "stats.pointofsale.order.event"));
        repository.enqueue(event("TRANSACTION", "stats.transaction.event"));

        List<Outbox> due = repository.findDue(10);

        assertThat(due).hasSize(1);
        assertThat(due.get(0).getAggregateType()).isEqualTo("ORDER");
    }

    @Test
    void claimIsExclusiveBetweenReplicas() {
        repository.enqueue(event("ORDER", "stats.pointofsale.order.event"));

        Outbox candidate = repository.findDue(10).get(0);

        Outbox claimedByFirst = repository.claim(candidate);
        Outbox claimedBySecond = otherReplica().claim(candidate);

        assertThat(claimedByFirst).isNotNull();
        assertThat(claimedByFirst.getClaimToken()).isNotBlank();
        assertThat(claimedBySecond).isNull();
    }

    @Test
    void claimedRowIsInvisibleToOtherRelays() {
        repository.enqueue(event("ORDER", "stats.pointofsale.order.event"));
        Outbox candidate = repository.findDue(10).get(0);

        repository.claim(candidate);

        assertThat(otherReplica().findDue(10)).isEmpty();
    }

    @Test
    void expiredLeaseMakesTheRowClaimableAgain() throws InterruptedException {
        // Kedua relay memakai durasi lease yang sama (konfigurasi bersama per service).
        OutboxRepository first = new OutboxRepository(mongoTemplate, "ORDER", 1);
        OutboxRepository second = new OutboxRepository(mongoTemplate, "ORDER", 1);
        first.enqueue(event("ORDER", "stats.pointofsale.order.event"));

        Outbox candidate = first.findDue(10).get(0);
        assertThat(first.claim(candidate)).isNotNull();

        Thread.sleep(1_200);

        Outbox reclaimed = second.claim(candidate);
        assertThat(reclaimed).isNotNull();
        assertThat(reclaimed.getClaimToken()).isNotEqualTo(candidate.getClaimToken());
    }

    @Test
    void retryReleasesTheRowWithBackoff() {
        repository.enqueue(event("ORDER", "stats.pointofsale.order.event"));
        Outbox candidate = repository.findDue(10).get(0);
        Outbox claimed = repository.claim(candidate);

        repository.markRetry(claimed, "kafka down", 60);

        assertThat(repository.findDue(10)).isEmpty();
        Outbox stored = mongoTemplate.findById(claimed.getId(), Outbox.class);
        assertThat(stored.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(stored.getLastError()).isEqualTo("kafka down");
        assertThat(stored.getNextAttemptAt()).isAfter(Instant.now());
        assertThat(stored.getClaimedAt()).isNull();
    }

    @Test
    void sentRowIsMarkedProcessed() {
        repository.enqueue(event("ORDER", "stats.pointofsale.order.event"));
        Outbox candidate = repository.findDue(10).get(0);
        Outbox claimed = repository.claim(candidate);

        repository.markSent(claimed);

        Outbox stored = mongoTemplate.findById(claimed.getId(), Outbox.class);
        assertThat(stored.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
        assertThat(stored.getProcessedAt()).isNotNull();
        assertThat(repository.countPending()).isZero();
    }

    @Test
    void backlogCountsOnlyPendingRowsOfTheOwnedDomains() {
        repository.enqueue(event("ORDER", "stats.pointofsale.order.event"));
        repository.enqueue(event("TRANSACTION", "stats.transaction.event"));

        assertThat(repository.countPending()).isEqualTo(1);
        assertThat(mongoTemplate.find(new Query(), Outbox.class)).hasSize(2);
    }
}
