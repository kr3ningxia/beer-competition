package com.beercompetition.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class NotificationRulesUpdateRequest {

    @NotEmpty(message = "至少配置一条通知规则")
    private List<@Valid NotificationRuleUpdateRequest> rules;
}
