package com.sanedge.pointofsale.common.outbox;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Builds the JSON envelope every email event must carry.
 *
 * <p>Identical contract to the ecommerce factory: the consumer
 * ({@code EmailService.hasValidEnvelope}) requires {@code event_id},
 * {@code schema_version == 1}, {@code event_type}, {@code email},
 * {@code subject}, {@code body}, plus {@code occurred_at} for observability.
 */
public final class OutboxEventFactory {

    /** Must match {@code EmailService.EVENT_SCHEMA_VERSION}. */
    public static final int SCHEMA_VERSION = 1;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OutboxEventFactory() {
    }

    /**
     * Builds the email envelope for {@code topic}.
     *
     * @return the serialized payload plus the generated {@code event_id}
     */
    public static Envelope email(String topic, String email, String subject, String body) {
        String eventId = UUID.randomUUID().toString();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", email);
        payload.put("subject", subject);
        payload.put("body", body);
        payload.put("event_id", eventId);
        payload.put("schema_version", SCHEMA_VERSION);
        payload.put("event_type", topic);
        payload.put("occurred_at", Instant.now().toString());

        try {
            return new Envelope(eventId, MAPPER.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize outbox payload for topic " + topic, e);
        }
    }

    /** The generated event id plus its serialized JSON payload. */
    public record Envelope(String eventId, String payload) {
    }

    /**
     * Best-effort conversion of a Mongo/domain String id into the numeric dimension id the
     * ClickHouse {@code stats_revenue} table expects. Ids are ObjectIds (non-numeric), so an
     * unparseable id yields {@code 0} rather than failing the enqueue.
     */
    public static long numericId(String id) {
        if (id == null) {
            return 0L;
        }
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Builds the analytics envelope published to {@code stats.<domain>.event}.
     *
     * <p>Consumed by {@code stats-writer} ({@code StatsWriterConsumer}) and written
     * into the ClickHouse {@code stats_revenue} table. Fields mirror what the
     * consumer reads: {@code period} (YYYY-MM), {@code amount}, {@code merchantId},
     * {@code productId}, plus {@code metric} derived from the topic.
     *
     * @param topic      destination topic; also used as {@code event_type}
     * @param period     accounting period, formatted {@code YYYY-MM}
     * @param amount     revenue / volume for the period
     * @param merchantId merchant dimension id (0 if not applicable)
     * @param productId  product dimension id (0 if not applicable)
     * @return the serialized payload plus the generated {@code event_id}
     */
    public static Envelope stats(String topic, String period, double amount, long merchantId, long productId) {
        String eventId = UUID.randomUUID().toString();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("period", period);
        payload.put("amount", amount);
        payload.put("merchantId", merchantId);
        payload.put("productId", productId);
        payload.put("metric", metricFromTopic(topic));
        payload.put("event_id", eventId);
        payload.put("schema_version", SCHEMA_VERSION);
        payload.put("event_type", topic);
        payload.put("occurred_at", Instant.now().toString());

        try {
            return new Envelope(eventId, MAPPER.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize stats payload for topic " + topic, e);
        }
    }

    /**
     * Derives the ClickHouse {@code metric} from a {@code stats.*.event} topic so the writer can
     * keep revenue ({@code txn}), order and merchant series distinct.
     */
    private static String metricFromTopic(String topic) {
        if (topic == null) {
            return "txn";
        }
        if (topic.contains("transaction")) {
            return "txn";
        }
        if (topic.contains("order")) {
            return "order";
        }
        if (topic.contains("merchant")) {
            return "merchant";
        }
        return "txn";
    }
}
