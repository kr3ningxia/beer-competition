package com.beercompetition.registration.refund;

import com.beercompetition.common.result.PageResult;
import com.beercompetition.pojo.dto.AdminEntryStatusRequest;
import com.beercompetition.pojo.dto.AdminOfflineRefundRequest;
import com.beercompetition.pojo.dto.PortalEntryRefundRequest;
import com.beercompetition.pojo.vo.AdminEntryVO;
import com.beercompetition.pojo.vo.EntryDetailVO;
import com.beercompetition.pojo.vo.FileDownloadVO;
import com.beercompetition.pojo.vo.RefundPreviewVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 编排厂商退款申请和后台退款处理，保留原支付渠道的恢复与重试语义。
 */
public interface EntryRefundService {

    EntryDetailVO requestPortalEntryRefund(Long entryId, PortalEntryRefundRequest request);

    RefundPreviewVO previewPortalEntryRefund(Long entryId);

    PageResult<AdminEntryVO> listAdminRefunds(String status, Integer page, Integer pageSize);

    void approveRefund(Long refundId, AdminEntryStatusRequest request);

    void rejectRefund(Long refundId, AdminEntryStatusRequest request);

    void completeOfflineRefund(Long refundId, AdminEntryStatusRequest request);

    void registerOfflineRefund(Long refundId, AdminOfflineRefundRequest request, MultipartFile voucher);

    void retryRefund(Long refundId, AdminEntryStatusRequest request);

    FileDownloadVO downloadOfflineVoucher(Long refundId);
}
