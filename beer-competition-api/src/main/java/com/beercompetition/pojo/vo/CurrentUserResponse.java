package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CurrentUserResponse {

    private Long userId;
    private String role;
    private String displayName;
    private String phone;
}
