package com.beercompetition.pojo.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;
@Data public class JudgeRecruitmentRequest {
 @NotNull private Long competitionId;
 @NotNull private LocalDateTime recruitmentStart;
 @NotNull private LocalDateTime recruitmentDeadline;
 private LocalTime judgingStartTime;
 @NotBlank private String venue;
 private String address; private String description; private String requirements;
 @Min(1) private Integer expectedCount;
}
