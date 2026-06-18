package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private String token;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private String scope;
    private Long userId;
    private String role;
    private String displayName;
    private Integer status;
    private Boolean profileRequired;
    private Boolean profileComplete;
    private Boolean newAccount;
}
