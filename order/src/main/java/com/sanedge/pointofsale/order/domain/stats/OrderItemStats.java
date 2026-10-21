package com.sanedge.pointofsale.order.domain.stats;

/**
 * Per-order-item stats projection emitted to {@code stats.pointofsale.order_item.event}.
 *
 * <p>Mirrors the Quarkus order service's {@code OrderItemStats}; the payload keys
 * are kept identical so a Quarkus-style consumer can ingest it unchanged.
 */
public record OrderItemStats(
        int orderItemId,
        String orderId,
        int merchantId,
        int categoryId,
        int productId,
        int quantity,
        int unitPrice,
        int subtotal) {
}
