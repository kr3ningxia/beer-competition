package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompetitionSponsorItemRequest {

    private Long id;

    @NotBlank(message = "赞助等级不能为空")
    @Size(max = 32, message = "赞助等级不能超过 32 个字符")
    private String tierLabel;

    @NotBlank(message = "赞助商名称不能为空")
    @Size(max = 64, message = "赞助商名称不能超过 64 个字符")
    private String sponsorName;

    private Long logoAssetId;
    private Integer sortOrder;
    private Boolean featured;
    private Boolean enabled;
}
