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
@TableName("entry_scan_label")
public class EntryScanLabel {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long competitionId;
    private Long beerEntryId;
    private String labelCode;
    private String shortCode;
    private String scanToken;
    private String status;
    private Long generatedBy;
    private LocalDateTime generatedTime;
    private LocalDateTime printedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
