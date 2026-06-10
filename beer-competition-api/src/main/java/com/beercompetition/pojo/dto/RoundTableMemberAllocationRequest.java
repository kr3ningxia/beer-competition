package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoundTableMemberAllocationRequest {

    @NotBlank(message = "评审不能为空")
    private String judgePublicId;

    private String role;
}
