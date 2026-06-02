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
@TableName("style_library")
public class StyleLibrary {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String version;
    private String language;
    private String source;
    private Integer status;
    private String tagsJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
