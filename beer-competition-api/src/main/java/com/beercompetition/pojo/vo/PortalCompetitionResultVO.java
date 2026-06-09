package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PortalCompetitionResultVO {

    private Long id;
    private String code;
    private String name;
    private String edition;
    private LocalDate matchDate;
    private LocalDateTime publishedAt;
    private List<PortalResultGroupVO> groups;
    private List<PortalAwardEntryVO> entries;
}
