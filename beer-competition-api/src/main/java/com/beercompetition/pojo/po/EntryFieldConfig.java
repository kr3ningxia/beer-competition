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
@TableName("entry_field_config")
public class EntryFieldConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long competitionId;
    private String fieldKey;
    private String fieldLabel;
    private String fieldType;
    private String helpText;
    private String optionsJson;
    private Integer requiredFlag;
    private Integer visibleToJudges;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
