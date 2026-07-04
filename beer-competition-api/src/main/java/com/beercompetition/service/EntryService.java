package com.beercompetition.service;

import com.beercompetition.common.result.PageResult;
import com.beercompetition.pojo.dto.AdminEntryStatusRequest;
import com.beercompetition.pojo.dto.AdminEntryUpdateRequest;
import com.beercompetition.pojo.dto.PortalEntrySubmitRequest;
import com.beercompetition.pojo.dto.PortalEntryDeliverySubmitRequest;
import com.beercompetition.pojo.dto.PortalEntryRefundRequest;
import com.beercompetition.pojo.dto.PortalEntryUpdateRequest;
import com.beercompetition.pojo.dto.PortalProfileUpdateRequest;
import com.beercompetition.pojo.vo.AdminEntryDetailVO;
import com.beercompetition.pojo.vo.AdminEntryVO;
import com.beercompetition.pojo.vo.EntryDetailVO;
import com.beercompetition.pojo.vo.EntrySummaryVO;
import com.beercompetition.pojo.vo.FileDownloadVO;
import com.beercompetition.pojo.vo.JudgeEntryVO;
import com.beercompetition.pojo.vo.PortalEntryLabelVO;
import com.beercompetition.pojo.vo.PortalCompetitionResultVO;
import com.beercompetition.pojo.vo.PortalMyParticipationVO;
import com.beercompetition.pojo.vo.PortalProfileVO;
import com.beercompetition.pojo.vo.PortalResultDetailVO;
import com.beercompetition.pojo.vo.PortalResultSummaryVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EntryService {

    PageResult<AdminEntryVO> listAdminEntries(Long competitionId, String status, String paymentStatus,
                                              String deliveryStatus, Long categoryId, Boolean assigned,
                                              String refundStatus, String keyword, Integer page, Integer pageSize);

    AdminEntryDetailVO getAdminEntry(Long entryId);

    AdminEntryDetailVO updateAdminEntry(Long entryId, AdminEntryUpdateRequest request);

    List<EntrySummaryVO> listPortalEntries();

    EntryDetailVO getPortalEntry(Long entryId);

    EntryDetailVO submitPortalEntry(Long competitionId, PortalEntrySubmitRequest request);

    EntryDetailVO updatePortalEntry(Long entryId, PortalEntryUpdateRequest request);

    EntryDetailVO submitPortalEntryDelivery(Long entryId, PortalEntryDeliverySubmitRequest request);

    EntryDetailVO cancelPortalEntry(Long entryId);

    EntryDetailVO requestPortalEntryRefund(Long entryId, PortalEntryRefundRequest request);

    PortalEntryLabelVO getPortalEntryLabel(Long entryId);

    FileDownloadVO downloadPortalEntryLabelPdf(Long entryId);

    PortalProfileVO getPortalProfile();

    PortalProfileVO updatePortalProfile(PortalProfileUpdateRequest request);

    PortalProfileVO uploadPortalAvatar(MultipartFile file);

    PortalMyParticipationVO getPortalMyParticipation();

    List<PortalCompetitionResultVO> listPublishedCompetitionResults();

    PortalCompetitionResultVO getPublishedCompetitionResult(Long competitionId);

    List<PortalResultSummaryVO> listPortalResults();

    PortalResultDetailVO getPortalResultDetail(Long entryId);

    FileDownloadVO downloadPortalResultCertificate(Long entryId);

    EntryDetailVO simulatePayment(Long entryId);

    void confirmPayment(Long entryId);

    void confirmPayment(Long entryId, AdminEntryStatusRequest request);

    void markStored(Long entryId);

    void markStored(Long entryId, AdminEntryStatusRequest request);

    void unmarkStored(Long entryId, AdminEntryStatusRequest request);

    void cancelEntry(Long entryId);

    void cancelEntry(Long entryId, AdminEntryStatusRequest request);

    PageResult<AdminEntryVO> listAdminRefunds(String status, Integer page, Integer pageSize);

    void approveRefund(Long refundId, AdminEntryStatusRequest request);

    void rejectRefund(Long refundId, AdminEntryStatusRequest request);

    void retryRefund(Long refundId, AdminEntryStatusRequest request);

    JudgeEntryVO getJudgeEntry(String uuid);

    JudgeEntryVO resolveJudgeScan(String code);
}
