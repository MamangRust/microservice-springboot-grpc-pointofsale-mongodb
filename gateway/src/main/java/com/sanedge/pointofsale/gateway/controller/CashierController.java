package com.sanedge.pointofsale.gateway.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
import com.sanedge.pointofsale.gateway.dto.requests.CreateCashierRequest;
import com.sanedge.pointofsale.gateway.dto.requests.FindAllCashierMerchant;
import com.sanedge.pointofsale.gateway.dto.requests.FindAllCashiers;
import com.sanedge.pointofsale.gateway.dto.requests.MonthCashierIdRequest;
import com.sanedge.pointofsale.gateway.dto.requests.MonthCashierMerchantRequest;
import com.sanedge.pointofsale.gateway.dto.requests.MonthTotalSales;
import com.sanedge.pointofsale.gateway.dto.requests.MonthTotalSalesCashier;
import com.sanedge.pointofsale.gateway.dto.requests.MonthTotalSalesMerchant;
import com.sanedge.pointofsale.gateway.dto.requests.UpdateCashierRequest;
import com.sanedge.pointofsale.gateway.dto.requests.YearCashierIdRequest;
import com.sanedge.pointofsale.gateway.dto.requests.YearCashierMerchantRequest;
import com.sanedge.pointofsale.gateway.dto.requests.YearTotalSalesCashier;
import com.sanedge.pointofsale.gateway.dto.requests.YearTotalSalesMerchant;
import com.sanedge.pointofsale.gateway.dto.responses.CashierResponse;
import com.sanedge.pointofsale.gateway.dto.responses.CashierResponseDeleteAt;
import com.sanedge.pointofsale.gateway.dto.responses.CashierResponseMonthSales;
import com.sanedge.pointofsale.gateway.dto.responses.CashierResponseMonthTotalSales;
import com.sanedge.pointofsale.gateway.dto.responses.CashierResponseYearSales;
import com.sanedge.pointofsale.gateway.dto.responses.CashierResponseYearTotalSales;
import com.sanedge.pointofsale.gateway.mapper.ProtoMapper;
import com.sanedge.pointofsale.pb.cashier.CashierCommandServiceGrpc;
import com.sanedge.pointofsale.pb.cashier.CashierQueryServiceGrpc;
import com.sanedge.pointofsale.pb.cashier.stats.CashierSalesServiceGrpc;
import com.sanedge.pointofsale.pb.cashier.stats.CashierTotalSalesServiceGrpc;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cashier")
@RequiredArgsConstructor
public class CashierController {
    private final CashierQueryServiceGrpc.CashierQueryServiceBlockingStub cashierQueryStub;
    private final CashierCommandServiceGrpc.CashierCommandServiceBlockingStub cashierCommandStub;
    private final CashierSalesServiceGrpc.CashierSalesServiceBlockingStub cashierSalesStub;
    private final CashierTotalSalesServiceGrpc.CashierTotalSalesServiceBlockingStub cashierTotalSalesStub;

    @GetMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponsePagination<List<CashierResponse>> findAll(FindAllCashiers req) {
        var resp = cashierQueryStub.findAll(com.sanedge.pointofsale.pb.cashier.FindAllCashierRequest.newBuilder()
                .setPage(nvl(req.getPage()))
                .setPageSize(nvl(req.getPageSize()))
                .setSearch(nvl(req.getSearch()))
                .build());
        return ApiResponsePagination.<List<CashierResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.cashiers(resp.getDataList()))
                .pagination(ProtoMappers.fromProtoPagination(resp.getPaginationMeta()))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<CashierResponse> findById(@PathVariable Integer id) {
        var resp = cashierQueryStub.findById(com.sanedge.pointofsale.pb.cashier.FindByIdCashierRequest.newBuilder()
                .setCashierId(id)
                .build());
        return ApiResponse.<CashierResponse>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.cashier(resp.getData()))
                .build();
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponsePagination<List<CashierResponseDeleteAt>> findByActive(FindAllCashiers req) {
        var resp = cashierQueryStub.findByActive(com.sanedge.pointofsale.pb.cashier.FindAllCashierRequest.newBuilder()
                .setPage(nvl(req.getPage()))
                .setPageSize(nvl(req.getPageSize()))
                .setSearch(nvl(req.getSearch()))
                .build());
        return ApiResponsePagination.<List<CashierResponseDeleteAt>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.cashiersDeleteAt(resp.getDataList()))
                .pagination(ProtoMappers.fromProtoPagination(resp.getPaginationMeta()))
                .build();
    }

    @GetMapping("/trashed")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponsePagination<List<CashierResponseDeleteAt>> findByTrashed(FindAllCashiers req) {
        var resp = cashierQueryStub.findByTrashed(com.sanedge.pointofsale.pb.cashier.FindAllCashierRequest.newBuilder()
                .setPage(nvl(req.getPage()))
                .setPageSize(nvl(req.getPageSize()))
                .setSearch(nvl(req.getSearch()))
                .build());
        return ApiResponsePagination.<List<CashierResponseDeleteAt>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.cashiersDeleteAt(resp.getDataList()))
                .pagination(ProtoMappers.fromProtoPagination(resp.getPaginationMeta()))
                .build();
    }

    @GetMapping("/merchant")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponsePagination<List<CashierResponse>> findByMerchant(FindAllCashierMerchant req) {
        var resp = cashierQueryStub.findByMerchant(
                com.sanedge.pointofsale.pb.cashier.FindByMerchantCashierRequest.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setPage(nvl(req.getPage()))
                        .setPageSize(nvl(req.getPageSize()))
                        .setSearch(nvl(req.getSearch()))
                        .build());
        return ApiResponsePagination.<List<CashierResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.cashiers(resp.getDataList()))
                .pagination(ProtoMappers.fromProtoPagination(resp.getPaginationMeta()))
                .build();
    }

    @GetMapping("/monthly-sales")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<List<CashierResponseMonthSales>> findMonthSales(@RequestParam Integer year) {
        var resp = cashierSalesStub.findMonthSales(com.sanedge.pointofsale.pb.cashier.FindYearCashier.newBuilder()
                .setYear(year)
                .build());
        return ApiResponse.<List<CashierResponseMonthSales>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::cashierMonthSales).toList())
                .build();
    }

    @GetMapping("/yearly-sales")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<List<CashierResponseYearSales>> findYearSales(@RequestParam Integer year) {
        var resp = cashierSalesStub.findYearSales(com.sanedge.pointofsale.pb.cashier.FindYearCashier.newBuilder()
                .setYear(year)
                .build());
        return ApiResponse.<List<CashierResponseYearSales>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::cashierYearSales).toList())
                .build();
    }

    @GetMapping("/monthly-total-sales")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<List<CashierResponseMonthTotalSales>> findMonthlyTotalSales(MonthTotalSales req) {
        var resp = cashierTotalSalesStub.findMonthlyTotalSales(
                com.sanedge.pointofsale.pb.cashier.FindYearMonthTotalSales.newBuilder()
                        .setYear(nvl(req.getYear()))
                        .setMonth(nvl(req.getMonth()))
                        .build());
        return ApiResponse.<List<CashierResponseMonthTotalSales>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::cashierMonthTotalSales).toList())
                .build();
    }

    @GetMapping("/yearly-total-sales")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<List<CashierResponseYearTotalSales>> findYearTotalSales(@RequestParam Integer year) {
        var resp = cashierTotalSalesStub.findYearlyTotalSales(
                com.sanedge.pointofsale.pb.cashier.FindYearTotalSales.newBuilder()
                        .setYear(year)
                        .build());
        return ApiResponse.<List<CashierResponseYearTotalSales>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::cashierYearTotalSales).toList())
                .build();
    }

    @GetMapping("/mycashier/monthly-sales")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<List<CashierResponseMonthSales>> findMonthSalesById(MonthCashierIdRequest req) {
        var resp = cashierSalesStub.findMonthSalesById(
                com.sanedge.pointofsale.pb.cashier.FindYearCashierById.newBuilder()
                        .setCashierId(nvl(req.getCashierId()))
                        .setYear(nvl(req.getYear()))
                        .build());
        return ApiResponse.<List<CashierResponseMonthSales>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::cashierMonthSales).toList())
                .build();
    }

    @GetMapping("/mycashier/yearly-sales")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<List<CashierResponseYearSales>> findYearSalesById(YearCashierIdRequest req) {
        var resp = cashierSalesStub.findYearSalesById(
                com.sanedge.pointofsale.pb.cashier.FindYearCashierById.newBuilder()
                        .setCashierId(nvl(req.getCashierId()))
                        .setYear(nvl(req.getYear()))
                        .build());
        return ApiResponse.<List<CashierResponseYearSales>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::cashierYearSales).toList())
                .build();
    }

    @GetMapping("/mycashier/monthly-total-sales")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<List<CashierResponseMonthTotalSales>> findMonthlyTotalSalesById(MonthTotalSalesCashier req) {
        var resp = cashierTotalSalesStub.findMonthlyTotalSalesById(
                com.sanedge.pointofsale.pb.cashier.FindYearMonthTotalSalesById.newBuilder()
                        .setCashierId(nvl(req.getCashierId()))
                        .setYear(nvl(req.getYear()))
                        .setMonth(nvl(req.getMonth()))
                        .build());
        return ApiResponse.<List<CashierResponseMonthTotalSales>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::cashierMonthTotalSales).toList())
                .build();
    }

    @GetMapping("/mycashier/yearly-total-sales")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<List<CashierResponseYearTotalSales>> findYearlyTotalSalesById(YearTotalSalesCashier req) {
        var resp = cashierTotalSalesStub.findYearlyTotalSalesById(
                com.sanedge.pointofsale.pb.cashier.FindYearTotalSalesById.newBuilder()
                        .setCashierId(nvl(req.getCashierId()))
                        .setYear(nvl(req.getYear()))
                        .build());
        return ApiResponse.<List<CashierResponseYearTotalSales>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::cashierYearTotalSales).toList())
                .build();
    }

    @GetMapping("/merchant/monthly-sales")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<List<CashierResponseMonthSales>> findMonthSalesByMerchant(MonthCashierMerchantRequest req) {
        var resp = cashierSalesStub.findMonthSalesByMerchant(
                com.sanedge.pointofsale.pb.cashier.FindYearCashierByMerchant.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setYear(nvl(req.getYear()))
                        .build());
        return ApiResponse.<List<CashierResponseMonthSales>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::cashierMonthSales).toList())
                .build();
    }

    @GetMapping("/merchant/yearly-sales")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<List<CashierResponseYearSales>> findYearSalesByMerchant(YearCashierMerchantRequest req) {
        var resp = cashierSalesStub.findYearSalesByMerchant(
                com.sanedge.pointofsale.pb.cashier.FindYearCashierByMerchant.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setYear(nvl(req.getYear()))
                        .build());
        return ApiResponse.<List<CashierResponseYearSales>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::cashierYearSales).toList())
                .build();
    }

    @GetMapping("/merchant/monthly-total-sales")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<List<CashierResponseMonthTotalSales>> findMonthlyTotalSalesByMerchant(
            MonthTotalSalesMerchant req) {
        var resp = cashierTotalSalesStub.findMonthlyTotalSalesByMerchant(
                com.sanedge.pointofsale.pb.cashier.FindYearMonthTotalSalesByMerchant.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setYear(nvl(req.getYear()))
                        .setMonth(nvl(req.getMonth()))
                        .build());
        return ApiResponse.<List<CashierResponseMonthTotalSales>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::cashierMonthTotalSales).toList())
                .build();
    }

    @GetMapping("/merchant/yearly-total-sales")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<List<CashierResponseYearTotalSales>> findYearlyTotalSalesByMerchant(YearTotalSalesMerchant req) {
        var resp = cashierTotalSalesStub.findYearlyTotalSalesByMerchant(
                com.sanedge.pointofsale.pb.cashier.FindYearTotalSalesByMerchant.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setYear(nvl(req.getYear()))
                        .build());
        return ApiResponse.<List<CashierResponseYearTotalSales>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::cashierYearTotalSales).toList())
                .build();
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<CashierResponse> createCashier(@RequestBody CreateCashierRequest req) {
        var resp = cashierCommandStub.createCashier(
                com.sanedge.pointofsale.pb.cashier.CreateCashierRequest.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setUserId(nvl(req.getUserId()))
                        .setName(nvl(req.getName()))
                        .build());
        return ApiResponse.<CashierResponse>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.cashier(resp.getData()))
                .build();
    }

    @PostMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<CashierResponse> updateCashier(@PathVariable Integer id,
            @RequestBody UpdateCashierRequest req) {
        req.setCashierId(id);
        var resp = cashierCommandStub.updateCashier(
                com.sanedge.pointofsale.pb.cashier.UpdateCashierRequest.newBuilder()
                        .setCashierId(nvl(req.getCashierId()))
                        .setName(nvl(req.getName()))
                        .build());
        return ApiResponse.<CashierResponse>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.cashier(resp.getData()))
                .build();
    }

    @PostMapping("/trashed/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<CashierResponseDeleteAt> trashedCashier(@PathVariable Integer id) {
        var resp = cashierCommandStub.trashedCashier(
                com.sanedge.pointofsale.pb.cashier.FindByIdCashierRequest.newBuilder()
                        .setCashierId(id)
                        .build());
        return ApiResponse.<CashierResponseDeleteAt>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.cashierDeleteAt(resp.getData()))
                .build();
    }

    @PostMapping("/restore/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<CashierResponseDeleteAt> restoreCashier(@PathVariable Integer id) {
        var resp = cashierCommandStub.restoreCashier(
                com.sanedge.pointofsale.pb.cashier.FindByIdCashierRequest.newBuilder()
                        .setCashierId(id)
                        .build());
        return ApiResponse.<CashierResponseDeleteAt>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.cashierDeleteAt(resp.getData()))
                .build();
    }

    @DeleteMapping("/permanent/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<Boolean> deleteCashierPermanent(@PathVariable Integer id) {
        var resp = cashierCommandStub.deleteCashierPermanent(
                com.sanedge.pointofsale.pb.cashier.FindByIdCashierRequest.newBuilder()
                        .setCashierId(id)
                        .build());
        return ApiResponse.<Boolean>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getSuccess())
                .build();
    }

    @PostMapping("/restore/all")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<Boolean> restoreAllCashier() {
        var resp = cashierCommandStub.restoreAllCashier(Empty.getDefaultInstance());
        return ApiResponse.<Boolean>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getSuccess())
                .build();
    }

    @PostMapping("/permanent/all")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<Boolean> deleteAllCashierPermanent() {
        var resp = cashierCommandStub.deleteAllCashierPermanent(Empty.getDefaultInstance());
        return ApiResponse.<Boolean>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getSuccess())
                .build();
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }
}
