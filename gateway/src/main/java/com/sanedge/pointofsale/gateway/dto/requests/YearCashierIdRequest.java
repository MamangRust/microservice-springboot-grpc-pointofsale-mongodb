package com.sanedge.pointofsale.gateway.dto.requests;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class YearCashierIdRequest {
    @NotNull(message = "cashier_id is required")
    private Integer cashierId;

    @NotNull(message = "year is required")
    private Integer year;
}
