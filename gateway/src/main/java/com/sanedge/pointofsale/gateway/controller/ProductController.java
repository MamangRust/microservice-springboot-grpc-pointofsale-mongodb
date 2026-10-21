package com.sanedge.pointofsale.gateway.controller;

import java.io.File;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.protobuf.Empty;
import com.sanedge.pointofsale.common.domain.responses.api.ApiResponse;
import com.sanedge.pointofsale.common.domain.responses.api.ApiResponsePagination;
import com.sanedge.pointofsale.common.grpc.ProtoMappers;
import com.sanedge.pointofsale.gateway.dto.requests.CreateProductRequest;
import com.sanedge.pointofsale.gateway.dto.requests.FindAllProductByCategoryRequest;
import com.sanedge.pointofsale.gateway.dto.requests.FindAllProductByMerchantRequest;
import com.sanedge.pointofsale.gateway.dto.requests.FindAllProductRequest;
import com.sanedge.pointofsale.gateway.dto.requests.UpdateProductRequest;
import com.sanedge.pointofsale.gateway.dto.responses.ProductResponse;
import com.sanedge.pointofsale.gateway.dto.responses.ProductResponseDeleteAt;
import com.sanedge.pointofsale.gateway.mapper.ProtoMapper;
import com.sanedge.pointofsale.gateway.service.FileService;
import com.sanedge.pointofsale.gateway.service.FolderService;
import com.sanedge.pointofsale.pb.product.ProductCommandServiceGrpc;
import com.sanedge.pointofsale.pb.product.ProductQueryServiceGrpc;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product")
public class ProductController {
    private final ProductQueryServiceGrpc.ProductQueryServiceBlockingStub productQueryStub;
    private final ProductCommandServiceGrpc.ProductCommandServiceBlockingStub productCommandStub;
    private final FileService fileService;
    private final FolderService folderService;

    private static final String PRODUCT_BASE_PATH = "static/product";

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponsePagination<List<ProductResponse>>> findAll(
            @ModelAttribute FindAllProductRequest req) {
        var resp = productQueryStub.findAll(com.sanedge.pointofsale.pb.product.FindAllProductRequest.newBuilder()
                .setPage(nvl(req.getPage()))
                .setPageSize(nvl(req.getPageSize()))
                .setSearch(nvl(req.getSearch()))
                .build());
        return ResponseEntity.ok(ApiResponsePagination.<List<ProductResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.products(resp.getDataList()))
                .pagination(ProtoMappers.fromProtoPagination(resp.getPaginationMeta()))
                .build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<ProductResponse>> findById(@PathVariable Long id) {
        var resp = productQueryStub.findById(com.sanedge.pointofsale.pb.product.FindByIdProductRequest.newBuilder()
                .setProductId(id.intValue())
                .build());
        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.product(resp.getData()))
                .build());
    }

    @GetMapping("/merchant/{merchant_id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponsePagination<List<ProductResponse>>> findByMerchant(
            @PathVariable("merchant_id") Integer merchantId,
            @ModelAttribute FindAllProductByMerchantRequest req) {
        var resp = productQueryStub.findByMerchant(
                com.sanedge.pointofsale.pb.product.FindByMerchantProductRequest.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setSearch(nvl(req.getSearch()))
                        .setCategoryId(nvl(req.getCategoryId()))
                        .setMinPrice(nvl(req.getMinPrice()))
                        .setMaxPrice(nvl(req.getMaxPrice()))
                        .setPage(nvl(req.getPage()))
                        .setPageSize(nvl(req.getPageSize()))
                        .build());
        return ResponseEntity.ok(ApiResponsePagination.<List<ProductResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.products(resp.getDataList()))
                .pagination(ProtoMappers.fromProtoPagination(resp.getPaginationMeta()))
                .build());
    }

    @GetMapping("/category/{category_name}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponsePagination<List<ProductResponse>>> findByCategory(
            @PathVariable("category_name") String categoryName,
            @ModelAttribute FindAllProductByCategoryRequest req) {
        var resp = productQueryStub.findByCategory(
                com.sanedge.pointofsale.pb.product.FindByCategoryProductRequest.newBuilder()
                        .setCategoryName(nvl(req.getCategoryName()))
                        .setSearch(nvl(req.getSearch()))
                        .setMinPrice(nvl(req.getMinPrice()))
                        .setMaxPrice(nvl(req.getMaxPrice()))
                        .setPage(nvl(req.getPage()))
                        .setPageSize(nvl(req.getPageSize()))
                        .build());
        return ResponseEntity.ok(ApiResponsePagination.<List<ProductResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.products(resp.getDataList()))
                .pagination(ProtoMappers.fromProtoPagination(resp.getPaginationMeta()))
                .build());
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponsePagination<List<ProductResponseDeleteAt>>> findActive(
            @ModelAttribute FindAllProductRequest req) {
        var resp = productQueryStub.findByActive(com.sanedge.pointofsale.pb.product.FindAllProductRequest.newBuilder()
                .setPage(nvl(req.getPage()))
                .setPageSize(nvl(req.getPageSize()))
                .setSearch(nvl(req.getSearch()))
                .build());
        return ResponseEntity.ok(ApiResponsePagination.<List<ProductResponseDeleteAt>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.productsDeleteAt(resp.getDataList()))
                .pagination(ProtoMappers.fromProtoPagination(resp.getPaginationMeta()))
                .build());
    }

    @GetMapping("/trashed")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponsePagination<List<ProductResponseDeleteAt>>> findTrashed(
            @ModelAttribute FindAllProductRequest req) {
        var resp = productQueryStub.findByTrashed(com.sanedge.pointofsale.pb.product.FindAllProductRequest.newBuilder()
                .setPage(nvl(req.getPage()))
                .setPageSize(nvl(req.getPageSize()))
                .setSearch(nvl(req.getSearch()))
                .build());
        return ResponseEntity.ok(ApiResponsePagination.<List<ProductResponseDeleteAt>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.productsDeleteAt(resp.getDataList()))
                .pagination(ProtoMappers.fromProtoPagination(resp.getPaginationMeta()))
                .build());
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @ModelAttribute CreateProductRequest req) {
        String folderPath = folderService.createFolder(PRODUCT_BASE_PATH, req.getSlugProduct());
        if (folderPath == null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<ProductResponse>builder()
                    .status("error")
                    .message("Failed to create folder for product")
                    .data(null)
                    .build());
        }

        String filePath = folderPath + File.separator + req.getImageProduct().getOriginalFilename();
        String savedPath = fileService.createFileImage(req.getImageProduct(), filePath);
        if (savedPath == null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<ProductResponse>builder()
                    .status("error")
                    .message("Failed to save product image")
                    .data(null)
                    .build());
        }

        var resp = productCommandStub.createProduct(
                com.sanedge.pointofsale.pb.product.CreateProductRequest.newBuilder()
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setCategoryId(nvl(req.getCategoryId()))
                        .setName(nvl(req.getName()))
                        .setDescription(nvl(req.getDescription()))
                        .setPrice(nvl(req.getPrice()))
                        .setCountInStock(nvl(req.getCountInStock()))
                        .setBrand(nvl(req.getBrand()))
                        .setWeight(String.valueOf(nvl(req.getWeight())))
                        .setRating(nvl(req.getRating()))
                        .setSlugProduct(nvl(req.getSlugProduct()))
                        .setImageProduct(savedPath)
                        .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<ProductResponse>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.product(resp.getData()))
                .build());
    }

    @PostMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Integer id,
            @Valid @ModelAttribute UpdateProductRequest req) {
        req.setProductId(id);

        String imageProduct = "";
        if (req.getImageProduct() != null && !req.getImageProduct().isEmpty()) {
            String folderPath = folderService.createFolder(PRODUCT_BASE_PATH, req.getSlugProduct());
            if (folderPath != null) {
                String filePath = folderPath + File.separator + req.getImageProduct().getOriginalFilename();
                String savedPath = fileService.createFileImage(req.getImageProduct(), filePath);
                if (savedPath != null) {
                    imageProduct = savedPath;
                }
            }
        }

        var resp = productCommandStub.updateProduct(
                com.sanedge.pointofsale.pb.product.UpdateProductRequest.newBuilder()
                        .setProductId(nvl(req.getProductId()))
                        .setMerchantId(nvl(req.getMerchantId()))
                        .setCategoryId(nvl(req.getCategoryId()))
                        .setName(nvl(req.getName()))
                        .setDescription(nvl(req.getDescription()))
                        .setPrice(nvl(req.getPrice()))
                        .setCountInStock(nvl(req.getCountInStock()))
                        .setBrand(nvl(req.getBrand()))
                        .setWeight(String.valueOf(nvl(req.getWeight())))
                        .setRating(nvl(req.getRating()))
                        .setSlugProduct(nvl(req.getSlugProduct()))
                        .setImageProduct(imageProduct)
                        .build());
        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.product(resp.getData()))
                .build());
    }

    @PostMapping("/trashed/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponseDeleteAt>> trashedProduct(@PathVariable Integer id) {
        var resp = productCommandStub.trashedProduct(
                com.sanedge.pointofsale.pb.product.FindByIdProductRequest.newBuilder()
                        .setProductId(id)
                        .build());
        return ResponseEntity.ok(ApiResponse.<ProductResponseDeleteAt>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.productDeleteAt(resp.getData()))
                .build());
    }

    @PostMapping("/restore/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponseDeleteAt>> restoreProduct(@PathVariable Integer id) {
        var resp = productCommandStub.restoreProduct(
                com.sanedge.pointofsale.pb.product.FindByIdProductRequest.newBuilder()
                        .setProductId(id)
                        .build());
        return ResponseEntity.ok(ApiResponse.<ProductResponseDeleteAt>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.productDeleteAt(resp.getData()))
                .build());
    }

    @DeleteMapping("/permanent/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> deleteProductPermanent(@PathVariable Integer id) {
        var resp = productCommandStub.deleteProductPermanent(
                com.sanedge.pointofsale.pb.product.FindByIdProductRequest.newBuilder()
                        .setProductId(id)
                        .build());
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getSuccess())
                .build());
    }

    @PostMapping("/restore/all")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> restoreAllProducts() {
        var resp = productCommandStub.restoreAllProduct(Empty.getDefaultInstance());
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getSuccess())
                .build());
    }

    @PostMapping("/permanent/all")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> deleteAllProductsPermanent() {
        var resp = productCommandStub.deleteAllProductPermanent(Empty.getDefaultInstance());
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
