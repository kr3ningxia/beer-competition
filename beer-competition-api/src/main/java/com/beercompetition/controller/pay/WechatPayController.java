package com.beercompetition.controller.pay;

import com.beercompetition.common.result.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pay/wechat")
public class WechatPayController {

    @PostMapping("/refund-notify")
    public Result<String> refundNotify(@RequestBody(required = false) String body) {
        return Result.success("退款回调待接入");
    }
}
