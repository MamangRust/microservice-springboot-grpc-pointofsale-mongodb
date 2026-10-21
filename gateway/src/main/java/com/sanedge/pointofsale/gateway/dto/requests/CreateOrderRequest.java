package com.sanedge.pointofsale.gateway.dto.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

import lombok.Data;

@Data

public class CreateOrderRequest {
    @NotNull
    private Integer merchantId;

    @NotNull
    private Integer cashierId;

    @Valid
    @NotNull
    private List<CreateOrderItemRequest> items;
}
