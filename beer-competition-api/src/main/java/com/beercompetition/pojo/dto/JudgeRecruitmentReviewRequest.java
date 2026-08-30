package com.beercompetition.pojo.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data public class JudgeRecruitmentReviewRequest { @NotBlank private String status; private String reviewRemark; }
