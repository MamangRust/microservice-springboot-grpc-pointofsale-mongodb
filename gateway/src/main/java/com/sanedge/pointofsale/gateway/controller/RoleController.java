package com.sanedge.pointofsale.gateway.controller;

import java.util.List;

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
import com.sanedge.pointofsale.gateway.dto.requests.CreateRoleRequest;
import com.sanedge.pointofsale.gateway.dto.requests.FindAllRoles;
import com.sanedge.pointofsale.gateway.dto.requests.UpdateRoleRequest;
import com.sanedge.pointofsale.gateway.dto.responses.RoleResponse;
import com.sanedge.pointofsale.gateway.dto.responses.RoleResponseDeleteAt;
import com.sanedge.pointofsale.gateway.mapper.ProtoMapper;
import com.sanedge.pointofsale.pb.role.RoleCommandServiceGrpc;
import com.sanedge.pointofsale.pb.role.RoleQueryServiceGrpc;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {
    private final RoleQueryServiceGrpc.RoleQueryServiceBlockingStub roleQueryStub;
    private final RoleCommandServiceGrpc.RoleCommandServiceBlockingStub roleCommandStub;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponsePagination<List<RoleResponse>>> findAllRoles(
            @ModelAttribute FindAllRoles req) {
        var resp = roleQueryStub.findAll(com.sanedge.pointofsale.pb.role.FindAllRoleRequest.newBuilder()
                .setPage(nvl(req.getPage()))
                .setPageSize(nvl(req.getPageSize()))
                .setSearch(nvl(req.getSearch()))
                .build());
        return ResponseEntity.ok(ApiResponsePagination.<List<RoleResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.roles(resp.getDataList()))
                .pagination(ProtoMappers.fromProtoPagination(resp.getPaginationMeta()))
                .build());
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponsePagination<List<RoleResponseDeleteAt>>> findActiveRoles(
            @ModelAttribute FindAllRoles req) {
        var resp = roleQueryStub.findByActive(com.sanedge.pointofsale.pb.role.FindAllRoleRequest.newBuilder()
                .setPage(nvl(req.getPage()))
                .setPageSize(nvl(req.getPageSize()))
                .setSearch(nvl(req.getSearch()))
                .build());
        return ResponseEntity.ok(ApiResponsePagination.<List<RoleResponseDeleteAt>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.rolesDeleteAt(resp.getDataList()))
                .pagination(ProtoMappers.fromProtoPagination(resp.getPaginationMeta()))
                .build());
    }

    @GetMapping("/trashed")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponsePagination<List<RoleResponseDeleteAt>>> findTrashedRoles(
            @ModelAttribute FindAllRoles req) {
        var resp = roleQueryStub.findByTrashed(com.sanedge.pointofsale.pb.role.FindAllRoleRequest.newBuilder()
                .setPage(nvl(req.getPage()))
                .setPageSize(nvl(req.getPageSize()))
                .setSearch(nvl(req.getSearch()))
                .build());
        return ResponseEntity.ok(ApiResponsePagination.<List<RoleResponseDeleteAt>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.rolesDeleteAt(resp.getDataList()))
                .pagination(ProtoMappers.fromProtoPagination(resp.getPaginationMeta()))
                .build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<RoleResponse>> findRoleById(@PathVariable Integer id) {
        var resp = roleQueryStub.findById(com.sanedge.pointofsale.pb.role.FindByIdRoleRequest.newBuilder()
                .setRoleId(id)
                .build());
        return ResponseEntity.ok(ApiResponse.<RoleResponse>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.role(resp.getData()))
                .build());
    }

    @GetMapping("/by-user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> findRolesByUserId(@PathVariable Integer userId) {
        var resp = roleQueryStub.findByUserId(com.sanedge.pointofsale.pb.role.FindByUserIdRoleRequest.newBuilder()
                .setUserId(userId)
                .build());
        return ResponseEntity.ok(ApiResponse.<List<RoleResponse>>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.roles(resp.getDataList()))
                .build());
    }

    @GetMapping("/by-name")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<RoleResponse>> findRoleByName(@RequestParam String name) {
        var resp = roleQueryStub.findByName(com.sanedge.pointofsale.pb.role.FindByNameRoleRequest.newBuilder()
                .setName(name)
                .build());
        return ResponseEntity.ok(ApiResponse.<RoleResponse>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.role(resp.getData()))
                .build());
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@RequestBody CreateRoleRequest request) {
        var resp = roleCommandStub.createRole(com.sanedge.pointofsale.pb.role.CreateRoleRequest.newBuilder()
                .setName(nvl(request.getName()))
                .build());
        return ResponseEntity.ok(ApiResponse.<RoleResponse>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.role(resp.getData()))
                .build());
    }

    @PostMapping("/update")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(@RequestBody UpdateRoleRequest request) {
        var resp = roleCommandStub.updateRole(com.sanedge.pointofsale.pb.role.UpdateRoleRequest.newBuilder()
                .setRoleId(nvl(request.getId()))
                .setName(nvl(request.getName()))
                .build());
        return ResponseEntity.ok(ApiResponse.<RoleResponse>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.role(resp.getData()))
                .build());
    }

    @PostMapping("/trash/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponseDeleteAt>> trashRole(@PathVariable Integer id) {
        var resp = roleCommandStub.trashedRole(com.sanedge.pointofsale.pb.role.FindByIdRoleRequest.newBuilder()
                .setRoleId(id)
                .build());
        return ResponseEntity.ok(ApiResponse.<RoleResponseDeleteAt>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.roleDeleteAt(resp.getData()))
                .build());
    }

    @PostMapping("/restore/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponseDeleteAt>> restoreRole(@PathVariable Integer id) {
        var resp = roleCommandStub.restoreRole(com.sanedge.pointofsale.pb.role.FindByIdRoleRequest.newBuilder()
                .setRoleId(id)
                .build());
        return ResponseEntity.ok(ApiResponse.<RoleResponseDeleteAt>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.roleDeleteAt(resp.getData()))
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> deleteRole(@PathVariable Integer id) {
        var resp = roleCommandStub.deleteRolePermanent(com.sanedge.pointofsale.pb.role.FindByIdRoleRequest.newBuilder()
                .setRoleId(id)
                .build());
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getSuccess())
                .build());
    }

    @PostMapping("/restore-all")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> restoreAllRoles() {
        var resp = roleCommandStub.restoreAllRole(Empty.getDefaultInstance());
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(resp.getSuccess())
                .build());
    }

    @PostMapping("/delete-all")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> deleteAllRoles() {
        var resp = roleCommandStub.deleteAllRolePermanent(Empty.getDefaultInstance());
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
