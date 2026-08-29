package com.beercompetition.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BeerCoinPurchaseOrderVO {

    private Long id;
    private String orderNo;
    private Long enterpriseAccountId;
    private String enterpriseAccountName;
    private Long productId;
    private String productName;
    private Long quantity;
    private BigDecimal amount;
    private String status;
    private String outTradeNo;
    private String wechatTransactionId;
    private String codeUrl;
    private LocalDateTime expireTime;
    private String wechatTradeState;
    private String wechatTradeStateDesc;
    private LocalDateTime paidTime;
    private LocalDateTime createTime;
    private List<BeerCoinPriceSegmentVO> priceSegments;
}
