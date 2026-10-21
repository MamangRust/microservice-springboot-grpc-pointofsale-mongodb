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
import com.sanedge.pointofsale.gateway.dto.requests.CreateTransactionRequest;
import com.sanedge.pointofsale.gateway.dto.requests.FindAllTransactionByMerchantRequest;
import com.sanedge.pointofsale.gateway.dto.requests.FindAllTransactionRequest;
import com.sanedge.pointofsale.gateway.dto.requests.MonthAmountTransactionMerchant;
import com.sanedge.pointofsale.gateway.dto.requests.MonthAmountTransactionRequest;
import com.sanedge.pointofsale.gateway.dto.requests.MonthMethodTransactionMerchantRequest;
import com.sanedge.pointofsale.gateway.dto.requests.MonthMethodTransactionRequest;
import com.sanedge.pointofsale.gateway.dto.requests.UpdateTransactionRequest;
import com.sanedge.pointofsale.gateway.dto.requests.YearAmountTransactionMerchant;
import com.sanedge.pointofsale.gateway.dto.requests.YearMethodTransactionMerchantRequest;
import com.sanedge.pointofsale.gateway.dto.responses.TransactionMonthlyAmountFailedResponse;
import com.sanedge.pointofsale.gateway.dto.responses.TransactionMonthlyAmountSuccessResponse;
import com.sanedge.pointofsale.gateway.dto.responses.TransactionMonthlyMethodResponse;
import com.sanedge.pointofsale.gateway.dto.responses.TransactionResponse;
import com.sanedge.pointofsale.gateway.dto.responses.TransactionResponseDeleteAt;
import com.sanedge.pointofsale.gateway.dto.responses.TransactionYearlyAmountFailedResponse;
import com.sanedge.pointofsale.gateway.dto.responses.TransactionYearlyAmountSuccessResponse;
import com.sanedge.pointofsale.gateway.dto.responses.TransactionYearlyMethodResponse;
import com.sanedge.pointofsale.gateway.mapper.ProtoMapper;
import com.sanedge.pointofsale.pb.transaction.TransactionCommandServiceGrpc;
import com.sanedge.pointofsale.pb.transaction.TransactionQueryServiceGrpc;
import com.sanedge.pointofsale.pb.transaction.stats.TransactionStatsAmountServiceGrpc;
import com.sanedge.pointofsale.pb.transaction.stats.TransactionStatsMethodServiceGrpc;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transaction")
public class TransactionController {
    private final TransactionQueryServiceGrpc.TransactionQueryServiceBlockingStub transactionQueryStub;
    private final TransactionCommandServiceGrpc.TransactionCommandServiceBlockingStub transactionCommandStub;
    private final TransactionStatsAmountServiceGrpc.TransactionStatsAmountServiceBlockingStub transactionStatsAmountStub;
    private final TransactionStatsMethodServiceGrpc.TransactionStatsMethodServiceBlockingStub transactionStatsMethodStub;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponsePagination<List<TransactionResponse>>> findAllTransactions(
            @ModelAttribute FindAllTransactionRequest req) {
        var resp = transactionQueryStub.findAll(
                com.sanedge.pointofsale.pb.transaction.FindAllTransactionRequest.newBuilder()
                        .setPage(nvl(req.getPage()))
                        .setPageSize(nvl(req.getPageSize()))
                        .setSearch(nvl(req.getSearch()))
                        .build());
        return ResponseEntity.ok(ApiResponsePagination.<List<TransactionResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.transactions(resp.getDataList()))
                .pagination(ProtoMappers.fromProtoPagination(resp.getPaginationMeta()))
                .build());
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponsePagination<List<TransactionResponseDeleteAt>>> findActiveTransactions(
            @ModelAttribute FindAllTransactionRequest req) {
        var resp = transactionQueryStub.findByActive(
                com.sanedge.pointofsale.pb.transaction.FindAllTransactionRequest.newBuilder()
                        .setPage(nvl(req.getPage()))
                        .setPageSize(nvl(req.getPageSize()))
                        .setSearch(nvl(req.getSearch()))
                        .build());
        return ResponseEntity.ok(ApiResponsePagination.<List<TransactionResponseDeleteAt>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.transactionsDeleteAt(resp.getDataList()))
                .pagination(ProtoMappers.fromProtoPagination(resp.getPaginationMeta()))
                .build());
    }

    @GetMapping("/trashed")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponsePagination<List<TransactionResponseDeleteAt>>> findTrashedTransactions(
            @ModelAttribute FindAllTransactionRequest req) {
        var resp = transactionQueryStub.findByTrashed(
                com.sanedge.pointofsale.pb.transaction.FindAllTransactionRequest.newBuilder()
                        .setPage(nvl(req.getPage()))
                        .setPageSize(nvl(req.getPageSize()))
                        .setSearch(nvl(req.getSearch()))
                        .build());
        return ResponseEntity.ok(ApiResponsePagination.<List<TransactionResponseDeleteAt>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.transactionsDeleteAt(resp.getDataList()))
                .pagination(ProtoMappers.fromProtoPagination(resp.getPaginationMeta()))
                .build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<TransactionResponse>> findTransactionById(@PathVariable Integer id) {
        var resp = transactionQueryStub.findById(
                com.sanedge.pointofsale.pb.transaction.FindByIdTransactionRequest.newBuilder()
                        .setTransactionId(id)
                        .build());
        return ResponseEntity.ok(ApiResponse.<TransactionResponse>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.transaction(resp.getData()))
                .build());
    }

    @GetMapping("/merchant/{merchantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponsePagination<List<TransactionResponse>>> findByMerchant(
            @PathVariable Integer merchantId,
            @ModelAttribute FindAllTransactionByMerchantRequest req) {
        req.setMerchantId(merchantId);
        var resp = transactionQueryStub.findByMerchant(
                com.sanedge.pointofsale.pb.transaction.FindByMerchantTransactionRequest.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setPage(nvl(req.getPage()))
                        .setPageSize(nvl(req.getPageSize()))
                        .setSearch(nvl(req.getSearch()))
                        .build());
        return ResponseEntity.ok(ApiResponsePagination.<List<TransactionResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.transactions(resp.getDataList()))
                .pagination(ProtoMappers.fromProtoPagination(resp.getPaginationMeta()))
                .build());
    }

    @GetMapping("/monthly-success")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TransactionMonthlyAmountSuccessResponse>>> getMonthlyAmountSuccess(
            @ModelAttribute MonthAmountTransactionRequest req) {
        var resp = transactionStatsAmountStub.findMonthlyTransactionSuccess(
                com.sanedge.pointofsale.pb.transaction.FindYearMonthTransaction.newBuilder()
                        .setYear(nvl(req.getYear()))
                        .setMonth(nvl(req.getMonth()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<TransactionMonthlyAmountSuccessResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::txnMonthlyAmountSuccess).toList())
                .build());
    }

    @GetMapping("/yearly-success")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TransactionYearlyAmountSuccessResponse>>> getYearlyAmountSuccess(
            @RequestParam Integer year) {
        var resp = transactionStatsAmountStub.findYearlyTransactionSuccess(
                com.sanedge.pointofsale.pb.transaction.FindYearTransaction.newBuilder()
                        .setYear(year)
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<TransactionYearlyAmountSuccessResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::txnYearlyAmountSuccess).toList())
                .build());
    }

    @GetMapping("/monthly-failed")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TransactionMonthlyAmountFailedResponse>>> getMonthlyAmountFailed(
            @ModelAttribute MonthAmountTransactionRequest req) {
        var resp = transactionStatsAmountStub.findMonthlyTransactionFailed(
                com.sanedge.pointofsale.pb.transaction.FindYearMonthTransaction.newBuilder()
                        .setYear(nvl(req.getYear()))
                        .setMonth(nvl(req.getMonth()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<TransactionMonthlyAmountFailedResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::txnMonthlyAmountFailed).toList())
                .build());
    }

    @GetMapping("/yearly-failed")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TransactionYearlyAmountFailedResponse>>> getYearlyAmountFailed(
            @RequestParam Integer year) {
        var resp = transactionStatsAmountStub.findYearlyTransactionFailed(
                com.sanedge.pointofsale.pb.transaction.FindYearTransaction.newBuilder()
                        .setYear(year)
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<TransactionYearlyAmountFailedResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::txnYearlyAmountFailed).toList())
                .build());
    }

    @GetMapping("/merchant/monthly-success")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TransactionMonthlyAmountSuccessResponse>>> getMonthlyAmountSuccessByMerchant(
            @ModelAttribute MonthAmountTransactionMerchant req) {
        var resp = transactionStatsAmountStub.findMonthlyTransactionSuccessByMerchant(
                com.sanedge.pointofsale.pb.transaction.FindYearMonthTransactionByMerchant.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setYear(nvl(req.getYear()))
                        .setMonth(nvl(req.getMonth()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<TransactionMonthlyAmountSuccessResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::txnMonthlyAmountSuccess).toList())
                .build());
    }

    @GetMapping("/merchant/yearly-success")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TransactionYearlyAmountSuccessResponse>>> getYearlyAmountSuccessByMerchant(
            @ModelAttribute YearAmountTransactionMerchant req) {
        var resp = transactionStatsAmountStub.findYearlyTransactionSuccessByMerchant(
                com.sanedge.pointofsale.pb.transaction.FindYearTransactionByMerchant.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setYear(nvl(req.getYear()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<TransactionYearlyAmountSuccessResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::txnYearlyAmountSuccess).toList())
                .build());
    }

    @GetMapping("/merchant/monthly-failed")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TransactionMonthlyAmountFailedResponse>>> getMonthlyAmountFailedByMerchant(
            @ModelAttribute MonthAmountTransactionMerchant req) {
        var resp = transactionStatsAmountStub.findMonthlyTransactionFailedByMerchant(
                com.sanedge.pointofsale.pb.transaction.FindYearMonthTransactionByMerchant.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setYear(nvl(req.getYear()))
                        .setMonth(nvl(req.getMonth()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<TransactionMonthlyAmountFailedResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::txnMonthlyAmountFailed).toList())
                .build());
    }

    @GetMapping("/merchant/yearly-failed")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TransactionYearlyAmountFailedResponse>>> getYearlyAmountFailedByMerchant(
            @ModelAttribute YearAmountTransactionMerchant req) {
        var resp = transactionStatsAmountStub.findYearlyTransactionFailedByMerchant(
                com.sanedge.pointofsale.pb.transaction.FindYearTransactionByMerchant.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setYear(nvl(req.getYear()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<TransactionYearlyAmountFailedResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::txnYearlyAmountFailed).toList())
                .build());
    }

    @GetMapping("/monthly-method-success")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TransactionMonthlyMethodResponse>>> getMonthlyMethodSuccess(
            @ModelAttribute MonthMethodTransactionRequest req) {
        var resp = transactionStatsMethodStub.findMonthlyMethodSuccess(
                com.sanedge.pointofsale.pb.transaction.FindYearMonthTransaction.newBuilder()
                        .setYear(nvl(req.getYear()))
                        .setMonth(nvl(req.getMonth()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<TransactionMonthlyMethodResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::txnMonthlyMethod).toList())
                .build());
    }

    @GetMapping("/yearly-method-success")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TransactionYearlyMethodResponse>>> getYearlyMethodSuccess(
            @RequestParam Integer year) {
        var resp = transactionStatsMethodStub.findYearlyMethodSuccess(
                com.sanedge.pointofsale.pb.transaction.FindYearTransaction.newBuilder()
                        .setYear(year)
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<TransactionYearlyMethodResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::txnYearlyMethod).toList())
                .build());
    }

    @GetMapping("/monthly-method-failed")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TransactionMonthlyMethodResponse>>> getMonthlyMethodFailed(
            @ModelAttribute MonthMethodTransactionRequest req) {
        var resp = transactionStatsMethodStub.findMonthlyMethodFailed(
                com.sanedge.pointofsale.pb.transaction.FindYearMonthTransaction.newBuilder()
                        .setYear(nvl(req.getYear()))
                        .setMonth(nvl(req.getMonth()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<TransactionMonthlyMethodResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::txnMonthlyMethod).toList())
                .build());
    }

    @GetMapping("/yearly-method-failed")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TransactionYearlyMethodResponse>>> getYearlyMethodFailed(
            @RequestParam Integer year) {
        var resp = transactionStatsMethodStub.findYearlyMethodFailed(
                com.sanedge.pointofsale.pb.transaction.FindYearTransaction.newBuilder()
                        .setYear(year)
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<TransactionYearlyMethodResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::txnYearlyMethod).toList())
                .build());
    }

    @GetMapping("/merchant/monthly-method-success")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TransactionMonthlyMethodResponse>>> getMonthlyMethodByMerchantSuccess(
            @ModelAttribute MonthMethodTransactionMerchantRequest req) {
        var resp = transactionStatsMethodStub.findMonthlyMethodSuccessByMerchant(
                com.sanedge.pointofsale.pb.transaction.FindYearMonthTransactionByMerchant.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setYear(nvl(req.getYear()))
                        .setMonth(nvl(req.getMonth()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<TransactionMonthlyMethodResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::txnMonthlyMethod).toList())
                .build());
    }

    @GetMapping("/merchant/yearly-method-success")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TransactionYearlyMethodResponse>>> getYearlyMethodByMerchantSuccess(
            @ModelAttribute YearMethodTransactionMerchantRequest req) {
        var resp = transactionStatsMethodStub.findYearlyMethodSuccessByMerchant(
                com.sanedge.pointofsale.pb.transaction.FindYearTransactionByMerchant.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setYear(nvl(req.getYear()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<TransactionYearlyMethodResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::txnYearlyMethod).toList())
                .build());
    }

    @GetMapping("/merchant/monthly-method-failed")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TransactionMonthlyMethodResponse>>> getMonthlyMethodByMerchantFailed(
            @ModelAttribute MonthMethodTransactionMerchantRequest req) {
        var resp = transactionStatsMethodStub.findMonthlyMethodFailedByMerchant(
                com.sanedge.pointofsale.pb.transaction.FindYearMonthTransactionByMerchant.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setYear(nvl(req.getYear()))
                        .setMonth(nvl(req.getMonth()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<TransactionMonthlyMethodResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::txnMonthlyMethod).toList())
                .build());
    }

    @GetMapping("/merchant/yearly-method-failed")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<TransactionYearlyMethodResponse>>> getYearlyMethodByMerchantFailed(
            @ModelAttribute YearMethodTransactionMerchantRequest req) {
        var resp = transactionStatsMethodStub.findYearlyMethodFailedByMerchant(
                com.sanedge.pointofsale.pb.transaction.FindYearTransactionByMerchant.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setYear(nvl(req.getYear()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<TransactionYearlyMethodResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::txnYearlyMethod).toList())
                .build());
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request) {
        var resp = transactionCommandStub.createTransaction(
                com.sanedge.pointofsale.pb.transaction.CreateTransactionRequest.newBuilder()
                        .setOrderId(nvl(request.getOrderID()))
                        .setMerchantId(nvl(request.getMerchantID()))
                        .setPaymentMethod(nvl(request.getPaymentMethod()))
                        .setAmount(nvl(request.getAmount()))
                        .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<TransactionResponse>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.transaction(resp.getData()))
                .build());
    }

    @PostMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateTransactionRequest request) {
        request.setTransactionID(id);
        var resp = transactionCommandStub.updateTransaction(
                com.sanedge.pointofsale.pb.transaction.UpdateTransactionRequest.newBuilder()
                        .setTransactionId(nvl(request.getTransactionID()))
                        .setOrderId(nvl(request.getOrderID()))
                        .setMerchantId(nvl(request.getMerchantID()))
                        .setPaymentMethod(nvl(request.getPaymentMethod()))
                        .setAmount(nvl(request.getAmount()))
                        .setPaymentStatus(nvl(request.getPaymentStatus()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<TransactionResponse>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.transaction(resp.getData()))
                .build());
    }

    @PostMapping("/trashed/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<TransactionResponseDeleteAt>> trashTransaction(@PathVariable Integer id) {
        var resp = transactionCommandStub.trashedTransaction(
                com.sanedge.pointofsale.pb.transaction.FindByIdTransactionRequest.newBuilder()
                        .setTransactionId(id)
                        .build());
        return ResponseEntity.ok(ApiResponse.<TransactionResponseDeleteAt>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.transactionDeleteAt(resp.getData()))
                .build());
    }

    @PostMapping("/restore/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<TransactionResponseDeleteAt>> restoreTransaction(@PathVariable Integer id) {
        var resp = transactionCommandStub.restoreTransaction(
                com.sanedge.pointofsale.pb.transaction.FindByIdTransactionRequest.newBuilder()
                        .setTransactionId(id)
                        .build());
        return ResponseEntity.ok(ApiResponse.<TransactionResponseDeleteAt>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.transactionDeleteAt(resp.getData()))
                .build());
    }

    @DeleteMapping("/permanent/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> deleteTransaction(@PathVariable Integer id) {
        var resp = transactionCommandStub.deleteTransactionPermanent(
                com.sanedge.pointofsale.pb.transaction.FindByIdTransactionRequest.newBuilder()
                        .setTransactionId(id)
                        .build());
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getSuccess())
                .build());
    }

    @PostMapping("/restore/all")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> restoreAllTrashedTransactions() {
        var resp = transactionCommandStub.restoreAllTransaction(Empty.getDefaultInstance());
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getSuccess())
                .build());
    }

    @PostMapping("/permanent/all")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> deleteAllTrashedTransactions() {
        var resp = transactionCommandStub.deleteAllTransactionPermanent(Empty.getDefaultInstance());
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
