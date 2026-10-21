package com.sanedge.pointofsale.gateway.dto.requests;

import jakarta.validation.constraints.Min;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class FindAllUserRequest {

    @Min(value = 1, message = "Page harus minimal 1")
    private Integer page = 1;

    @Min(value = 1, message = "Page size harus minimal 1")
    private Integer pageSize = 10;

    private String search = "";
}
