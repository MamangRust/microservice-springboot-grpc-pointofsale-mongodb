package com.sanedge.pointofsale.gateway.dto.requests;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data

public class CreateRoleRequest {

    @NotBlank(message = "Nama role wajib diisi")
    private String name;
}
