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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.protobuf.Empty;
import com.sanedge.pointofsale.common.domain.responses.api.ApiResponse;
import com.sanedge.pointofsale.common.domain.responses.api.ApiResponsePagination;
import com.sanedge.pointofsale.common.grpc.ProtoMappers;
import com.sanedge.pointofsale.gateway.dto.requests.CreateCategoryRequest;
import com.sanedge.pointofsale.gateway.dto.requests.FindAllCategory;
import com.sanedge.pointofsale.gateway.dto.requests.MonthPriceId;
import com.sanedge.pointofsale.gateway.dto.requests.MonthPriceMerchant;
import com.sanedge.pointofsale.gateway.dto.requests.MonthTotalPrice;
import com.sanedge.pointofsale.gateway.dto.requests.MonthTotalPriceCategory;
import com.sanedge.pointofsale.gateway.dto.requests.MonthTotalPriceMerchant;
import com.sanedge.pointofsale.gateway.dto.requests.UpdateCategoryRequest;
import com.sanedge.pointofsale.gateway.dto.requests.YearPriceId;
import com.sanedge.pointofsale.gateway.dto.requests.YearPriceMerchant;
import com.sanedge.pointofsale.gateway.dto.requests.YearTotalPriceCategory;
import com.sanedge.pointofsale.gateway.dto.requests.YearTotalPriceMerchant;
import com.sanedge.pointofsale.gateway.dto.responses.CategoriesMonthPriceResponse;
import com.sanedge.pointofsale.gateway.dto.responses.CategoriesMonthlyTotalPriceResponse;
import com.sanedge.pointofsale.gateway.dto.responses.CategoriesYearPriceResponse;
import com.sanedge.pointofsale.gateway.dto.responses.CategoriesYearlyTotalPriceResponse;
import com.sanedge.pointofsale.gateway.dto.responses.CategoryResponse;
import com.sanedge.pointofsale.gateway.dto.responses.CategoryResponseDeleteAt;
import com.sanedge.pointofsale.gateway.mapper.ProtoMapper;
import com.sanedge.pointofsale.pb.category.CategoryCommandServiceGrpc;
import com.sanedge.pointofsale.pb.category.CategoryQueryServiceGrpc;
import com.sanedge.pointofsale.pb.category.stats.CategoryPriceServiceGrpc;
import com.sanedge.pointofsale.pb.category.stats.CategoryTotalPriceServiceGrpc;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/category")
public class CategoryController {
    private final CategoryQueryServiceGrpc.CategoryQueryServiceBlockingStub categoryQueryStub;
    private final CategoryCommandServiceGrpc.CategoryCommandServiceBlockingStub categoryCommandStub;
    private final CategoryTotalPriceServiceGrpc.CategoryTotalPriceServiceBlockingStub categoryTotalPriceStub;
    private final CategoryPriceServiceGrpc.CategoryPriceServiceBlockingStub categoryPriceStub;

    @GetMapping
    public ResponseEntity<ApiResponsePagination<List<CategoryResponse>>> findAll(
            @ModelAttribute FindAllCategory req) {
        var resp = categoryQueryStub.findAll(com.sanedge.pointofsale.pb.category.FindAllCategoryRequest.newBuilder()
                .setPage(nvl(req.getPage()))
                .setPageSize(nvl(req.getPageSize()))
                .setSearch(nvl(req.getSearch()))
                .build());
        return ResponseEntity.ok(ApiResponsePagination.<List<CategoryResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.categories(resp.getDataList()))
                .pagination(ProtoMappers.fromProtoPagination(resp.getPaginationMeta()))
                .build());
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponsePagination<List<CategoryResponseDeleteAt>>> findByActive(
            @ModelAttribute FindAllCategory req) {
        var resp = categoryQueryStub.findByActive(com.sanedge.pointofsale.pb.category.FindAllCategoryRequest.newBuilder()
                .setPage(nvl(req.getPage()))
                .setPageSize(nvl(req.getPageSize()))
                .setSearch(nvl(req.getSearch()))
                .build());
        return ResponseEntity.ok(ApiResponsePagination.<List<CategoryResponseDeleteAt>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.categoriesDeleteAt(resp.getDataList()))
                .pagination(ProtoMappers.fromProtoPagination(resp.getPaginationMeta()))
                .build());
    }

    @GetMapping("/trashed")
    public ResponseEntity<ApiResponsePagination<List<CategoryResponseDeleteAt>>> findByTrashed(
            @ModelAttribute FindAllCategory req) {
        var resp = categoryQueryStub.findByTrashed(com.sanedge.pointofsale.pb.category.FindAllCategoryRequest.newBuilder()
                .setPage(nvl(req.getPage()))
                .setPageSize(nvl(req.getPageSize()))
                .setSearch(nvl(req.getSearch()))
                .build());
        return ResponseEntity.ok(ApiResponsePagination.<List<CategoryResponseDeleteAt>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.categoriesDeleteAt(resp.getDataList()))
                .pagination(ProtoMappers.fromProtoPagination(resp.getPaginationMeta()))
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> findById(@PathVariable Integer id) {
        var resp = categoryQueryStub.findById(com.sanedge.pointofsale.pb.category.FindByIdCategoryRequest.newBuilder()
                .setCategoryId(id)
                .build());
        return ResponseEntity.ok(ApiResponse.<CategoryResponse>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.category(resp.getData()))
                .build());
    }

    @GetMapping("/monthly-total-pricing")
    public ResponseEntity<ApiResponse<List<CategoriesMonthlyTotalPriceResponse>>> findMonthTotalPrice(
            @ModelAttribute MonthTotalPrice req) {
        var resp = categoryTotalPriceStub.findMonthlyTotalPrices(
                com.sanedge.pointofsale.pb.category.FindYearMonthTotalPrice.newBuilder()
                        .setYear(nvl(req.getYear()))
                        .setMonth(nvl(req.getMonth()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<CategoriesMonthlyTotalPriceResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::categoryMonthTotalPrice).toList())
                .build());
    }

    @GetMapping("/yearly-total-pricing")
    public ResponseEntity<ApiResponse<List<CategoriesYearlyTotalPriceResponse>>> findYearTotalPrice(
            @RequestParam Integer year) {
        var resp = categoryTotalPriceStub.findYearlyTotalPrices(
                com.sanedge.pointofsale.pb.category.FindYearTotalPrice.newBuilder()
                        .setYear(year)
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<CategoriesYearlyTotalPriceResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::categoryYearTotalPrice).toList())
                .build());
    }

    @GetMapping("/merchant/monthly-total-pricing")
    public ResponseEntity<ApiResponse<List<CategoriesMonthlyTotalPriceResponse>>> findMonthTotalPriceByMerchant(
            @ModelAttribute MonthTotalPriceMerchant req) {
        var resp = categoryTotalPriceStub.findMonthlyTotalPricesByMerchant(
                com.sanedge.pointofsale.pb.category.FindYearMonthTotalPriceByMerchant.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setYear(nvl(req.getYear()))
                        .setMonth(nvl(req.getMonth()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<CategoriesMonthlyTotalPriceResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::categoryMonthTotalPrice).toList())
                .build());
    }

    @GetMapping("/merchant/yearly-total-pricing")
    public ResponseEntity<ApiResponse<List<CategoriesYearlyTotalPriceResponse>>> findYearTotalPriceByMerchant(
            @ModelAttribute YearTotalPriceMerchant req) {
        var resp = categoryTotalPriceStub.findYearlyTotalPricesByMerchant(
                com.sanedge.pointofsale.pb.category.FindYearTotalPriceByMerchant.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setYear(nvl(req.getYear()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<CategoriesYearlyTotalPriceResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::categoryYearTotalPrice).toList())
                .build());
    }

    @GetMapping("/mycategory/monthly-total-pricing")
    public ResponseEntity<ApiResponse<List<CategoriesMonthlyTotalPriceResponse>>> findMonthTotalPriceById(
            @ModelAttribute MonthTotalPriceCategory req) {
        var resp = categoryTotalPriceStub.findMonthlyTotalPricesById(
                com.sanedge.pointofsale.pb.category.FindYearMonthTotalPriceById.newBuilder()
                        .setCategoryId(nvl(req.getCategoryId()))
                        .setYear(nvl(req.getYear()))
                        .setMonth(nvl(req.getMonth()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<CategoriesMonthlyTotalPriceResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::categoryMonthTotalPrice).toList())
                .build());
    }

    @GetMapping("/mycategory/yearly-total-pricing")
    public ResponseEntity<ApiResponse<List<CategoriesYearlyTotalPriceResponse>>> findYearTotalPriceById(
            @ModelAttribute YearTotalPriceCategory req) {
        var resp = categoryTotalPriceStub.findYearlyTotalPricesById(
                com.sanedge.pointofsale.pb.category.FindYearTotalPriceById.newBuilder()
                        .setCategoryId(nvl(req.getCategoryId()))
                        .setYear(nvl(req.getYear()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<CategoriesYearlyTotalPriceResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::categoryYearTotalPrice).toList())
                .build());
    }

    @GetMapping("/monthly-pricing")
    public ResponseEntity<ApiResponse<List<CategoriesMonthPriceResponse>>> findMonthPrice(
            @RequestParam Integer year) {
        var resp = categoryPriceStub.findMonthPrice(com.sanedge.pointofsale.pb.category.FindYearCategory.newBuilder()
                .setYear(year)
                .build());
        return ResponseEntity.ok(ApiResponse.<List<CategoriesMonthPriceResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::categoryMonthPrice).toList())
                .build());
    }

    @GetMapping("/yearly-pricing")
    public ResponseEntity<ApiResponse<List<CategoriesYearPriceResponse>>> findYearPrice(
            @RequestParam Integer year) {
        var resp = categoryPriceStub.findYearPrice(com.sanedge.pointofsale.pb.category.FindYearCategory.newBuilder()
                .setYear(year)
                .build());
        return ResponseEntity.ok(ApiResponse.<List<CategoriesYearPriceResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::categoryYearPrice).toList())
                .build());
    }

    @GetMapping("/merchant/monthly-pricing")
    public ResponseEntity<ApiResponse<List<CategoriesMonthPriceResponse>>> findMonthPriceByMerchant(
            @ModelAttribute MonthPriceMerchant req) {
        var resp = categoryPriceStub.findMonthPriceByMerchant(
                com.sanedge.pointofsale.pb.category.FindYearCategoryByMerchant.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setYear(nvl(req.getYear()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<CategoriesMonthPriceResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::categoryMonthPrice).toList())
                .build());
    }

    @GetMapping("/merchant/yearly-pricing")
    public ResponseEntity<ApiResponse<List<CategoriesYearPriceResponse>>> findYearPriceByMerchant(
            @ModelAttribute YearPriceMerchant req) {
        var resp = categoryPriceStub.findYearPriceByMerchant(
                com.sanedge.pointofsale.pb.category.FindYearCategoryByMerchant.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setYear(nvl(req.getYear()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<CategoriesYearPriceResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::categoryYearPrice).toList())
                .build());
    }

    @GetMapping("/mycategory/monthly-pricing")
    public ResponseEntity<ApiResponse<List<CategoriesMonthPriceResponse>>> findMonthPriceById(
            @ModelAttribute MonthPriceId req) {
        var resp = categoryPriceStub.findMonthPriceById(
                com.sanedge.pointofsale.pb.category.FindYearCategoryById.newBuilder()
                        .setCategoryId(nvl(req.getCategoryId()))
                        .setYear(nvl(req.getYear()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<CategoriesMonthPriceResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::categoryMonthPrice).toList())
                .build());
    }

    @GetMapping("/mycategory/yearly-pricing")
    public ResponseEntity<ApiResponse<List<CategoriesYearPriceResponse>>> findYearPriceById(
            @ModelAttribute YearPriceId req) {
        var resp = categoryPriceStub.findYearPriceById(
                com.sanedge.pointofsale.pb.category.FindYearCategoryById.newBuilder()
                        .setCategoryId(nvl(req.getCategoryId()))
                        .setYear(nvl(req.getYear()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<List<CategoriesYearPriceResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getDataList().stream().map(ProtoMapper::categoryYearPrice).toList())
                .build());
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @ModelAttribute CreateCategoryRequest req) {
        var resp = categoryCommandStub.createCategory(
                com.sanedge.pointofsale.pb.category.CreateCategoryRequest.newBuilder()
                        .setName(nvl(req.getName()))
                        .setDescription(nvl(req.getDescription()))
                        .setSlugCategory(nvl(req.getSlugCategory()))
                        .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<CategoryResponse>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.category(resp.getData()))
                .build());
    }

    @PostMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Integer id,
            @Valid @ModelAttribute UpdateCategoryRequest req) {
        req.setCategoryId(id);
        var resp = categoryCommandStub.updateCategory(
                com.sanedge.pointofsale.pb.category.UpdateCategoryRequest.newBuilder()
                        .setCategoryId(nvl(req.getCategoryId()))
                        .setName(nvl(req.getName()))
                        .setDescription(nvl(req.getDescription()))
                        .setSlugCategory(nvl(req.getSlugCategory()))
                        .build());
        return ResponseEntity.ok(ApiResponse.<CategoryResponse>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.category(resp.getData()))
                .build());
    }

    @PostMapping("/trashed/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponseDeleteAt>> trashedCategory(@PathVariable Integer id) {
        var resp = categoryCommandStub.trashedCategory(
                com.sanedge.pointofsale.pb.category.FindByIdCategoryRequest.newBuilder()
                        .setCategoryId(id)
                        .build());
        return ResponseEntity.ok(ApiResponse.<CategoryResponseDeleteAt>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.categoryDeleteAt(resp.getData()))
                .build());
    }

    @PostMapping("/restore/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponseDeleteAt>> restoreCategory(@PathVariable Integer id) {
        var resp = categoryCommandStub.restoreCategory(
                com.sanedge.pointofsale.pb.category.FindByIdCategoryRequest.newBuilder()
                        .setCategoryId(id)
                        .build());
        return ResponseEntity.ok(ApiResponse.<CategoryResponseDeleteAt>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.categoryDeleteAt(resp.getData()))
                .build());
    }

    @DeleteMapping("/permanent/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> deleteCategoryPermanent(@PathVariable Integer id) {
        var resp = categoryCommandStub.deleteCategoryPermanent(
                com.sanedge.pointofsale.pb.category.FindByIdCategoryRequest.newBuilder()
                        .setCategoryId(id)
                        .build());
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getSuccess())
                .build());
    }

    @PostMapping("/restore/all")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> restoreAllCategories() {
        var resp = categoryCommandStub.restoreAllCategory(Empty.getDefaultInstance());
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getSuccess())
                .build());
    }

    @PostMapping("/permanent/all")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> deleteAllCategoriesPermanent() {
        var resp = categoryCommandStub.deleteAllCategoryPermanent(Empty.getDefaultInstance());
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
