package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CompetitionReopenRegistrationRequest {

    private LocalDateTime registrationDeadline;

    @NotBlank(message = "请填写重新开放原因")
    @Size(max = 120, message = "重新开放原因不能超过120个字符")
    private String reason;
}
