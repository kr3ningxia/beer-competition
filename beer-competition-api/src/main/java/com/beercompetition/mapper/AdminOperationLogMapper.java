package com.beercompetition.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.vo.AdminOperationLogVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Collection;

public interface AdminOperationLogMapper extends BaseMapper<AdminOperationLog> {

    @Select("""
            <script>
            SELECT
              l.id,
              l.create_time,
              l.admin_user_id,
              u.username AS admin_username,
              u.name AS admin_name,
              l.action,
              l.target_type,
              l.target_public_id,
              l.summary
            FROM admin_operation_log l
            LEFT JOIN admin_user u ON u.id = l.admin_user_id
            WHERE 1 = 1
            <if test="startTime != null">
              AND l.create_time &gt;= #{startTime}
            </if>
            <if test="endTime != null">
              AND l.create_time &lt;= #{endTime}
            </if>
            <if test="adminUserId != null">
              AND l.admin_user_id = #{adminUserId}
            </if>
            <if test="targetType != null and targetType != ''">
              AND l.target_type = #{targetType}
            </if>
            <if test="actions != null and actions.size() &gt; 0">
              AND l.action IN
              <foreach collection="actions" item="action" open="(" separator="," close=")">
                #{action}
              </foreach>
            </if>
            <if test="keyword != null and keyword != ''">
              AND (
                l.action LIKE CONCAT('%', #{keyword}, '%')
                OR l.target_public_id LIKE CONCAT('%', #{keyword}, '%')
                OR l.summary LIKE CONCAT('%', #{keyword}, '%')
                OR u.username LIKE CONCAT('%', #{keyword}, '%')
                OR u.name LIKE CONCAT('%', #{keyword}, '%')
              )
            </if>
            ORDER BY l.id DESC
            </script>
            """)
    Page<AdminOperationLogVO> selectAdminOperationLogPage(Page<AdminOperationLogVO> page,
                                                          @Param("startTime") LocalDateTime startTime,
                                                          @Param("endTime") LocalDateTime endTime,
                                                          @Param("adminUserId") Long adminUserId,
                                                          @Param("targetType") String targetType,
                                                          @Param("actions") Collection<String> actions,
                                                          @Param("keyword") String keyword);
}
