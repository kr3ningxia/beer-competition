package com.beercompetition.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("judge_recruitment_application")
public class JudgeRecruitmentApplication {
    @TableId(type = IdType.AUTO) private Long id;
    private String publicId;
    private Long recruitmentId;
    private Long judgeAccountId;
    private String status;
    private Boolean availabilityConfirmed;
    private String note;
    private String reviewRemark;
    private Long processedBy;
    private LocalDateTime processedTime;
    private LocalDateTime withdrawnTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
