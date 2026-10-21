package com.sanedge.pointofsale.order.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sanedge.pointofsale.BaseIntegrationTest;
import com.sanedge.pointofsale.common.domain.responses.api.ApiResponse;
import com.sanedge.pointofsale.common.outbox.OutboxRepository;
import com.sanedge.pointofsale.order.domain.requests.CreateOrderItemRequest;
import com.sanedge.pointofsale.order.domain.requests.CreateOrderRequest;
import com.sanedge.pointofsale.order.domain.responses.OrderResponse;
import com.sanedge.pointofsale.order.models.Order;
import com.sanedge.pointofsale.order.service.OrderCommandService;
import com.sanedge.pointofsale.pb.cashier.ApiResponseCashier;
import com.sanedge.pointofsale.pb.cashier.CashierQueryServiceGrpc;
import com.sanedge.pointofsale.pb.cashier.CashierResponse;
import com.sanedge.pointofsale.pb.merchant.ApiResponseMerchant;
import com.sanedge.pointofsale.pb.merchant.MerchantQueryServiceGrpc;
import com.sanedge.pointofsale.pb.merchant.MerchantResponse;
import com.sanedge.pointofsale.pb.order_item.ApiResponseOrderItem;
import com.sanedge.pointofsale.pb.order_item.OrderItemCommandServiceGrpc;
import com.sanedge.pointofsale.pb.order_item.OrderItemResponse;
import com.sanedge.pointofsale.pb.product.ApiResponseProduct;
import com.sanedge.pointofsale.pb.product.ProductCommandServiceGrpc;
import com.sanedge.pointofsale.pb.product.ProductQueryServiceGrpc;
import com.sanedge.pointofsale.pb.product.ProductResponse;

import jakarta.validation.Validator;

/**
 * When the outbox insert fails, the whole transaction is rolled back: tidak ada order yang
 * tersisa tanpa event. Kegagalan outbox membatalkan request (tidak ditelan diam-diam).
 */
class OrderOutboxRollbackIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OrderCommandService commandService;

    @MockitoBean
    private OutboxRepository outboxRepository;

    @MockitoBean
    private Validator validator;

    @MockitoBean
    private MerchantQueryServiceGrpc.MerchantQueryServiceBlockingStub merchantQueryStub;

    @MockitoBean
    private CashierQueryServiceGrpc.CashierQueryServiceBlockingStub cashierQueryStub;

    @MockitoBean
    private ProductQueryServiceGrpc.ProductQueryServiceBlockingStub productQueryStub;

    @MockitoBean
    private ProductCommandServiceGrpc.ProductCommandServiceBlockingStub productCommandStub;

    @MockitoBean
    private OrderItemCommandServiceGrpc.OrderItemCommandServiceBlockingStub orderItemCommandStub;

    @BeforeEach
    void setupMocks() {
        when(merchantQueryStub.findById(any()))
                .thenReturn(ApiResponseMerchant.newBuilder()
                        .setStatus("success")
                        .setMessage("ok")
                        .setData(MerchantResponse.newBuilder().setMerchantId(1).build())
                        .build());

        when(cashierQueryStub.findById(any()))
                .thenReturn(ApiResponseCashier.newBuilder()
                        .setStatus("success")
                        .setMessage("ok")
                        .setData(CashierResponse.newBuilder().setCashierId(1).build())
                        .build());

        when(productQueryStub.findById(any()))
                .thenReturn(ApiResponseProduct.newBuilder()
                        .setStatus("success")
                        .setMessage("ok")
                        .setData(ProductResponse.newBuilder()
                                .setProductId(101)
                                .setMerchantId(1)
                                .setCategoryId(1)
                                .setName("Order Product")
                                .setPrice(15000)
                                .setCountInStock(10)
                                .build())
                        .build());

        when(orderItemCommandStub.createOrderItem(any()))
                .thenReturn(ApiResponseOrderItem.newBuilder()
                        .setStatus("success")
                        .setMessage("ok")
                        .setData(OrderItemResponse.newBuilder().setOrderItemId(1).build())
                        .build());

        when(productCommandStub.updateProduct(any()))
                .thenReturn(ApiResponseProduct.newBuilder()
                        .setStatus("success")
                        .setMessage("ok")
                        .setData(ProductResponse.newBuilder()
                                .setProductId(101)
                                .setMerchantId(1)
                                .setCategoryId(1)
                                .setName("Order Product")
                                .setPrice(15000)
                                .setCountInStock(8)
                                .build())
                        .build());

        doThrow(new IllegalStateException("outbox insert failed"))
                .when(outboxRepository).enqueue(any());
    }

    @Test
    void orderRowIsRolledBackWhenTheOutboxInsertFails() {
        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductId(101);
        item.setQuantity(2);
        item.setPrice(15000);

        List<CreateOrderItemRequest> items = new ArrayList<>();
        items.add(item);

        CreateOrderRequest req = new CreateOrderRequest();
        req.setMerchantId(1);
        req.setCashierId(1);
        req.setItems(items);

        ApiResponse<OrderResponse> response = commandService.create(req);

        assertThat(response.getStatus()).isEqualTo("error");
        assertThat(mongoTemplate.find(new Query(), Order.class)).isEmpty();
    }
}
