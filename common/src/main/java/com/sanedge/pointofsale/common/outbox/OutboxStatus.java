package com.sanedge.pointofsale.common.outbox;

/**
 * Lifecycle of a row in the shared {@code outbox} collection.
 *
 * <p>A row is created {@link #PENDING}; a relay claims it (lease + claim token), publishes it and
 * moves it to {@link #PROCESSED}. A failed publish releases the row back to {@link #PENDING} with
 * a backoff; once the retry budget is exhausted the row is parked as {@link #FAILED}
 * (dead letter) and never picked up again.
 */
public enum OutboxStatus {
    PENDING,
    PROCESSED,
    FAILED
}
