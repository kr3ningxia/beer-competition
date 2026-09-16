package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PortalPublicJudgeVO {

    private Long snapshotId;
    private String name;
    private String avatarUrl;
    private List<String> roles;
    private String qualification;
}
