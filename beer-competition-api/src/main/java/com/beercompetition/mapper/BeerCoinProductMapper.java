package com.beercompetition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beercompetition.pojo.po.BeerCoinProduct;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface BeerCoinProductMapper extends BaseMapper<BeerCoinProduct> {

    @Select("SELECT * FROM beer_coin_product WHERE status = 'ACTIVE' ORDER BY effective_time DESC, id DESC LIMIT 1")
    BeerCoinProduct selectActive();

    @Select("SELECT * FROM beer_coin_product WHERE status = 'ACTIVE' ORDER BY effective_time DESC, id DESC LIMIT 1 FOR UPDATE")
    BeerCoinProduct selectActiveForUpdate();

    @Select("SELECT * FROM beer_coin_product WHERE product_code = #{productCode} LIMIT 1 FOR UPDATE")
    BeerCoinProduct selectByCodeForUpdate(@Param("productCode") String productCode);
}
