package com.beercompetition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beercompetition.pojo.po.PaymentOrderItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentOrderItemMapper extends BaseMapper<PaymentOrderItem> {
}
