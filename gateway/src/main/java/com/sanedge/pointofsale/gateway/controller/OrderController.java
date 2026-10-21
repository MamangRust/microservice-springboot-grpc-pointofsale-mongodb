package com.sanedge.pointofsale.gateway.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.protobuf.Empty;
import com.sanedge.pointofsale.common.domain.responses.api.ApiResponse;
import com.sanedge.pointofsale.common.domain.responses.api.ApiResponsePagination;
import com.sanedge.pointofsale.common.grpc.ProtoMappers;
import com.sanedge.pointofsale.gateway.dto.requests.CreateOrderItemRequest;
import com.sanedge.pointofsale.gateway.dto.requests.CreateOrderRequest;
import com.sanedge.pointofsale.gateway.dto.requests.FindAllOrderByMerchantRequest;
import com.sanedge.pointofsale.gateway.dto.requests.FindAllOrderRequest;
import com.sanedge.pointofsale.gateway.dto.requests.MonthOrderMerchantRequest;
import com.sanedge.pointofsale.gateway.dto.requests.MonthTotalRevenue;
import com.sanedge.pointofsale.gateway.dto.requests.MonthTotalRevenueMerchantRequest;
import com.sanedge.pointofsale.gateway.dto.requests.UpdateOrderItemRequest;
import com.sanedge.pointofsale.gateway.dto.requests.UpdateOrderRequest;
import com.sanedge.pointofsale.gateway.dto.requests.YearOrderMerchantRequest;
import com.sanedge.pointofsale.gateway.dto.requests.YearTotalRevenueMerchantRequest;
import com.sanedge.pointofsale.gateway.dto.responses.OrderMonthlyResponse;
import com.sanedge.pointofsale.gateway.dto.responses.OrderMonthlyTotalRevenueResponse;
import com.sanedge.pointofsale.gateway.dto.responses.OrderResponse;
import com.sanedge.pointofsale.gateway.dto.responses.OrderResponseDeleteAt;
import com.sanedge.pointofsale.gateway.dto.responses.OrderYearlyResponse;
import com.sanedge.pointofsale.gateway.dto.responses.OrderYearlyTotalRevenueResponse;
import com.sanedge.pointofsale.gateway.mapper.ProtoMapper;
import com.sanedge.pointofsale.pb.order.OrderCommandServiceGrpc;
import com.sanedge.pointofsale.pb.order.OrderQueryServiceGrpc;
import com.sanedge.pointofsale.pb.order.stats.OrderSoldoutServiceGrpc;
import com.sanedge.pointofsale.pb.order.stats.OrderTotalRevenueServiceGrpc;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order")
public class OrderController {

    private final OrderQueryServiceGrpc.OrderQueryServiceBlockingStub orderQueryStub;
    private final OrderCommandServiceGrpc.OrderCommandServiceBlockingStub orderCommandStub;
    private final OrderSoldoutServiceGrpc.OrderSoldoutServiceBlockingStub orderSoldoutStub;
    private final OrderTotalRevenueServiceGrpc.OrderTotalRevenueServiceBlockingStub orderTotalRevenueStub;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponsePagination<List<OrderResponse>>> findAll(
            @ModelAttribute FindAllOrderRequest req) {
        var resp = orderQueryStub.findAll(com.sanedge.pointofsale.pb.order.FindAllOrderRequest.newBuilder()
                .setPage(nvl(req.getPage()))
                .setPageSize(nvl(req.getPageSize()))
                .setSearch(nvl(req.getSearch()))
                .build());
        return ResponseEntity.ok(ApiResponsePagination.<List<OrderResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.orders(resp.getDataList()))
                .pagination(ProtoMappers.fromProtoPagination(resp.getPaginationMeta()))
                .build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<OrderResponse>> findById(@PathVariable Integer id) {
        var resp = orderQueryStub.findById(com.sanedge.pointofsale.pb.order.FindByIdOrderRequest.newBuilder()
                .setOrderId(id)
                .build());
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.order(resp.getData()))
                .build());
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponsePagination<List<OrderResponseDeleteAt>>> findByActive(
            @ModelAttribute FindAllOrderRequest req) {
        var resp = orderQueryStub.findByActive(com.sanedge.pointofsale.pb.order.FindAllOrderRequest.newBuilder()
                .setPage(nvl(req.getPage()))
                .setPageSize(nvl(req.getPageSize()))
                .setSearch(nvl(req.getSearch()))
                .build());
        return ResponseEntity.ok(ApiResponsePagination.<List<OrderResponseDeleteAt>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.ordersDeleteAt(resp.getDataList()))
                .pagination(ProtoMappers.fromProtoPagination(resp.getPaginationMeta()))
                .build());
    }

    @GetMapping("/trashed")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponsePagination<List<OrderResponseDeleteAt>>> findByTrashed(
            @ModelAttribute FindAllOrderRequest req) {
        var resp = orderQueryStub.findByTrashed(com.sanedge.pointofsale.pb.order.FindAllOrderRequest.newBuilder()
                .setPage(nvl(req.getPage()))
                .setPageSize(nvl(req.getPageSize()))
                .setSearch(nvl(req.getSearch()))
                .build());
        return ResponseEntity.ok(ApiResponsePagination.<List<OrderResponseDeleteAt>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.ordersDeleteAt(resp.getDataList()))
                .pagination(ProtoMappers.fromProtoPagination(resp.getPaginationMeta()))
                .build());
    }

    @GetMapping("/merchant/{merchant_id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponsePagination<List<OrderResponse>>> findByMerchant(
            @PathVariable Integer merchant_id,
            @ModelAttribute FindAllOrderByMerchantRequest req) {
        req.setMerchantId(merchant_id);
        var resp = orderQueryStub.findByMerchant(
                com.sanedge.pointofsale.pb.order.FindByMerchantOrderRequest.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setPage(nvl(req.getPage()))
                        .setPageSize(nvl(req.getPageSize()))
                        .setSearch(nvl(req.getSearch()))
                        .build());
        return ResponseEntity.ok(ApiResponsePagination.<List<OrderResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.orders(resp.getDataList()))
                .pagination(ProtoMappers.fromProtoPagination(resp.getPaginationMeta()))
                .build());
    }

    @GetMapping("/monthly-revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<OrderMonthlyResponse>>> findMonthlyRevenue(
            @RequestParam Integer yearMonth) {
        var resp = orderSoldoutStub.findMonthlyOrders(com.sanedge.pointofsale.pb.order.FindYearMonthOrder.newBuilder()
                .setYearMonth(yearMonth)
                .build());
        return ResponseEntity.ok(ApiResponse.<List<OrderMonthlyResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::orderMonthly).toList())
                .build());
    }

    @GetMapping("/yearly-revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<OrderYearlyResponse>>> findYearlyRevenue(
            @RequestParam Integer year) {
        var resp = orderSoldoutStub.findYearlyOrders(com.sanedge.pointofsale.pb.order.FindYearOrder.newBuilder()
                .setYear(year)
                .build());
        return ResponseEntity.ok(ApiResponse.<List<OrderYearlyResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::orderYearly).toList())
                .build());
    }

    @GetMapping("/monthly-total-revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<OrderMonthlyTotalRevenueResponse>>> findMonthlyTotalRevenue(
            MonthTotalRevenue req) {
        var resp = orderTotalRevenueStub.findMonthlyTotalRevenue(
                com.sanedge.pointofsale.pb.order.FindYearMonthTotalRevenue.newBuilder()
                        .setYear(nvl(req.getYear()))
                        .setMonth(nvl(req.getMonth()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<OrderMonthlyTotalRevenueResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::orderMonthlyTotalRevenue).toList())
                .build());
    }

    @GetMapping("/yearly-total-revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<OrderYearlyTotalRevenueResponse>>> findYearlyTotalRevenue(
            @RequestParam Integer year) {
        var resp = orderTotalRevenueStub.findYearlyTotalRevenue(
                com.sanedge.pointofsale.pb.order.FindYearOrder.newBuilder()
                        .setYear(year)
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<OrderYearlyTotalRevenueResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::orderYearlyTotalRevenue).toList())
                .build());
    }

    @GetMapping("/merchant/monthly-revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<OrderMonthlyResponse>>> findMonthlyRevenueByMerchant(
            MonthOrderMerchantRequest req) {
        var resp = orderSoldoutStub.findMonthlyOrdersByMerchant(
                com.sanedge.pointofsale.pb.order.FindYearMonthOrderByMerchant.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setYearMonth(nvl(req.getYear()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<OrderMonthlyResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::orderMonthly).toList())
                .build());
    }

    @GetMapping("/merchant/yearly-revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<OrderYearlyResponse>>> findYearlyRevenueByMerchant(
            YearOrderMerchantRequest req) {
        var resp = orderSoldoutStub.findYearlyOrdersByMerchant(
                com.sanedge.pointofsale.pb.order.FindYearOrderByMerchant.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setYear(nvl(req.getYear()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<OrderYearlyResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::orderYearly).toList())
                .build());
    }

    @GetMapping("/merchant/monthly-total-revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<OrderMonthlyTotalRevenueResponse>>> findMonthlyTotalRevenueByMerchant(
            MonthTotalRevenueMerchantRequest req) {
        var resp = orderTotalRevenueStub.findMonthlyTotalRevenueByMerchant(
                com.sanedge.pointofsale.pb.order.FindYearMonthTotalRevenueByMerchant.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setYear(nvl(req.getYear()))
                        .setMonth(nvl(req.getMonth()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<OrderMonthlyTotalRevenueResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::orderMonthlyTotalRevenue).toList())
                .build());
    }

    @GetMapping("/merchant/yearly-total-revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<OrderYearlyTotalRevenueResponse>>> findYearlyTotalRevenueByMerchant(
            YearTotalRevenueMerchantRequest req) {
        var resp = orderTotalRevenueStub.findYearlyTotalRevenueByMerchant(
                com.sanedge.pointofsale.pb.order.FindYearOrderByMerchant.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setYear(nvl(req.getYear()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<OrderYearlyTotalRevenueResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::orderYearlyTotalRevenue).toList())
                .build());
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> create(@Valid @RequestBody CreateOrderRequest req) {
        var builder = com.sanedge.pointofsale.pb.order.CreateOrderRequest.newBuilder()
                .setMerchantId(nvl(req.getMerchantId()))
                .setCashierId(nvl(req.getCashierId()));
        if (req.getItems() != null) {
            for (CreateOrderItemRequest item : req.getItems()) {
                builder.addItems(com.sanedge.pointofsale.pb.order.CreateOrderItemRequest.newBuilder()
                        .setProductId(nvl(item.getProductId()))
                        .setQuantity(nvl(item.getQuantity()))
                        .setPrice(nvl(item.getPrice()))
                        .build());
            }
        }
        var resp = orderCommandStub.createOrder(builder.build());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<OrderResponse>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.order(resp.getData()))
                .build());
    }

    @PostMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateOrderRequest req) {
        req.setOrderId(id);
        var builder = com.sanedge.pointofsale.pb.order.UpdateOrderRequest.newBuilder()
                .setOrderId(nvl(req.getOrderId()))
                .setCashierId(nvl(req.getCashierId()));
        if (req.getItems() != null) {
            for (UpdateOrderItemRequest item : req.getItems()) {
                builder.addItems(com.sanedge.pointofsale.pb.order.UpdateOrderItemRequest.newBuilder()
                        .setOrderItemId(nvl(item.getOrderItemId()))
                        .setProductId(nvl(item.getProductId()))
                        .setQuantity(nvl(item.getQuantity()))
                        .setPrice(nvl(item.getPrice()))
                        .build());
            }
        }
        var resp = orderCommandStub.updateOrder(builder.build());
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.order(resp.getData()))
                .build());
    }

    @PostMapping("/trashed/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponseDeleteAt>> trashedOrder(@PathVariable Integer id) {
        var resp = orderCommandStub.trashedOrder(com.sanedge.pointofsale.pb.order.FindByIdOrderRequest.newBuilder()
                .setOrderId(id)
                .build());
        return ResponseEntity.ok(ApiResponse.<OrderResponseDeleteAt>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.orderDeleteAt(resp.getData()))
                .build());
    }

    @PostMapping("/restore/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponseDeleteAt>> restoreOrder(@PathVariable Integer id) {
        var resp = orderCommandStub.restoreOrder(com.sanedge.pointofsale.pb.order.FindByIdOrderRequest.newBuilder()
                .setOrderId(id)
                .build());
        return ResponseEntity.ok(ApiResponse.<OrderResponseDeleteAt>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.orderDeleteAt(resp.getData()))
                .build());
    }

    @DeleteMapping("/permanent/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> deleteOrderPermanent(@PathVariable Integer id) {
        var resp = orderCommandStub.deleteOrderPermanent(
                com.sanedge.pointofsale.pb.order.FindByIdOrderRequest.newBuilder()
                        .setOrderId(id)
                        .build());
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getSuccess())
                .build());
    }

    @PostMapping("/restore/all")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> restoreAllOrders() {
        var resp = orderCommandStub.restoreAllOrder(Empty.getDefaultInstance());
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getSuccess())
                .build());
    }

    @PostMapping("/permanent/all")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> deleteAllOrdersPermanent() {
        var resp = orderCommandStub.deleteAllOrderPermanent(Empty.getDefaultInstance());
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getSuccess())
                .build());
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }
}
