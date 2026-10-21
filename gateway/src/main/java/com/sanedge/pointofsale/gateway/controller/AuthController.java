package com.sanedge.pointofsale.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sanedge.pointofsale.common.domain.responses.api.ApiResponse;
import com.sanedge.pointofsale.gateway.dto.requests.AuthRequest;
import com.sanedge.pointofsale.gateway.dto.requests.RefreshTokenRequest;
import com.sanedge.pointofsale.gateway.dto.requests.RegisterRequest;
import com.sanedge.pointofsale.gateway.dto.responses.TokenResponse;
import com.sanedge.pointofsale.gateway.dto.responses.UserResponse;
import com.sanedge.pointofsale.gateway.mapper.ProtoMapper;
import com.sanedge.pointofsale.pb.auth.AuthServiceGrpc;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthServiceGrpc.AuthServiceBlockingStub authStub;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody RegisterRequest req) {
        var resp = authStub.registerUser(com.sanedge.pointofsale.pb.auth.RegisterRequest.newBuilder()
                .setFirstname(nvl(req.getFirstname()))
                .setLastname(nvl(req.getLastname()))
                .setUsername(nvl(req.getUsername()))
                .setEmail(nvl(req.getEmail()))
                .setPassword(nvl(req.getPassword()))
                .setConfirmPassword(nvl(req.getConfirmPassword()))
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<UserResponse>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.user(resp.getData()))
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@RequestBody AuthRequest req) {
        var resp = authStub.loginUser(com.sanedge.pointofsale.pb.auth.LoginRequest.newBuilder()
                .setUsername(nvl(req.getUsername()))
                .setPassword(nvl(req.getPassword()))
                .build());
        return ResponseEntity.ok(ApiResponse.<TokenResponse>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.token(resp.getData()))
                .build());
    }

    @GetMapping("/healthchecker")
    public ResponseEntity<String> healthChecker() {
        return ResponseEntity.ok("✅ API is up and running!");
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var resp = authStub.getMe(com.sanedge.pointofsale.pb.auth.GetMeRequest.newBuilder()
                .setUsername(username)
                .build());
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.user(resp.getData()))
                .build());
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@RequestBody RefreshTokenRequest req) {
        var resp = authStub.refreshToken(com.sanedge.pointofsale.pb.auth.RefreshTokenRequest.newBuilder()
                .setRefreshToken(nvl(req.getRefreshToken()))
                .build());
        return ResponseEntity.ok(ApiResponse.<TokenResponse>builder()
                .status(resp.getStatus())
                .message(resp.getMessage())
                .data(ProtoMapper.token(resp.getData()))
                .build());
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }
}
