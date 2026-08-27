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
    private String adminType;
    private Long organizerId;
    private Integer status;
    private Boolean mustChangePassword;
    private Boolean mustChangeUsername;
    private String statusLabel;
    private Boolean currentUser;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
