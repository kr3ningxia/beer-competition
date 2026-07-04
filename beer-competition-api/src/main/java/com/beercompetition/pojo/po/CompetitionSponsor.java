package com.beercompetition.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
@TableName("competition_sponsor")
public class CompetitionSponsor {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long competitionId;
    private String tierLabel;
    private String sponsorName;
    @TableField("logo_asset_id")
    private Long logoAssetId;
    private Integer sortOrder;
    private Integer featuredFlag;
    private Integer enabledFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
