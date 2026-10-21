package com.sanedge.pointofsale.gateway.dto.requests;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class YearPriceId {
    @NotNull
    private Integer categoryId;

    @NotNull
    private Integer year;
}
