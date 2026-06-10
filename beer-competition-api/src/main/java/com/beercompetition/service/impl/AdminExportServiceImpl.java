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
import com.beercompetition.properties.ExportProperties;
import com.beercompetition.service.AdminExportService;
import com.beercompetition.service.EntryScanLabelService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
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
    private final ExportProperties exportProperties;

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
                "联系人", "投递组别", "基础风格", "ABV", "额外报名字段", "酒款状态", "付款状态", "报名金额", "付款时间", "报名时间"));
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
        rows.add(List.of("比赛名称", "酒款 UUID", "现场短编号", "酒款名称", "厂牌名称", "组别", "风格", "寄样方式",
                "快递公司", "快递单号", "寄样备注", "送样状态", "提交时间", "签收时间", "入库状态", "签收备注"));
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
                .fileName(safeFilename(context.competition().getName()) + "-收样入库清单.xlsx")
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .content(SimpleXlsxBuilder.build(List.of(new SimpleXlsxBuilder.Sheet("收样入库清单", rows))))
                .build();
    }

    @Override
    public FileDownloadVO exportLabels(Long competitionId, Long categoryId, String entryStatus, String paymentStatus,
                                       String deliveryStatus, String keyword, Integer copies) {
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

        // 2) 生成标签清单、单张 PNG 和打印 HTML
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
                List<LabelItem> labelItems = new ArrayList<>();
                for (BeerEntry entry : labelEntries) {
                    EntryScanLabel label = context.labelsByEntryId().get(entry.getId());
                    String categoryName = categoryName(context.categoriesById().get(entry.getCategoryId()));
                    LabelItem item = new LabelItem(entry, label, categoryName, normalizedCopies);
                    labelItems.add(item);
                    for (int index = 1; index <= normalizedCopies; index++) {
                        String name = "labels/" + safeFilename(label.getShortCode()) + "-" + index + ".png";
                        SimpleXlsxBuilder.writeZipEntry(zip, name, buildLabelPng(item));
                    }
                }
                SimpleXlsxBuilder.writeZipEntry(zip, "labels.html", buildLabelsHtml(labelItems));
                SimpleXlsxBuilder.writeZipEntry(zip, "标签清单.xlsx", buildLabelLedger(context, labelItems));
            }
            logExport("EXPORT_LABELS", context.competition(), context.filters() + ", copies=" + normalizedCopies, labelEntries.size());
            return FileDownloadVO.builder()
                    .fileName(safeFilename(context.competition().getName()) + "-批量瓶贴.zip")
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
        rows.add(List.of("短编号", "标签编码", "酒款 UUID", "组别", "风格", "ABV", "标签张数"));
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

    private byte[] buildLabelPng(LabelItem item) {
        try {
            int width = 1040;
            int height = 1440;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(0xfff8ec));
            g.fillRect(0, 0, width, height);
            g.setColor(new Color(0xfffdf9));
            g.fillRoundRect(56, 56, 928, 1328, 72, 72);
            g.setColor(new Color(0xd4bf9f));
            g.setStroke(new BasicStroke(4));
            g.drawRoundRect(56, 56, 928, 1328, 72, 72);

            drawCenteredText(g, "现场评审标签", new Font("Microsoft YaHei", Font.BOLD, 48), new Color(0x8c6330), 520, 176);
            g.setColor(new Color(0xf7ecd8));
            g.fillRoundRect(112, 256, 816, 816, 72, 72);
            g.setColor(new Color(0x3a2818));
            g.setStroke(new BasicStroke(40));
            g.drawRoundRect(112, 256, 816, 816, 72, 72);

            BufferedImage qr = buildQrImage(scanUrl(item.label()), 704);
            g.drawImage(qr, 168, 312, null);
            drawCenteredText(g, "现场短编号", new Font("Microsoft YaHei", Font.BOLD, 48), new Color(0x8c6330), 520, 1168);
            drawCenteredText(g, value(item.label().getShortCode()), new Font("Microsoft YaHei", Font.BOLD, 112), new Color(0x24170f), 520, 1272);
            drawCenteredText(g, fitMetaLine(item), new Font("Microsoft YaHei", Font.PLAIN, 44), new Color(0x665647), 520, 1360);
            g.dispose();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new BaseException("标签图片生成失败");
        }
    }

    private BufferedImage buildQrImage(String text, int size) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 0);
        var matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints);
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                image.setRGB(x, y, matrix.get(x, y) ? 0x2e2014 : 0xf7ecd8);
            }
        }
        return image;
    }

    private String buildLabelsHtml(List<LabelItem> labelItems) {
        StringBuilder html = new StringBuilder();
        html.append("""
                <!doctype html>
                <html lang="zh-CN">
                  <head>
                    <meta charset="utf-8">
                    <title>批量打印现场评审标签</title>
                    <style>
                      * { box-sizing: border-box; }
                      body { margin: 0; padding: 16px; font-family: "Microsoft YaHei", sans-serif; background: #f5ead9; }
                      .sheet { display: grid; grid-template-columns: repeat(3, 64mm); gap: 8mm 6mm; align-items: start; }
                      .label { page-break-inside: avoid; break-inside: avoid; padding: 0; background: #fffdf9; }
                      .label img { display: block; width: 64mm; height: auto; }
                      @page { size: A4; margin: 10mm; }
                      @media print { body { padding: 0; background: #fff; } .sheet { gap: 6mm; } }
                    </style>
                  </head>
                  <body>
                    <main class="sheet">
                """);
        for (LabelItem item : labelItems) {
            for (int index = 1; index <= item.copies(); index++) {
                String src = "labels/" + urlPath(item.label().getShortCode()) + "-" + index + ".png";
                html.append("<article class=\"label\"><img src=\"")
                        .append(src)
                        .append("\" alt=\"")
                        .append(SimpleXlsxBuilder.escapeXml(item.label().getShortCode()))
                        .append("\"></article>\n");
            }
        }
        html.append("""
                    </main>
                  </body>
                </html>
                """);
        return html.toString();
    }

    private String scanUrl(EntryScanLabel label) {
        String baseUrl = exportProperties.getJudgeH5BaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            baseUrl = "http://localhost:5174";
        }
        String normalizedBase = baseUrl.replaceAll("/+$", "");
        return normalizedBase + "/q/" + URLEncoder.encode(label.getScanToken(), StandardCharsets.UTF_8);
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

    private void drawCenteredText(Graphics2D g, String text, Font font, Color color, int centerX, int baselineY) {
        g.setFont(font);
        g.setColor(color);
        FontMetrics metrics = g.getFontMetrics();
        int x = centerX - metrics.stringWidth(text) / 2;
        g.drawString(text, x, baselineY);
    }

    private String fitMetaLine(LabelItem item) {
        String category = StringUtils.hasText(item.categoryName()) ? item.categoryName() : "待确认组别";
        String style = StringUtils.hasText(item.entry().getStyle()) ? item.entry().getStyle() : "Style Pending";
        String abv = item.entry().getAbv() == null ? "ABV Pending" : decimal(item.entry().getAbv()) + "%";
        String text = category + " · " + style + " · " + abv;
        return text.length() > 28 ? text.substring(0, 27) + "…" : text;
    }

    private String entryStatusLabel(String status) {
        return switch (value(status)) {
            case "PENDING_PAYMENT" -> "待付款";
            case "REGISTERED" -> "报名成功";
            case "STORED" -> "已入库";
            case "CANCELED" -> "已取消";
            case "RESULT_PUBLISHED" -> "结果已发布";
            default -> value(status);
        };
    }

    private String paymentStatusLabel(String status) {
        return switch (value(status)) {
            case "UNPAID" -> "待付款";
            case "PAID" -> "已付款";
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

    private String urlPath(String value) {
        return URLEncoder.encode(safeFilename(value), StandardCharsets.UTF_8).replace("+", "%20");
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
