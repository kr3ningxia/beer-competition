package com.beercompetition.pojo.dto;

import com.beercompetition.pojo.enums.OrganizerApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 平台管理员处理入驻申请的请求。
 */
@Data
public class OrganizerApplicationReviewRequest {

    @NotNull(message = "请选择处理结果")
    private OrganizerApplicationStatus status;

    @Size(max = 1000, message = "审核备注不能超过1000个字符")
    private String remark;
}
