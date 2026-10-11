package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 首轮进行中调整桌内酒款（摘除/加入）请求。 */
@Data
public class RoundEntryChangeRequest {

    @NotBlank(message = "请选择酒款")
    private String entryUuid;

    @Size(max = 255, message = "调整原因不能超过 255 字")
    private String reason;
}
