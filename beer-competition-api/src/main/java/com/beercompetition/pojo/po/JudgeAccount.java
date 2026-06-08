package com.beercompetition.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("judge_account")
public class JudgeAccount {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String publicId;
    private String phoneEnc;
    private String phoneHash;
    private String phoneLast4;
    private String wechatEnc;
    private String name;
    private String qualification;
    private Boolean breweryConflictFlag;
    private String breweryConflictText;
    private Integer status;
    private LocalDateTime submittedTime;
    private LocalDateTime reviewedTime;
    private Long reviewedBy;
    private String reviewRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
