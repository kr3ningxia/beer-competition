package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoundTableMemberVO {

    private String judgePublicId;
    private String name;
    private String role;
    private String roleLabel;
    private Boolean systemTaskRequired;
}
