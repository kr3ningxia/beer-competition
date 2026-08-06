package com.beercompetition.registration.entry;

import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.PortalAccountMapper;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.PortalAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 从当前登录会话解析有效的厂商账号及其厂牌归属。
 *
 * <p>厂商报名用例统一从这里取得可信厂牌 ID，不能使用请求参数指定数据归属。</p>
 */
@Service
@RequiredArgsConstructor
public class PortalAccountAccessService {

    private final PortalAccountMapper portalAccountMapper;
    private final BreweryMapper breweryMapper;

    /**
     * 返回当前登录厂商账号，并确认其关联厂牌仍然存在。
     */
    public PortalAccount requireCurrentAccount() {
        PortalAccount account = portalAccountMapper.selectById(BaseContext.getCurrentId());
        if (account == null) {
            throw new ResourceNotFoundException("厂牌账号不存在");
        }
        Brewery brewery = breweryMapper.selectById(account.getBreweryId());
        if (brewery == null) {
            throw new ResourceNotFoundException("厂牌不存在");
        }
        return account;
    }
}
