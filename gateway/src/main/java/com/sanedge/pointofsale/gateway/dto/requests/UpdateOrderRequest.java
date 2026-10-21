package com.sanedge.pointofsale.gateway.dto.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

import lombok.Data;

@Data

public class UpdateOrderRequest {
    @NotNull
    private Integer orderId;

    @NotNull
    private Integer cashierId;

    @Valid
    @NotNull
    private List<UpdateOrderItemRequest> items;
}
