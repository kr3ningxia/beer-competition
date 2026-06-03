package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.mapper.EntryScanLabelMapper;
import com.beercompetition.pojo.enums.EntryScanLabelStatus;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.EntryScanLabel;
import com.beercompetition.service.EntryScanLabelService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EntryScanLabelServiceImpl implements EntryScanLabelService {

    private static final int SHORT_CODE_BOUND = 36 * 36 * 36 * 36 * 36;
    private static final int CODE_RETRY_LIMIT = 20;
    private static final String LABEL_CODE_PREFIX = "BE-";
    private static final String TOKEN_PREFIX = "Q";

    private final EntryScanLabelMapper entryScanLabelMapper;

    @Override
    public EntryScanLabel createActiveLabel(BeerEntry entry, Long generatedBy) {
        // 1) 校验作品上下文
        if (entry == null || entry.getId() == null || entry.getCompetitionId() == null) {
            throw new BaseException("酒款信息不完整，无法生成现场标签");
        }

        // 2) 创建唯一扫码标签，并通过唯一索引处理极低概率并发冲突
        for (int i = 0; i < CODE_RETRY_LIMIT; i++) {
            EntryScanLabel label = EntryScanLabel.builder()
                    .competitionId(entry.getCompetitionId())
                    .beerEntryId(entry.getId())
                    .labelCode(generateLabelCode())
                    .shortCode(generateShortCode())
                    .scanToken(generateScanToken())
                    .status(EntryScanLabelStatus.ACTIVE.name())
                    .generatedBy(generatedBy)
                    .generatedTime(LocalDateTime.now())
                    .build();
            try {
                entryScanLabelMapper.insert(label);
                return label;
            } catch (DuplicateKeyException ex) {
                if (i == CODE_RETRY_LIMIT - 1) {
                    throw new BaseException("现场标签生成失败，请重试");
                }
            }
        }
        throw new BaseException("现场标签生成失败，请重试");
    }

    @Override
    public EntryScanLabel requireActiveLabel(Long beerEntryId) {
        // 1) 查询作品当前有效标签
        EntryScanLabel label = entryScanLabelMapper.selectOne(new LambdaQueryWrapper<EntryScanLabel>()
                .eq(EntryScanLabel::getBeerEntryId, beerEntryId)
                .eq(EntryScanLabel::getStatus, EntryScanLabelStatus.ACTIVE.name())
                .last("LIMIT 1"));
        if (label == null) {
            throw new ResourceNotFoundException("现场标签不存在");
        }
        return label;
    }

    @Override
    public EntryScanLabel resolveActiveLabel(String code) {
        // 1) 规范化扫码或手输内容
        String normalized = normalizeCode(code);

        // 2) 使用 token、短编号或匿名标签编码解析有效标签
        EntryScanLabel label = entryScanLabelMapper.selectOne(new LambdaQueryWrapper<EntryScanLabel>()
                .eq(EntryScanLabel::getStatus, EntryScanLabelStatus.ACTIVE.name())
                .and(wrapper -> wrapper
                        .eq(EntryScanLabel::getScanToken, normalized)
                        .or()
                        .eq(EntryScanLabel::getShortCode, normalized)
                        .or()
                        .eq(EntryScanLabel::getLabelCode, normalized))
                .last("LIMIT 1"));
        if (label == null) {
            throw new ResourceNotFoundException("现场标签不存在");
        }
        return label;
    }

    @Override
    public Map<Long, EntryScanLabel> listActiveLabels(Collection<Long> beerEntryIds) {
        // 1) 批量查询作品当前有效标签
        if (beerEntryIds == null || beerEntryIds.isEmpty()) {
            return Map.of();
        }
        return entryScanLabelMapper.selectList(new LambdaQueryWrapper<EntryScanLabel>()
                        .in(EntryScanLabel::getBeerEntryId, beerEntryIds)
                        .eq(EntryScanLabel::getStatus, EntryScanLabelStatus.ACTIVE.name()))
                .stream()
                .collect(Collectors.toMap(EntryScanLabel::getBeerEntryId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private String normalizeCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BaseException("请提供二维码或短编号");
        }
        return code.trim().toUpperCase();
    }

    private String generateLabelCode() {
        for (int i = 0; i < CODE_RETRY_LIMIT; i++) {
            String code = LABEL_CODE_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
            if (!existsByLabelCode(code)) {
                return code;
            }
        }
        throw new BaseException("匿名编码生成失败，请重试");
    }

    private String generateShortCode() {
        for (int i = 0; i < CODE_RETRY_LIMIT; i++) {
            String suffix = Integer.toString(ThreadLocalRandom.current().nextInt(SHORT_CODE_BOUND), 36).toUpperCase();
            String code = "0".repeat(5 - suffix.length()) + suffix;
            if (!existsByShortCode(code)) {
                return code;
            }
        }
        throw new BaseException("现场短编号生成失败，请重试");
    }

    private String generateScanToken() {
        for (int i = 0; i < CODE_RETRY_LIMIT; i++) {
            String token = TOKEN_PREFIX + UUID.randomUUID().toString().replace("-", "").toUpperCase();
            if (!existsByScanToken(token)) {
                return token;
            }
        }
        throw new BaseException("二维码令牌生成失败，请重试");
    }

    private boolean existsByLabelCode(String labelCode) {
        return entryScanLabelMapper.selectCount(new LambdaQueryWrapper<EntryScanLabel>()
                .eq(EntryScanLabel::getLabelCode, labelCode)) > 0;
    }

    private boolean existsByShortCode(String shortCode) {
        return entryScanLabelMapper.selectCount(new LambdaQueryWrapper<EntryScanLabel>()
                .eq(EntryScanLabel::getShortCode, shortCode)) > 0;
    }

    private boolean existsByScanToken(String scanToken) {
        return entryScanLabelMapper.selectCount(new LambdaQueryWrapper<EntryScanLabel>()
                .eq(EntryScanLabel::getScanToken, scanToken)) > 0;
    }
}
