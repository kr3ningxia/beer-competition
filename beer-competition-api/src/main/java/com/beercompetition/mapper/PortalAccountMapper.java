package com.beercompetition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beercompetition.pojo.po.PortalAccount;
import com.beercompetition.pojo.vo.NotificationRecipientRow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PortalAccountMapper extends BaseMapper<PortalAccount> {

    @Select("""
            SELECT a.id AS account_id,
                   a.brewery_id,
                   a.email_enc,
                   a.email_hash,
                   a.display_name,
                   b.company_name,
                   b.contact_name,
                   COUNT(e.id) AS entry_count,
                   COALESCE(SUM(CASE WHEN COALESCE(ed.delivery_status, 'NOT_SUBMITTED') = 'NOT_SUBMITTED' THEN 1 ELSE 0 END), 0) AS pending_delivery_count
            FROM portal_account a
            JOIN brewery b ON b.id = a.brewery_id
            JOIN beer_entry e ON e.brewery_id = a.brewery_id
                              AND e.competition_id = #{competitionId}
                              AND e.deleted_flag = 0
                              AND e.status IN ('REGISTERED', 'STORED', 'RESULT_PUBLISHED')
            LEFT JOIN entry_payment ep ON ep.beer_entry_id = e.id
            LEFT JOIN entry_delivery ed ON ed.beer_entry_id = e.id
            WHERE a.status = 1
              AND (ep.id IS NULL OR ep.status IN ('PAID', 'SUCCESS'))
            GROUP BY a.id, a.email_enc, a.email_hash, a.display_name, b.company_name, b.contact_name
            ORDER BY b.company_name, a.id
            """)
    List<NotificationRecipientRow> selectNotificationRecipients(@Param("competitionId") Long competitionId);
}
