package com.beercompetition.common.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionUser {

    private Long userId;
    private String role;
    private String displayName;
    private Long competitionId;
}
