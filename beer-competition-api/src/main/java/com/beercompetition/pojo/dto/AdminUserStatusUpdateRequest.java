package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminUserStatusUpdateRequest {

    @NotNull(message = "请选择账号状态")
    private Integer status;
}
