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
@TableName("competition_notification_rule")
public class CompetitionNotificationRule {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long competitionId;
    private String eventCode;
    private Integer enabled;
    private String scheduleMode;
    private LocalDateTime scheduledAt;
    private Integer offsetMinutes;
    private String sendTime;
    private String timezone;
    private Long templateId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
