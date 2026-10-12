package com.beercompetition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beercompetition.pojo.po.RoundTable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface RoundTableMapper extends BaseMapper<RoundTable> {

    @Select("SELECT * FROM round_table WHERE id = #{id} FOR UPDATE")
    RoundTable selectByIdForUpdate(@Param("id") Long id);
}
