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
@TableName("competition_collection_config")
public class CompetitionCollectionConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long competitionId;
    private Long wechatQrAssetId;
    private String bankAccountName;
    private String bankAccountNo;
    private String bankName;
    private String collectionNote;
    private String paymentContact;
    private String enabledMethodsJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
