package com.beercompetition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beercompetition.pojo.po.BankTransferPayment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface BankTransferPaymentMapper extends BaseMapper<BankTransferPayment> {

    @Select("""
            <script>
            SELECT btp.*
              FROM bank_transfer_payment btp
              JOIN competition c ON c.id = btp.competition_id
             WHERE (#{status} IS NULL OR btp.status = #{status})
               AND (#{competitionId} IS NULL OR btp.competition_id = #{competitionId})
               AND (#{organizerId} IS NULL OR c.organizer_id = #{organizerId})
             ORDER BY btp.submitted_time DESC, btp.id DESC
            </script>
            """)
    List<BankTransferPayment> selectAdminTransfers(@Param("status") String status,
                                                   @Param("competitionId") Long competitionId,
                                                   @Param("organizerId") Long organizerId);
}
