package com.beercompetition.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminFeedbackCommentUpdateRequest {

    @NotBlank(message = "评价内容不能为空")
    @Size(max = 1000, message = "评价内容不能超过 1000 字")
    private String comments;

    @Valid
    private List<AdminFeedbackDimensionNoteRequest> dimensionNotes;

    @NotBlank(message = "请填写修改原因")
    @Size(max = 120, message = "修改原因不能超过 120 字")
    private String reason;

    private LocalDateTime expectedUpdatedAt;
}
