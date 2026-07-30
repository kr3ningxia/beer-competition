package com.beercompetition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.vo.AdminEntryVO;
import com.beercompetition.pojo.vo.CompetitionEntryStatsVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

public interface BeerEntryMapper extends BaseMapper<BeerEntry> {

    @Select("""
            <script>
            SELECT e.id,
                   e.uuid,
                   l.label_code,
                   l.short_code,
                   l.scan_token,
                   e.competition_id,
                   c.name AS competition_name,
                   c.code AS competition_code,
                   e.name,
                   b.company_name AS brewery_company_name,
                   b.contact_name AS brewery_contact_name,
                   e.category_id,
                   cc.name AS category_name,
                   e.style,
                   e.abv,
                   e.status,
                   CASE
                     WHEN ep.status IS NOT NULL AND ep.status != '' THEN ep.status
                     WHEN e.status IN ('REGISTERED', 'STORED', 'RESULT_PUBLISHED') THEN 'PAID'
                     ELSE 'UNPAID'
                   END AS payment_status,
                   er.status AS refund_status,
                   er.reason AS refund_reason,
                   er.requested_time AS refund_requested_at,
                   er.processed_time AS refund_processed_at,
                   COALESCE(ed.delivery_status, 'NOT_SUBMITTED') AS delivery_status,
                   ed.carrier,
                   ed.tracking_no,
                   CASE WHEN e.stored_flag = 1 THEN TRUE ELSE FALSE END AS `stored`,
                   CASE WHEN EXISTS (
                     SELECT 1 FROM round_table_entry rte WHERE rte.beer_entry_id = e.id
                   ) THEN TRUE ELSE FALSE END AS assigned,
                   e.create_time AS submitted_at,
                   ep.paid_time,
                   ed.received_time AS delivery_received_at,
                   GREATEST(e.update_time,
                            COALESCE(ep.update_time, e.update_time),
                            COALESCE(ed.update_time, e.update_time),
                            COALESCE(er.update_time, e.update_time)) AS last_modified_at,
                   CASE
                     WHEN e.status != 'RESULT_PUBLISHED' AND COALESCE(c.status, '') != 'PUBLISHED'
                     THEN TRUE ELSE FALSE
                   END AS can_edit,
                   CASE
                     WHEN er.status = 'APPROVED' AND ep.pay_method IN ('BANK_TRANSFER', 'MANUAL')
                     THEN TRUE ELSE FALSE
                   END AS can_confirm_offline_refund
            FROM beer_entry e
            LEFT JOIN competition c ON c.id = e.competition_id
            LEFT JOIN brewery b ON b.id = e.brewery_id
            LEFT JOIN competition_category cc ON cc.id = e.category_id
            LEFT JOIN entry_payment ep ON ep.beer_entry_id = e.id
            LEFT JOIN entry_delivery ed ON ed.beer_entry_id = e.id
            LEFT JOIN entry_refund er ON er.id = (
              SELECT MAX(latest_refund.id)
              FROM entry_refund latest_refund
              WHERE latest_refund.beer_entry_id = e.id
            )
            LEFT JOIN entry_scan_label l ON l.id = (
              SELECT MAX(active_label.id)
              FROM entry_scan_label active_label
              WHERE active_label.beer_entry_id = e.id AND active_label.status = 'ACTIVE'
            )
            <where>
              e.deleted_flag = 0
              AND e.status != 'CANCELED'
              <if test="competitionId != null">AND e.competition_id = #{competitionId}</if>
              <if test="status != null and status != ''">AND e.status = #{status}</if>
              <if test="categoryId != null">AND e.category_id = #{categoryId}</if>
              <if test="paymentStatus != null and paymentStatus != ''">
                AND (CASE
                  WHEN ep.status IS NOT NULL AND ep.status != '' THEN ep.status
                  WHEN e.status IN ('REGISTERED', 'STORED', 'RESULT_PUBLISHED') THEN 'PAID'
                  ELSE 'UNPAID'
                END) = #{paymentStatus}
              </if>
              <if test="deliveryStatus != null and deliveryStatus != ''">
                AND COALESCE(ed.delivery_status, 'NOT_SUBMITTED') = #{deliveryStatus}
              </if>
              <if test="refundStatus != null and refundStatus != ''">AND er.status = #{refundStatus}</if>
              <if test="assigned != null">
                AND EXISTS (
                  SELECT 1 FROM round_table_entry rte_filter
                  WHERE rte_filter.beer_entry_id = e.id
                ) = #{assigned}
              </if>
              <if test="keyword != null and keyword != ''">
                AND (
                  e.name LIKE CONCAT('%', #{keyword}, '%')
                  OR b.company_name LIKE CONCAT('%', #{keyword}, '%')
                  OR c.name LIKE CONCAT('%', #{keyword}, '%')
                  OR e.uuid LIKE CONCAT('%', #{keyword}, '%')
                  OR l.label_code LIKE CONCAT('%', #{keyword}, '%')
                  OR l.short_code LIKE CONCAT('%', #{keyword}, '%')
                  OR cc.name LIKE CONCAT('%', #{keyword}, '%')
                  OR e.style LIKE CONCAT('%', #{keyword}, '%')
                  OR ed.carrier LIKE CONCAT('%', #{keyword}, '%')
                  OR ed.tracking_no LIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
            </where>
            ORDER BY CASE
                       WHEN er.status IN ('REQUESTED', 'FAILED')
                         OR (er.status = 'APPROVED' AND ep.pay_method IN ('BANK_TRANSFER', 'MANUAL')) THEN 0
                       WHEN er.status = 'SUCCESS'
                         OR (CASE
                           WHEN ep.status IS NOT NULL AND ep.status != '' THEN ep.status
                           WHEN e.status IN ('REGISTERED', 'STORED', 'RESULT_PUBLISHED') THEN 'PAID'
                           ELSE 'UNPAID'
                         END) = 'REFUNDED' THEN 2
                       ELSE 1
                     END,
                     last_modified_at DESC,
                     e.id DESC
            LIMIT #{offset}, #{limit}
            </script>
            """)
    List<AdminEntryVO> selectAdminEntryPage(
            @Param("competitionId") Long competitionId,
            @Param("status") String status,
            @Param("paymentStatus") String paymentStatus,
            @Param("deliveryStatus") String deliveryStatus,
            @Param("categoryId") Long categoryId,
            @Param("assigned") Boolean assigned,
            @Param("refundStatus") String refundStatus,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM beer_entry e
            LEFT JOIN competition c ON c.id = e.competition_id
            LEFT JOIN brewery b ON b.id = e.brewery_id
            LEFT JOIN competition_category cc ON cc.id = e.category_id
            LEFT JOIN entry_payment ep ON ep.beer_entry_id = e.id
            LEFT JOIN entry_delivery ed ON ed.beer_entry_id = e.id
            LEFT JOIN entry_refund er ON er.id = (
              SELECT MAX(latest_refund.id)
              FROM entry_refund latest_refund
              WHERE latest_refund.beer_entry_id = e.id
            )
            LEFT JOIN entry_scan_label l ON l.id = (
              SELECT MAX(active_label.id)
              FROM entry_scan_label active_label
              WHERE active_label.beer_entry_id = e.id AND active_label.status = 'ACTIVE'
            )
            <where>
              e.deleted_flag = 0
              AND e.status != 'CANCELED'
              <if test="competitionId != null">AND e.competition_id = #{competitionId}</if>
              <if test="status != null and status != ''">AND e.status = #{status}</if>
              <if test="categoryId != null">AND e.category_id = #{categoryId}</if>
              <if test="paymentStatus != null and paymentStatus != ''">
                AND (CASE
                  WHEN ep.status IS NOT NULL AND ep.status != '' THEN ep.status
                  WHEN e.status IN ('REGISTERED', 'STORED', 'RESULT_PUBLISHED') THEN 'PAID'
                  ELSE 'UNPAID'
                END) = #{paymentStatus}
              </if>
              <if test="deliveryStatus != null and deliveryStatus != ''">
                AND COALESCE(ed.delivery_status, 'NOT_SUBMITTED') = #{deliveryStatus}
              </if>
              <if test="refundStatus != null and refundStatus != ''">AND er.status = #{refundStatus}</if>
              <if test="assigned != null">
                AND EXISTS (
                  SELECT 1 FROM round_table_entry rte_filter
                  WHERE rte_filter.beer_entry_id = e.id
                ) = #{assigned}
              </if>
              <if test="keyword != null and keyword != ''">
                AND (
                  e.name LIKE CONCAT('%', #{keyword}, '%')
                  OR b.company_name LIKE CONCAT('%', #{keyword}, '%')
                  OR c.name LIKE CONCAT('%', #{keyword}, '%')
                  OR e.uuid LIKE CONCAT('%', #{keyword}, '%')
                  OR l.label_code LIKE CONCAT('%', #{keyword}, '%')
                  OR l.short_code LIKE CONCAT('%', #{keyword}, '%')
                  OR cc.name LIKE CONCAT('%', #{keyword}, '%')
                  OR e.style LIKE CONCAT('%', #{keyword}, '%')
                  OR ed.carrier LIKE CONCAT('%', #{keyword}, '%')
                  OR ed.tracking_no LIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
            </where>
            </script>
            """)
    long countAdminEntries(
            @Param("competitionId") Long competitionId,
            @Param("status") String status,
            @Param("paymentStatus") String paymentStatus,
            @Param("deliveryStatus") String deliveryStatus,
            @Param("categoryId") Long categoryId,
            @Param("assigned") Boolean assigned,
            @Param("refundStatus") String refundStatus,
            @Param("keyword") String keyword);

    @Select("""
            <script>
            SELECT competition_id,
                   COUNT(*) AS total_count,
                   SUM(CASE WHEN status = #{pendingPaymentStatus} THEN 1 ELSE 0 END) AS pending_payment_count,
                   SUM(CASE WHEN status != #{canceledStatus} THEN 1 ELSE 0 END) AS registered_count,
                   SUM(CASE WHEN status != #{canceledStatus} AND stored_flag = #{storedFlag} THEN 1 ELSE 0 END) AS stored_count,
                   SUM(CASE WHEN status = #{canceledStatus} THEN 1 ELSE 0 END) AS canceled_count,
                   SUM(CASE WHEN status IN (#{resultPublishedStatus}, #{legacyPublishedStatus}) THEN 1 ELSE 0 END) AS result_published_count
            FROM beer_entry
            WHERE deleted_flag = 0
              AND competition_id IN
            <foreach collection="competitionIds" item="competitionId" open="(" separator="," close=")">
              #{competitionId}
            </foreach>
            GROUP BY competition_id
            </script>
            """)
    List<CompetitionEntryStatsVO> selectCompetitionStats(
            @Param("competitionIds") Collection<Long> competitionIds,
            @Param("pendingPaymentStatus") String pendingPaymentStatus,
            @Param("canceledStatus") String canceledStatus,
            @Param("resultPublishedStatus") String resultPublishedStatus,
            @Param("legacyPublishedStatus") String legacyPublishedStatus,
            @Param("storedFlag") Integer storedFlag);
}
