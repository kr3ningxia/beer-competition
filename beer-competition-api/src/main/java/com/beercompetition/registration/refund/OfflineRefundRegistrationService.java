package com.beercompetition.registration.refund;

import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.mapper.EntryRefundMapper;
import com.beercompetition.mapper.FileAssetMapper;
import com.beercompetition.pojo.dto.AdminOfflineRefundRequest;
import com.beercompetition.pojo.enums.EntryPayMethod;
import com.beercompetition.pojo.enums.EntryRefundStatus;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.EntryRefund;
import com.beercompetition.pojo.po.FileAsset;
import com.beercompetition.properties.StorageProperties;
import com.beercompetition.service.WechatPaymentService;
import com.beercompetition.storage.FileStorageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 登记银行卡线下退款凭证，并在同一事务内推进退款状态。
 *
 * <p>文件上传完成后若数据库写入失败，当前存储实现不会自动删除对象；文件治理
 * 模块接入时应通过待确认资产或补偿任务处理这类孤立文件。</p>
 */
@Service
@RequiredArgsConstructor
public class OfflineRefundRegistrationService {

    private static final String TARGET_ENTRY = "BEER_ENTRY";
    private static final String BUSINESS_TYPE = "OFFLINE_REFUND_VOUCHER";

    private final EntryRefundMapper entryRefundMapper;
    private final EntryPaymentMapper entryPaymentMapper;
    private final BeerEntryMapper beerEntryMapper;
    private final FileAssetMapper fileAssetMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final WechatPaymentService wechatPaymentService;
    private final FileStorageService fileStorageService;
    private final StorageProperties storageProperties;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public void register(Long refundId, AdminOfflineRefundRequest request, MultipartFile voucher) {
        EntryRefund refund = requireRefund(refundId);
        if (!EntryRefundStatus.APPROVED.name().equals(refund.getStatus())) {
            throw new BaseException("只有待银行卡退款的记录可以登记转账");
        }
        EntryPayment payment = entryPaymentMapper.selectById(refund.getEntryPaymentId());
        if (!isManualRefundPayment(payment)) {
            throw new BaseException("当前退款不是银行卡退款");
        }
        if (voucher == null || voucher.isEmpty()) {
            throw new BaseException("请上传打款凭证");
        }

        String filename = sanitizeUploadFilename(voucher.getOriginalFilename(), "offline-refund-voucher.pdf");
        byte[] bytes = readUploadBytes(voucher, "读取打款凭证失败");
        String storagePath = fileStorageService.upload(BUSINESS_TYPE, filename, bytes);
        FileAsset asset = FileAsset.builder()
                .businessType(BUSINESS_TYPE)
                .ownerType("ADMIN")
                .ownerId(BaseContext.getCurrentId())
                .storageProvider(storageProperties.getProvider())
                .fileName(filename)
                .storagePath(storagePath)
                .publicUrl(resolveUploadPublicUrl(storagePath))
                .createTime(LocalDateTime.now())
                .build();
        fileAssetMapper.insert(asset);

        refund.setStatus(EntryRefundStatus.PROCESSING.name());
        refund.setOfflineRefundVoucherAssetId(asset.getId());
        refund.setProcessedByAdminId(BaseContext.getCurrentId());
        refund.setProcessedTime(LocalDateTime.now());
        refund.setFailReason(null);
        entryRefundMapper.updateById(refund);
        writeEntryLog(requireEntry(refund.getBeerEntryId()).getUuid(), request.getReason());
        wechatPaymentService.completeOfflineRefund(refundId, request.getReason(), BaseContext.getCurrentId());
    }

    private EntryRefund requireRefund(Long refundId) {
        EntryRefund refund = entryRefundMapper.selectById(refundId);
        if (refund == null) {
            throw new ResourceNotFoundException("退款记录不存在");
        }
        return refund;
    }

    private BeerEntry requireEntry(Long entryId) {
        BeerEntry entry = beerEntryMapper.selectById(entryId);
        if (entry == null) {
            throw new ResourceNotFoundException("酒款不存在");
        }
        return entry;
    }

    private boolean isManualRefundPayment(EntryPayment payment) {
        return payment != null && (EntryPayMethod.BANK_TRANSFER.name().equals(payment.getPayMethod())
                || EntryPayMethod.MANUAL.name().equals(payment.getPayMethod()));
    }

    private void writeEntryLog(String entryUuid, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", "登记银行卡退款");
        payload.put("reason", StringUtils.hasText(reason) ? reason : "");
        adminOperationLogMapper.insert(AdminOperationLog.builder()
                .adminUserId(BaseContext.getCurrentId())
                .action("ENTRY_REFUND_REGISTER_OFFLINE")
                .targetType(TARGET_ENTRY)
                .targetPublicId(entryUuid)
                .summary(writeObjectJson(payload))
                .build());
    }

    private byte[] readUploadBytes(MultipartFile file, String errorMessage) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BaseException(errorMessage);
        }
    }

    private String sanitizeUploadFilename(String originalFilename, String defaultFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return defaultFilename;
        }
        return Path.of(originalFilename).getFileName().toString();
    }

    private String resolveUploadPublicUrl(String storagePath) {
        if (!"local".equalsIgnoreCase(storageProperties.getProvider())) {
            return storagePath;
        }
        Path baseDir = Path.of(storageProperties.getLocalBaseDir()).toAbsolutePath().normalize();
        Path filePath = Path.of(storagePath).toAbsolutePath().normalize();
        String relativePath = baseDir.relativize(filePath).toString().replace('\\', '/');
        return "/uploads/" + relativePath;
    }

    private String writeObjectJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BaseException("保存状态记录失败");
        }
    }
}
