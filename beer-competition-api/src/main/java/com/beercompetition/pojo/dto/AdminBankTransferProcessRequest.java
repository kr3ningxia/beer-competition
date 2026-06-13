package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminBankTransferProcessRequest {

    @Size(max = 255, message = "处理说明最多 255 个字")
    private String adminNote;
}
