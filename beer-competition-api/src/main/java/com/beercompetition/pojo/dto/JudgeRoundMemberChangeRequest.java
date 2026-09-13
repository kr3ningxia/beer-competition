package com.beercompetition.pojo.dto;

import com.beercompetition.pojo.enums.JudgeRoleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/** 当前轮次评委离场、回场及桌长替换请求。 */
@Data
public class JudgeRoundMemberChangeRequest {

    @Valid
    private List<JudgeRoundMemberItemRequest> add;

    private List<String> removeJudgePublicIds;

    private String captainJudgePublicId;

    private String reason;

    @Data
    public static class JudgeRoundMemberItemRequest {
        @NotBlank(message = "评审不能为空")
        private String judgePublicId;

        private JudgeRoleType role;
    }
}
