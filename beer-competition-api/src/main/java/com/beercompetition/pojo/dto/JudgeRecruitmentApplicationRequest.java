package com.beercompetition.pojo.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data public class JudgeRecruitmentApplicationRequest { @NotNull private Boolean availabilityConfirmed; private String note; }
