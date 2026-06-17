package com.beercompetition.testsupport;

import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.context.SessionUser;
import com.beercompetition.pojo.enums.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

@SpringBootTest
@ActiveProfiles("local")
public abstract class IntegrationTestBase {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected final String testRun = "IT" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

    @AfterEach
    void cleanIntegrationData() {
        BaseContext.clear();
        cleanupByPrefix(testRun);
    }

    protected void asAdmin(Long adminId) {
        setUser(adminId, UserRole.ADMIN, "测试管理员");
    }

    protected void asPortal(Long portalId) {
        setUser(portalId, UserRole.PORTAL, "测试厂商");
    }

    protected void asJudge(Long judgeId) {
        setUser(judgeId, UserRole.JUDGE, "测试评委");
    }

    protected void clearUser() {
        BaseContext.clear();
    }

    private void setUser(Long userId, UserRole role, String displayName) {
        BaseContext.setCurrentUser(SessionUser.builder()
                .userId(userId)
                .role(role.name())
                .displayName(displayName)
                .build());
    }

    protected void cleanupByPrefix(String prefix) {
        jdbcTemplate.update("""
                DELETE btp FROM bank_transfer_payment btp
                JOIN competition c ON c.id = btp.competition_id
                WHERE c.code LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE fa FROM file_asset fa
                JOIN portal_account pa ON pa.id = fa.owner_id
                WHERE fa.business_type = 'BANK_TRANSFER_VOUCHER'
                  AND fa.owner_type = 'PORTAL_ACCOUNT'
                  AND pa.display_name LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE wpn FROM wechat_pay_notify wpn
                JOIN entry_payment ep ON ep.out_trade_no = wpn.out_trade_no
                JOIN beer_entry be ON be.id = ep.beer_entry_id
                WHERE be.uuid LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE rtc FROM round_table_confirmation rtc
                JOIN round_table rt ON rt.id = rtc.round_table_id
                JOIN competition c ON c.id = rt.competition_id
                WHERE c.code LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE rjrd FROM round_judge_ranking_draft rjrd
                JOIN round_table rt ON rt.id = rjrd.round_table_id
                JOIN competition c ON c.id = rt.competition_id
                WHERE c.code LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE rr FROM round_result rr
                JOIN competition c ON c.id = rr.competition_id
                WHERE c.code LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE jss FROM judge_score_session jss
                JOIN competition c ON c.id = jss.competition_id
                WHERE c.code LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE sr FROM score_record sr
                JOIN competition c ON c.id = sr.competition_id
                WHERE c.code LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE ar FROM award_result ar
                JOIN competition c ON c.id = ar.competition_id
                WHERE c.code LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE art FROM award_rule art
                JOIN competition c ON c.id = art.competition_id
                WHERE c.code LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE rtm FROM round_table_member rtm
                JOIN round_table rt ON rt.id = rtm.round_table_id
                JOIN competition c ON c.id = rt.competition_id
                WHERE c.code LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE rte FROM round_table_entry rte
                JOIN competition c ON c.id = rte.competition_id
                WHERE c.code LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE rt FROM round_table rt
                JOIN competition c ON c.id = rt.competition_id
                WHERE c.code LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE cr FROM competition_round cr
                JOIN competition c ON c.id = cr.competition_id
                WHERE c.code LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE esl FROM entry_scan_label esl
                JOIN beer_entry be ON be.id = esl.beer_entry_id
                WHERE be.uuid LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE er FROM entry_refund er
                JOIN beer_entry be ON be.id = er.beer_entry_id
                WHERE be.uuid LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE ep FROM entry_payment ep
                JOIN beer_entry be ON be.id = ep.beer_entry_id
                WHERE be.uuid LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE ed FROM entry_delivery ed
                JOIN beer_entry be ON be.id = ed.beer_entry_id
                WHERE be.uuid LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE beef FROM beer_entry_extra_field beef
                JOIN beer_entry be ON be.id = beef.beer_entry_id
                WHERE be.uuid LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("DELETE FROM beer_entry WHERE uuid LIKE ?", prefix + "%");
        jdbcTemplate.update("""
                DELETE jaa FROM competition_judge_assignment jaa
                JOIN competition c ON c.id = jaa.competition_id
                WHERE c.code LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE jt FROM competition_judge_table jt
                JOIN competition c ON c.id = jt.competition_id
                WHERE c.code LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE csc FROM competition_score_config csc
                JOIN competition c ON c.id = csc.competition_id
                WHERE c.code LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE aol FROM admin_operation_log aol
                JOIN competition c ON aol.target_public_id = CAST(c.id AS CHAR)
                WHERE aol.target_type = 'COMPETITION'
                  AND c.code LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE cs FROM competition_style_config cs
                JOIN competition c ON c.id = cs.competition_id
                WHERE c.code LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE cc FROM competition_category cc
                JOIN competition c ON c.id = cc.competition_id
                WHERE c.code LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("""
                DELETE efc FROM entry_field_config efc
                JOIN competition c ON c.id = efc.competition_id
                WHERE c.code LIKE ?
                """, prefix + "%");
        jdbcTemplate.update("DELETE FROM competition WHERE code LIKE ?", prefix + "%");
        jdbcTemplate.update("DELETE FROM portal_account WHERE display_name LIKE ?", prefix + "%");
        jdbcTemplate.update("DELETE FROM brewery WHERE company_name LIKE ?", prefix + "%");
        jdbcTemplate.update("DELETE FROM judge_account WHERE public_id LIKE ?", prefix + "%");
    }
}
