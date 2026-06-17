package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminUserVO {

    private Long id;
    private String username;
    private String name;
    private Integer status;
    private String statusLabel;
    private Boolean currentUser;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
