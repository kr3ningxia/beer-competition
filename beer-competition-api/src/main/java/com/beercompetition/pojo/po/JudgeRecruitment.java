package com.beercompetition.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("judge_recruitment")
public class JudgeRecruitment {
    @TableId(type = IdType.AUTO) private Long id;
    private String publicId;
    private Long competitionId;
    private String status;
    private LocalDateTime recruitmentStart;
    private LocalDateTime recruitmentDeadline;
    private String venue;
    private String address;
    private String description;
    private String requirements;
    private Integer expectedCount;
    private Long createdBy;
    private LocalDateTime closedTime;
    private LocalDateTime reopenedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
