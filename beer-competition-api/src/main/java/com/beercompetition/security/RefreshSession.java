package com.beercompetition.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshSession {

    private String sessionId;
    private String tokenHash;
    private Long userId;
    private String role;
    private String scope;
    private String displayName;
    private Long issuedAtMillis;
    private Long expiresAtMillis;
}
