package com.sanedge.pointofsale.gateway.dto.requests;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class YearTotalSalesCashier {
    @NotNull
    private Integer cashierId;

    @NotNull
    private Integer year;
}
