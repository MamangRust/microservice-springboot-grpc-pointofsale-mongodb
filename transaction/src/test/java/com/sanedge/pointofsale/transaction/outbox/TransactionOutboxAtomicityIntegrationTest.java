package com.sanedge.pointofsale.transaction.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sanedge.pointofsale.BaseIntegrationTest;
import com.sanedge.pointofsale.common.domain.responses.api.ApiResponse;
import com.sanedge.pointofsale.common.outbox.Outbox;
import com.sanedge.pointofsale.pb.merchant.ApiResponseMerchant;
import com.sanedge.pointofsale.pb.merchant.MerchantQueryServiceGrpc;
import com.sanedge.pointofsale.pb.merchant.MerchantResponse;
import com.sanedge.pointofsale.pb.order.ApiResponseOrder;
import com.sanedge.pointofsale.pb.order.OrderQueryServiceGrpc;
import com.sanedge.pointofsale.pb.order.OrderResponse;
import com.sanedge.pointofsale.pb.order_item.ApiOrderItemsResponse;
import com.sanedge.pointofsale.pb.order_item.OrderItemQueryServiceGrpc;
import com.sanedge.pointofsale.pb.order_item.OrderItemResponse;
import com.sanedge.pointofsale.pb.user.ApiResponseUser;
import com.sanedge.pointofsale.pb.user.UserQueryServiceGrpc;
import com.sanedge.pointofsale.pb.user.UserResponse;
import com.sanedge.pointofsale.transaction.domain.requests.CreateTransactionRequest;
import com.sanedge.pointofsale.transaction.domain.responses.TransactionResponse;
import com.sanedge.pointofsale.transaction.models.Transaction;
import com.sanedge.pointofsale.transaction.service.TransactionCommandService;

import jakarta.validation.Validator;

/**
 * Baris transaction dan event outbox ditulis dalam satu transaksi MongoDB: keduanya ada atau
 * tidak ada sama sekali.
 */
class TransactionOutboxAtomicityIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TransactionCommandService commandService;

    @MockitoBean
    private Validator validator;

    @MockitoBean
    private MerchantQueryServiceGrpc.MerchantQueryServiceBlockingStub merchantQueryStub;

    @MockitoBean
    private OrderQueryServiceGrpc.OrderQueryServiceBlockingStub orderQueryStub;

    @MockitoBean
    private OrderItemQueryServiceGrpc.OrderItemQueryServiceBlockingStub orderItemQueryStub;

    @MockitoBean
    private UserQueryServiceGrpc.UserQueryServiceBlockingStub userQueryStub;

    @BeforeEach
    void setupMocks() {
        when(merchantQueryStub.findById(any()))
                .thenReturn(ApiResponseMerchant.newBuilder()
                        .setStatus("success")
                        .setMessage("ok")
                        .setData(MerchantResponse.newBuilder().setMerchantId(1).setUserId(7).build())
                        .build());

        when(orderQueryStub.findById(any()))
                .thenReturn(ApiResponseOrder.newBuilder()
                        .setStatus("success")
                        .setMessage("ok")
                        .setData(OrderResponse.newBuilder()
                                .setOrderId(1)
                                .setMerchantId(1)
                                .setCashierId(1)
                                .setTotalPrice(35000L)
                                .build())
                        .build());

        when(orderItemQueryStub.findByOrder(any()))
                .thenReturn(ApiOrderItemsResponse.newBuilder()
                        .setStatus("success")
                        .setMessage("ok")
                        .addData(OrderItemResponse.newBuilder()
                                .setOrderItemId(1)
                                .setOrderId(1)
                                .setProductId(101)
                                .setQuantity(2)
                                .setPrice(15000)
                                .build())
                        .build());

        when(userQueryStub.findById(any()))
                .thenReturn(ApiResponseUser.newBuilder()
                        .setStatus("success")
                        .setMessage("ok")
                        .setData(UserResponse.newBuilder()
                                .setId(7)
                                .setUsername("ada")
                                .setFirstname("Ada")
                                .setLastname("Lovelace")
                                .setEmail("ada@example.com")
                                .build())
                        .build());
    }

    @Test
    void transactionRowAndOutboxEventsAreCommittedTogether() {
        CreateTransactionRequest req = new CreateTransactionRequest();
        req.setOrderID(1);
        req.setMerchantID(1);
        req.setPaymentMethod("CREDIT_CARD");
        req.setAmount(100000);
        req.setPaymentStatus("PENDING");

        ApiResponse<TransactionResponse> response = commandService.create(req);

        assertThat(response.getStatus()).isEqualTo("success");

        assertThat(mongoTemplate.find(new Query(), Transaction.class)).hasSize(1);

        List<Outbox> events = mongoTemplate.find(new Query(), Outbox.class);
        assertThat(events).hasSize(2);
        assertThat(events).extracting(Outbox::getTopic)
                .containsExactlyInAnyOrder("email-service-topic-transaction-create", "stats.transaction.event");
        assertThat(events).allSatisfy(event -> {
            assertThat(event.getStatus().name()).isEqualTo("PENDING");
            assertThat(event.getAggregateType()).isEqualTo("TRANSACTION");
        });
    }
}
