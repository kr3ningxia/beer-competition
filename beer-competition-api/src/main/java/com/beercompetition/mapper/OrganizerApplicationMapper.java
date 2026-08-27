package com.beercompetition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beercompetition.pojo.po.OrganizerApplication;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface OrganizerApplicationMapper extends BaseMapper<OrganizerApplication> {

    @Select("SELECT * FROM organizer_application WHERE id = #{id} FOR UPDATE")
    OrganizerApplication selectByIdForUpdate(@Param("id") Long id);
}
