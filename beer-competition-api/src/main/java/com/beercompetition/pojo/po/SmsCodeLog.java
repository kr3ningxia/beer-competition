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
@TableName("sms_code_log")
public class SmsCodeLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String phoneHash;
    private String maskedPhone;
    private String bizType;
    private String status;
    private LocalDateTime createTime;
}
