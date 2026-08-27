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
@TableName("organizer_application")
public class OrganizerApplication {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long portalAccountId;
    private String applicationNo;
    private String organizationName;
    private String contactName;
    private String contactPhoneEnc;
    private String contactPhoneHash;
    private String contactPhoneLast4;
    private String contactEmail;
    private String wechatEnc;
    private String businessDescription;
    private String expectedScale;
    private String supplementalNote;
    private Long materialAssetId;
    private String status;
    private Long reviewedByAdminId;
    private LocalDateTime reviewedTime;
    private String reviewRemark;
    private Long organizerId;
    private Long initialAdminUserId;
    private LocalDateTime accountIssuedTime;
    private LocalDateTime submittedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
