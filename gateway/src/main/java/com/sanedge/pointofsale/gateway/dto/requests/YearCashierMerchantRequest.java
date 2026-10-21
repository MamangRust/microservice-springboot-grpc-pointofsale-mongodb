package com.sanedge.pointofsale.gateway.dto.requests;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class YearCashierMerchantRequest {
    @NotNull(message = "merchant_id is required")
    private Integer merchantId;

    @NotNull(message = "year is required")
    private Integer year;
}
