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
@TableName("email_delivery")
public class EmailDelivery {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long competitionId;
    private String eventCode;
    private Long recipientAccountId;
    private String recipientEmailEnc;
    private String recipientEmailHash;
    private Integer resultVersion;
    private String subjectSnapshot;
    private String htmlBodySnapshot;
    private LocalDateTime scheduledTime;
    private String status;
    private Integer attemptCount;
    private LocalDateTime nextRetryTime;
    private String providerRequestId;
    private String lastError;
    private LocalDateTime sentTime;
    private LocalDateTime deliveredTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
