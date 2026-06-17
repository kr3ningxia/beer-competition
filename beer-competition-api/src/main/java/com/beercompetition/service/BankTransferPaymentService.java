package com.beercompetition.service;

import com.beercompetition.common.result.PageResult;
import com.beercompetition.pojo.dto.AdminBankTransferProcessRequest;
import com.beercompetition.pojo.dto.PortalBankTransferSubmitRequest;
import com.beercompetition.pojo.vo.BankTransferAccountVO;
import com.beercompetition.pojo.vo.BankTransferVO;
import com.beercompetition.pojo.vo.BankTransferVoucherVO;
import com.beercompetition.pojo.vo.FileDownloadVO;
import org.springframework.web.multipart.MultipartFile;

public interface BankTransferPaymentService {

    BankTransferAccountVO getAccount();

    BankTransferVoucherVO uploadVoucher(MultipartFile file);

    BankTransferVO submitPortalTransfer(PortalBankTransferSubmitRequest request);

    BankTransferVO getPortalTransfer(Long id);

    BankTransferVO updatePortalTransfer(Long id, PortalBankTransferSubmitRequest request);

    BankTransferVO cancelPortalTransfer(Long id);

    PageResult<BankTransferVO> listAdminTransfers(String status, Long competitionId, String keyword,
                                                  Integer page, Integer pageSize);

    BankTransferVO getAdminTransfer(Long id);

    BankTransferVO confirmTransfer(Long id, AdminBankTransferProcessRequest request);

    BankTransferVO rejectTransfer(Long id, AdminBankTransferProcessRequest request);

    FileDownloadVO downloadVoucher(Long id);
}
