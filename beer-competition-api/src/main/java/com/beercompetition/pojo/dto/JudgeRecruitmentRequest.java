package com.beercompetition.pojo.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class JudgeRecruitmentRequest {
 @NotNull private Long competitionId;
 @NotNull private LocalDateTime recruitmentStart;
 @NotNull private LocalDateTime recruitmentDeadline;
 @NotBlank private String venue;
 private String address; private String description; private String requirements;
 @Min(1) private Integer expectedCount;
}
