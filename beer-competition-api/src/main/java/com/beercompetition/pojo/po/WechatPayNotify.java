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
@TableName("wechat_pay_notify")
public class WechatPayNotify {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String notifyId;
    private String eventType;
    private String businessType;
    private String outTradeNo;
    private String outRefundNo;
    private String wechatTransactionId;
    private String wechatRefundId;
    private String rawJson;
    private Integer processedFlag;
    private String processMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
