package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class StyleLibraryVO {

    private Long id;
    private String value;
    private String label;
    private String code;
    private String name;
    private String version;
    private String language;
    private String source;
    private Integer status;
    private String statusLabel;
    private Integer categoryCount;
    private Integer styleCount;
    private List<String> tags;
    private List<String> categories;
    private List<String> styles;
    private List<StyleItemVO> styleItems;
    private LocalDateTime updatedAt;
}
