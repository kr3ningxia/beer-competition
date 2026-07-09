package com.beercompetition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beercompetition.common.context.BaseContext;
import com.beercompetition.common.exception.BaseException;
import com.beercompetition.common.exception.ResourceNotFoundException;
import com.beercompetition.common.util.SimpleXlsxBuilder;
import com.beercompetition.mapper.AdminOperationLogMapper;
import com.beercompetition.mapper.BeerEntryExtraFieldMapper;
import com.beercompetition.mapper.BeerEntryMapper;
import com.beercompetition.mapper.BreweryMapper;
import com.beercompetition.mapper.CompetitionCategoryMapper;
import com.beercompetition.mapper.CompetitionMapper;
import com.beercompetition.mapper.EntryDeliveryMapper;
import com.beercompetition.mapper.EntryPaymentMapper;
import com.beercompetition.pojo.enums.EntryDeliveryStatus;
import com.beercompetition.pojo.enums.EntryPaymentStatus;
import com.beercompetition.pojo.enums.EntryScanLabelStatus;
import com.beercompetition.pojo.enums.EntryStatus;
import com.beercompetition.pojo.po.AdminOperationLog;
import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.BeerEntryExtraField;
import com.beercompetition.pojo.po.Brewery;
import com.beercompetition.pojo.po.Competition;
import com.beercompetition.pojo.po.CompetitionCategory;
import com.beercompetition.pojo.po.EntryDelivery;
import com.beercompetition.pojo.po.EntryPayment;
import com.beercompetition.pojo.po.EntryScanLabel;
import com.beercompetition.pojo.vo.FileDownloadVO;
import com.beercompetition.service.AdminExportService;
import com.beercompetition.service.EntryScanLabelService;
import com.beercompetition.service.support.EntryLabelFileGenerator;
import com.beercompetition.service.support.EntryLabelFileGenerator.LabelRenderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class AdminExportServiceImpl implements AdminExportService {

    private static final String TARGET_COMPETITION = "COMPETITION";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int LABEL_COPY_DEFAULT = 2;
    private static final int LABEL_COPY_MIN = 1;
    private static final int LABEL_COPY_MAX = 6;
    private static final String LABEL_EXPORT_FORMAT_PDF = "pdf";
    private static final Set<EntryStatus> LABEL_ALLOWED_ENTRY_STATUSES = EnumSet.of(
            EntryStatus.REGISTERED,
            EntryStatus.STORED,
            EntryStatus.RESULT_PUBLISHED
    );

    private final CompetitionMapper competitionMapper;
    private final CompetitionCategoryMapper competitionCategoryMapper;
    private final BeerEntryMapper beerEntryMapper;
    private final BeerEntryExtraFieldMapper beerEntryExtraFieldMapper;
    private final BreweryMapper breweryMapper;
    private final EntryPaymentMapper entryPaymentMapper;
    private final EntryDeliveryMapper entryDeliveryMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final EntryScanLabelService entryScanLabelService;
    private final EntryLabelFileGenerator entryLabelFileGenerator;

    @Override
    public FileDownloadVO exportEntries(Long competitionId, Long categoryId, String entryStatus, String paymentStatus,
                                        String deliveryStatus, String keyword) {
        // 1) 查询比赛与筛选后的报名数据
        ExportContext context = loadContext(competitionId, categoryId, entryStatus, paymentStatus, deliveryStatus, keyword);
        if (context.entries().isEmpty()) {
            throw new BaseException("当前筛选范围没有可导出的酒款");
        }

        // 2) 组装报名台账工作表
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("比赛名称", "比赛编号", "酒款 UUID", "匿名标签编码", "现场短编号", "酒款名称", "厂牌名称",
                "联系人", "投递组别", "基础风格", "ABV", "额外报名字段", "酒款状态", "支付状态", "报名金额", "支付时间", "报名时间"));
        for (BeerEntry entry : context.entries()) {
            EntryScanLabel label = context.labelsByEntryId().get(entry.getId());
            Brewery brewery = context.breweriesById().get(entry.getBreweryId());
            EntryPayment payment = context.paymentsByEntryId().get(entry.getId());
            rows.add(List.of(
                    context.competition().getName(),
                    value(context.competition().getCode()),
                    value(entry.getUuid()),
                    label == null ? "" : value(label.getLabelCode()),
                    label == null ? "" : value(label.getShortCode()),
                    value(entry.getName()),
                    brewery == null ? "" : value(brewery.getCompanyName()),
                    brewery == null ? "" : value(brewery.getContactName()),
                    categoryName(context.categoriesById().get(entry.getCategoryId())),
                    value(entry.getStyle()),
                    decimal(entry.getAbv()),
                    formatExtraFields(context.extraFieldsByEntryId().get(entry.getId())),
                    entryStatusLabel(entry.getStatus()),
                    paymentStatusLabel(resolvePaymentStatus(entry, payment)),
                    payment == null ? "" : decimal(payment.getAmount()),
                    payment == null ? "" : dateTime(payment.getPaidTime()),
                    dateTime(entry.getCreateTime())
            ));
        }

        // 3) 记录导出并返回 Excel
        logExport("EXPORT_ENTRIES", context.competition(), context.filters(), context.entries().size());
        return FileDownloadVO.builder()
                .fileName(safeFilename(context.competition().getName()) + "-报名酒款台账.xlsx")
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .content(SimpleXlsxBuilder.build(List.of(new SimpleXlsxBuilder.Sheet("报名酒款台账", rows))))
                .build();
    }

    @Override
    public FileDownloadVO exportDelivery(Long competitionId, Long categoryId, String entryStatus, String paymentStatus,
                                         String deliveryStatus, String keyword) {
        // 1) 查询比赛与筛选后的收样数据
        ExportContext context = loadContext(competitionId, categoryId, entryStatus, paymentStatus, deliveryStatus, keyword);
        if (context.entries().isEmpty()) {
            throw new BaseException("当前筛选范围没有可导出的酒款");
        }

        // 2) 组装收样入库工作表
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("比赛名称", "酒款 UUID", "现场短编号", "酒款名称", "厂牌名称", "组别", "风格", "送样方式",
                "快递公司", "快递单号", "送样备注", "送样状态", "提交时间", "签收时间", "入库状态", "签收备注"));
        for (BeerEntry entry : context.entries()) {
            EntryScanLabel label = context.labelsByEntryId().get(entry.getId());
            Brewery brewery = context.breweriesById().get(entry.getBreweryId());
            EntryDelivery delivery = context.deliveriesByEntryId().get(entry.getId());
            rows.add(List.of(
                    context.competition().getName(),
                    value(entry.getUuid()),
                    label == null ? "" : value(label.getShortCode()),
                    value(entry.getName()),
                    brewery == null ? "" : value(brewery.getCompanyName()),
                    categoryName(context.categoriesById().get(entry.getCategoryId())),
                    value(entry.getStyle()),
                    deliveryMethodLabel(delivery == null ? "" : delivery.getDeliveryMethod()),
                    delivery == null ? "" : value(delivery.getCarrier()),
                    delivery == null ? "" : value(delivery.getTrackingNo()),
                    delivery == null ? "" : value(delivery.getDeliveryNote()),
                    deliveryStatusLabel(resolveDeliveryStatus(delivery)),
                    delivery == null ? "" : dateTime(delivery.getSubmittedTime()),
                    delivery == null ? "" : dateTime(delivery.getReceivedTime()),
                    Objects.equals(entry.getStoredFlag(), 1) ? "已入库" : "未入库",
                    delivery == null ? "" : value(delivery.getReceiveRemark())
            ));
        }

        // 3) 记录导出并返回 Excel
        logExport("EXPORT_DELIVERY", context.competition(), context.filters(), context.entries().size());
        return FileDownloadVO.builder()
                .fileName(safeFilename(context.competition().getName()) + "-样品入库清单.xlsx")
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .content(SimpleXlsxBuilder.build(List.of(new SimpleXlsxBuilder.Sheet("样品入库清单", rows))))
                .build();
    }

    @Override
    public FileDownloadVO exportLabels(Long competitionId, Long categoryId, String entryStatus, String paymentStatus,
                                       String deliveryStatus, String keyword, Integer copies, String format) {
        // 1) 查询比赛与瓶贴候选数据
        int normalizedCopies = Math.min(Math.max(copies == null ? LABEL_COPY_DEFAULT : copies, LABEL_COPY_MIN), LABEL_COPY_MAX);
        ExportContext context = loadContext(competitionId, categoryId, entryStatus, paymentStatus, deliveryStatus, keyword);
        List<BeerEntry> labelEntries = context.entries().stream()
                .filter(entry -> canExportLabel(entry, context.paymentsByEntryId().get(entry.getId())))
                .filter(entry -> context.labelsByEntryId().get(entry.getId()) != null)
                .toList();
        if (labelEntries.isEmpty()) {
            throw new BaseException("这场比赛还没有生成现场标签");
        }

        // 2) 组装标签渲染数据
        List<LabelItem> labelItems = new ArrayList<>();
        for (BeerEntry entry : labelEntries) {
            EntryScanLabel label = context.labelsByEntryId().get(entry.getId());
            String categoryName = categoryName(context.categoriesById().get(entry.getCategoryId()));
            labelItems.add(new LabelItem(entry, label, categoryName, normalizedCopies));
        }

        // 3) 按格式生成 PDF 或兼容的图片文件包
        List<LabelRenderItem> expandedItems = expandLabelRenderItems(labelItems);
        if (LABEL_EXPORT_FORMAT_PDF.equalsIgnoreCase(value(format))) {
            byte[] content = entryLabelFileGenerator.buildFourUpPdf(expandedItems);
            logExport("EXPORT_LABELS", context.competition(), context.filters() + ", copies=" + normalizedCopies + ", format=pdf", labelEntries.size());
            return FileDownloadVO.builder()
                    .fileName(safeFilename(context.competition().getName()) + "-批量参赛标签.pdf")
                    .contentType(EntryLabelFileGenerator.CONTENT_TYPE_PDF)
                    .content(content)
                    .build();
        }

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
                int pageNo = 1;
                for (int index = 0; index < expandedItems.size(); index += 4) {
                    List<LabelRenderItem> pageItems = expandedItems.subList(index, Math.min(index + 4, expandedItems.size()));
                    String name = "label-sheets/page-" + String.format("%03d", pageNo) + ".png";
                    SimpleXlsxBuilder.writeZipEntry(zip, name, entryLabelFileGenerator.buildFourUpPng(pageItems));
                    pageNo++;
                }
                SimpleXlsxBuilder.writeZipEntry(zip, "labels.html", buildLabelsHtml(pageNo - 1));
                SimpleXlsxBuilder.writeZipEntry(zip, "标签清单.xlsx", buildLabelLedger(context, labelItems));
            }
            logExport("EXPORT_LABELS", context.competition(), context.filters() + ", copies=" + normalizedCopies + ", format=zip", labelEntries.size());
            return FileDownloadVO.builder()
                    .fileName(safeFilename(context.competition().getName()) + "-批量参赛标签.zip")
                    .contentType("application/zip")
                    .content(output.toByteArray())
                    .build();
        } catch (IOException ex) {
            throw new BaseException("导出瓶贴失败");
        }
    }

    @Override
    public void logScoringExport(Long competitionId) {
        Competition competition = requireCompetition(competitionId);
        logExport("EXPORT_SCORING", competition, "competitionId=" + competitionId, 0);
    }

    private ExportContext loadContext(Long competitionId, Long categoryId, String entryStatus, String paymentStatus,
                                      String deliveryStatus, String keyword) {
        Competition competition = requireCompetition(competitionId);
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        List<BeerEntry> rawEntries = beerEntryMapper.selectList(new LambdaQueryWrapper<BeerEntry>()
                .eq(BeerEntry::getCompetitionId, competitionId)
                .eq(categoryId != null, BeerEntry::getCategoryId, categoryId)
                .eq(StringUtils.hasText(entryStatus), BeerEntry::getStatus, entryStatus)
                .orderByAsc(BeerEntry::getCategoryId)
                .orderByAsc(BeerEntry::getId));
        List<Long> entryIds = rawEntries.stream().map(BeerEntry::getId).toList();
        Map<Long, EntryPayment> paymentsByEntryId = loadPayments(entryIds);
        Map<Long, EntryDelivery> deliveriesByEntryId = loadDeliveries(entryIds);
        Map<Long, EntryScanLabel> labelsByEntryId = entryScanLabelService.listActiveLabels(entryIds);
        Map<Long, CompetitionCategory> categoriesById = loadCategories(competitionId);
        Map<Long, Brewery> breweriesById = loadBreweries(rawEntries);
        Map<Long, List<BeerEntryExtraField>> extraFieldsByEntryId = loadExtraFields(entryIds);

        List<BeerEntry> entries = rawEntries.stream()
                .filter(entry -> !StringUtils.hasText(paymentStatus) || paymentStatus.equals(resolvePaymentStatus(entry, paymentsByEntryId.get(entry.getId()))))
                .filter(entry -> !StringUtils.hasText(deliveryStatus) || deliveryStatus.equals(resolveDeliveryStatus(deliveriesByEntryId.get(entry.getId()))))
                .filter(entry -> !StringUtils.hasText(normalizedKeyword) || matchesKeyword(entry, labelsByEntryId.get(entry.getId()),
                        categoriesById.get(entry.getCategoryId()), breweriesById.get(entry.getBreweryId()), normalizedKeyword))
                .toList();
        String filters = "competitionId=" + competitionId
                + ", categoryId=" + value(categoryId)
                + ", entryStatus=" + value(entryStatus)
                + ", paymentStatus=" + value(paymentStatus)
                + ", deliveryStatus=" + value(deliveryStatus)
                + ", keyword=" + value(keyword);
        return new ExportContext(competition, entries, categoriesById, breweriesById, paymentsByEntryId,
                deliveriesByEntryId, labelsByEntryId, extraFieldsByEntryId, filters);
    }

    private Competition requireCompetition(Long competitionId) {
        if (competitionId == null) {
            throw new BaseException("请选择比赛");
        }
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        return competition;
    }

    private Map<Long, EntryPayment> loadPayments(Collection<Long> entryIds) {
        if (entryIds.isEmpty()) {
            return Map.of();
        }
        return entryPaymentMapper.selectList(new LambdaQueryWrapper<EntryPayment>()
                        .in(EntryPayment::getBeerEntryId, entryIds))
                .stream()
                .collect(Collectors.toMap(EntryPayment::getBeerEntryId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, EntryDelivery> loadDeliveries(Collection<Long> entryIds) {
        if (entryIds.isEmpty()) {
            return Map.of();
        }
        return entryDeliveryMapper.selectList(new LambdaQueryWrapper<EntryDelivery>()
                        .in(EntryDelivery::getBeerEntryId, entryIds))
                .stream()
                .collect(Collectors.toMap(EntryDelivery::getBeerEntryId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, CompetitionCategory> loadCategories(Long competitionId) {
        return competitionCategoryMapper.selectList(new LambdaQueryWrapper<CompetitionCategory>()
                        .eq(CompetitionCategory::getCompetitionId, competitionId))
                .stream()
                .collect(Collectors.toMap(CompetitionCategory::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, Brewery> loadBreweries(List<BeerEntry> entries) {
        Set<Long> breweryIds = entries.stream()
                .map(BeerEntry::getBreweryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (breweryIds.isEmpty()) {
            return Map.of();
        }
        return breweryMapper.selectBatchIds(breweryIds)
                .stream()
                .collect(Collectors.toMap(Brewery::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, List<BeerEntryExtraField>> loadExtraFields(Collection<Long> entryIds) {
        if (entryIds.isEmpty()) {
            return Map.of();
        }
        return beerEntryExtraFieldMapper.selectList(new LambdaQueryWrapper<BeerEntryExtraField>()
                        .in(BeerEntryExtraField::getBeerEntryId, entryIds)
                        .orderByAsc(BeerEntryExtraField::getId))
                .stream()
                .collect(Collectors.groupingBy(BeerEntryExtraField::getBeerEntryId, LinkedHashMap::new, Collectors.toList()));
    }

    private boolean matchesKeyword(BeerEntry entry, EntryScanLabel label, CompetitionCategory category, Brewery brewery, String keyword) {
        return contains(entry.getUuid(), keyword)
                || contains(entry.getName(), keyword)
                || contains(entry.getStyle(), keyword)
                || contains(label == null ? null : label.getShortCode(), keyword)
                || contains(label == null ? null : label.getLabelCode(), keyword)
                || contains(category == null ? null : category.getName(), keyword)
                || contains(brewery == null ? null : brewery.getCompanyName(), keyword)
                || contains(brewery == null ? null : brewery.getContactName(), keyword);
    }

    private boolean canExportLabel(BeerEntry entry, EntryPayment payment) {
        EntryStatus status = parseEntryStatus(entry.getStatus());
        return !EntryStatus.CANCELED.equals(status)
                && LABEL_ALLOWED_ENTRY_STATUSES.contains(status)
                && EntryPaymentStatus.PAID.name().equals(resolvePaymentStatus(entry, payment));
    }

    private byte[] buildLabelLedger(ExportContext context, List<LabelItem> labelItems) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("参赛编号", "标签编码", "酒款 UUID", "组别", "风格", "ABV", "标签张数"));
        for (LabelItem item : labelItems) {
            rows.add(List.of(
                    item.label().getShortCode(),
                    item.label().getLabelCode(),
                    item.entry().getUuid(),
                    item.categoryName(),
                    value(item.entry().getStyle()),
                    decimal(item.entry().getAbv()),
                    String.valueOf(item.copies())
            ));
        }
        return SimpleXlsxBuilder.build(List.of(new SimpleXlsxBuilder.Sheet(context.competition().getName() + "瓶贴清单", rows)));
    }

    private String buildLabelsHtml(int pageCount) {
        StringBuilder html = new StringBuilder();
        html.append("""
                <!doctype html>
                <html lang="zh-CN">
                  <head>
                    <meta charset="utf-8">
                    <title>批量打印现场评审标签</title>
                    <style>
                      * { box-sizing: border-box; }
                      body { margin: 0; background: #fff; }
                      .sheet { display: block; width: 210mm; min-height: 297mm; margin: 0 auto; page-break-after: always; break-after: page; }
                      .sheet:last-child { page-break-after: auto; break-after: auto; }
                      .sheet img { display: block; width: 210mm; height: 297mm; object-fit: contain; }
                      @page { size: A4; margin: 0; }
                    </style>
                  </head>
                  <body>
                """);
        for (int pageNo = 1; pageNo <= pageCount; pageNo++) {
            String src = "label-sheets/page-" + String.format("%03d", pageNo) + ".png";
            html.append("<main class=\"sheet\"><img src=\"")
                    .append(src)
                    .append("\" alt=\"第 ")
                    .append(pageNo)
                    .append(" 页标签\"></main>\n");
        }
        html.append("""
                  </body>
                </html>
                """);
        return html.toString();
    }

    private List<LabelRenderItem> expandLabelRenderItems(List<LabelItem> labelItems) {
        List<LabelRenderItem> items = new ArrayList<>();
        for (LabelItem item : labelItems) {
            LabelRenderItem renderItem = toLabelRenderItem(item);
            for (int index = 0; index < item.copies(); index++) {
                items.add(renderItem);
            }
        }
        return items;
    }

    private LabelRenderItem toLabelRenderItem(LabelItem item) {
        return new LabelRenderItem(
                item.entry().getUuid(),
                item.label().getLabelCode(),
                item.label().getShortCode(),
                item.label().getScanToken(),
                item.categoryName()
        );
    }

    private void logExport(String action, Competition competition, String filters, int count) {
        Long adminId = BaseContext.getCurrentId();
        if (adminId == null) {
            return;
        }
        String summary = "{\"competitionId\":" + competition.getId()
                + ",\"count\":" + count
                + ",\"filters\":\"" + SimpleXlsxBuilder.escapeXml(filters).replace("\"", "\\\"") + "\"}";
        adminOperationLogMapper.insert(AdminOperationLog.builder()
                .adminUserId(adminId)
                .action(action)
                .targetType(TARGET_COMPETITION)
                .targetPublicId(String.valueOf(competition.getId()))
                .summary(summary)
                .build());
    }

    private String resolvePaymentStatus(BeerEntry entry, EntryPayment payment) {
        if (payment != null && StringUtils.hasText(payment.getStatus())) {
            return payment.getStatus();
        }
        if (EntryStatus.PENDING_PAYMENT.name().equals(entry.getStatus())) {
            return EntryPaymentStatus.UNPAID.name();
        }
        if (EntryStatus.CANCELED.name().equals(entry.getStatus())) {
            return EntryPaymentStatus.CANCELED.name();
        }
        return "";
    }

    private String resolveDeliveryStatus(EntryDelivery delivery) {
        return delivery == null ? EntryDeliveryStatus.NOT_SUBMITTED.name() : delivery.getDeliveryStatus();
    }

    private EntryStatus parseEntryStatus(String status) {
        try {
            return EntryStatus.valueOf(status);
        } catch (RuntimeException ex) {
            return EntryStatus.PENDING_PAYMENT;
        }
    }

    private String formatExtraFields(List<BeerEntryExtraField> fields) {
        if (fields == null || fields.isEmpty()) {
            return "";
        }
        return fields.stream()
                .sorted(Comparator.comparing(BeerEntryExtraField::getId))
                .map(field -> value(field.getFieldLabel()) + "：" + value(field.getFieldValue()))
                .collect(Collectors.joining("；"));
    }

    private String entryStatusLabel(String status) {
        return switch (value(status)) {
            case "PENDING_PAYMENT" -> "待支付";
            case "REGISTERED" -> "报名成功";
            case "STORED" -> "已入库";
            case "CANCELED" -> "已取消";
            case "RESULT_PUBLISHED" -> "结果已发布";
            default -> value(status);
        };
    }

    private String paymentStatusLabel(String status) {
        return switch (value(status)) {
            case "UNPAID" -> "待支付";
            case "PAID" -> "已支付";
            case "CANCELED" -> "已取消";
            case "REFUNDED" -> "已退款";
            default -> value(status);
        };
    }

    private String deliveryStatusLabel(String status) {
        return switch (value(status)) {
            case "NOT_SUBMITTED" -> "未提交";
            case "SUBMITTED" -> "已提交";
            case "RECEIVED" -> "已入库";
            default -> value(status);
        };
    }

    private String deliveryMethodLabel(String method) {
        return switch (value(method)) {
            case "EXPRESS" -> "快递寄送";
            case "ONSITE" -> "现场交样";
            default -> value(method);
        };
    }

    private String categoryName(CompetitionCategory category) {
        return category == null ? "" : value(category.getName());
    }

    private boolean contains(String text, String keyword) {
        return StringUtils.hasText(text) && text.toLowerCase().contains(keyword);
    }

    private String dateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME_FORMATTER.format(value);
    }

    private String decimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String safeFilename(String value) {
        String name = StringUtils.hasText(value) ? value.trim() : "导出文件";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private record ExportContext(Competition competition,
                                 List<BeerEntry> entries,
                                 Map<Long, CompetitionCategory> categoriesById,
                                 Map<Long, Brewery> breweriesById,
                                 Map<Long, EntryPayment> paymentsByEntryId,
                                 Map<Long, EntryDelivery> deliveriesByEntryId,
                                 Map<Long, EntryScanLabel> labelsByEntryId,
                                 Map<Long, List<BeerEntryExtraField>> extraFieldsByEntryId,
                                 String filters) {
    }

    private record LabelItem(BeerEntry entry, EntryScanLabel label, String categoryName, int copies) {
    }
}
