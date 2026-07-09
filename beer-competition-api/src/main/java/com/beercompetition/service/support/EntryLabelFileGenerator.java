package com.beercompetition.service.support;

import com.beercompetition.common.exception.BaseException;
import com.beercompetition.properties.ExportProperties;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EntryLabelFileGenerator {

    public static final String CONTENT_TYPE_PNG = "image/png";
    public static final String CONTENT_TYPE_PDF = "application/pdf";

    private static final int LABEL_IMAGE_WIDTH = 1040;
    private static final int LABEL_IMAGE_HEIGHT = 1440;
    private static final int A4_IMAGE_WIDTH = 2480;
    private static final int A4_IMAGE_HEIGHT = 3508;
    private static final int LABELS_PER_PAGE = 4;
    private static final float PDF_MARGIN_X = 30f;
    private static final float PDF_MARGIN_Y = 36f;
    private static final float PDF_GAP_X = 20f;
    private static final float PDF_GAP_Y = 56f;
    private static final String FONT_FAMILY = registerLabelFonts();

    private final ExportProperties exportProperties;

    public byte[] buildLabelPng(LabelRenderItem item) {
        try {
            BufferedImage image = buildLabelImage(item);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new BaseException("标签图片生成失败");
        }
    }

    public byte[] buildFourUpPng(List<LabelRenderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BaseException("没有可导出的参赛标签");
        }
        try {
            BufferedImage image = buildFourUpImage(items);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new BaseException("标签图片生成失败");
        }
    }

    public byte[] buildFourUpPdf(List<LabelRenderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BaseException("没有可导出的参赛标签");
        }
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int index = 0; index < items.size(); index += LABELS_PER_PAGE) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                drawLabelPage(document, page, items.subList(index, Math.min(index + LABELS_PER_PAGE, items.size())));
            }
            document.save(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new BaseException("标签 PDF 生成失败");
        }
    }

    public String scanUrl(LabelRenderItem item) {
        String baseUrl = exportProperties.getJudgeH5BaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            baseUrl = "http://localhost:5174";
        }
        String token = firstText(item.scanToken(), item.shortCode(), item.labelCode(), item.uuid());
        return baseUrl.replaceAll("/+$", "") + "/q/" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private BufferedImage buildLabelImage(LabelRenderItem item) throws Exception {
        BufferedImage image = new BufferedImage(LABEL_IMAGE_WIDTH, LABEL_IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(new Color(0xfff8ec));
        g.fillRect(0, 0, LABEL_IMAGE_WIDTH, LABEL_IMAGE_HEIGHT);
        g.setColor(new Color(0xfffdf9));
        g.fillRoundRect(56, 56, 928, 1328, 72, 72);
        g.setColor(new Color(0xd4bf9f));
        g.setStroke(new BasicStroke(4));
        g.drawRoundRect(56, 56, 928, 1328, 72, 72);

        drawCenteredText(g, "现场评审标签", labelFont(Font.BOLD, 48), new Color(0x8c6330), 520, 176);
        g.setColor(new Color(0xf7ecd8));
        g.fillRoundRect(112, 256, 816, 816, 72, 72);
        g.setColor(new Color(0x3a2818));
        g.setStroke(new BasicStroke(40));
        g.drawRoundRect(112, 256, 816, 816, 72, 72);

        BufferedImage qr = buildQrImage(scanUrl(item), 704);
        g.drawImage(qr, 168, 312, null);
        drawCenteredText(g, "参赛编号", labelFont(Font.BOLD, 48), new Color(0x8c6330), 520, 1168);
        drawCenteredText(g, firstText(item.shortCode(), "PENDING"), labelFont(Font.BOLD, 112), new Color(0x24170f), 520, 1272);
        drawCenteredText(g, "组别：" + firstText(item.categoryName(), "待确认组别"), labelFont(Font.PLAIN, 44), new Color(0x665647), 520, 1360);
        g.dispose();
        return image;
    }

    private BufferedImage buildFourUpImage(List<LabelRenderItem> items) throws Exception {
        BufferedImage sheet = new BufferedImage(A4_IMAGE_WIDTH, A4_IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, A4_IMAGE_WIDTH, A4_IMAGE_HEIGHT);

        PDRectangle mediaBox = PDRectangle.A4;
        float labelWidth = (mediaBox.getWidth() - PDF_MARGIN_X * 2 - PDF_GAP_X) / 2;
        float labelHeight = labelWidth * LABEL_IMAGE_HEIGHT / LABEL_IMAGE_WIDTH;
        float topY = mediaBox.getHeight() - PDF_MARGIN_Y - labelHeight;
        float bottomY = topY - PDF_GAP_Y - labelHeight;
        float rightX = PDF_MARGIN_X + labelWidth + PDF_GAP_X;
        float scaleX = A4_IMAGE_WIDTH / mediaBox.getWidth();
        float scaleY = A4_IMAGE_HEIGHT / mediaBox.getHeight();

        float[][] positions = new float[][]{
                {PDF_MARGIN_X, topY},
                {rightX, topY},
                {PDF_MARGIN_X, bottomY},
                {rightX, bottomY}
        };
        for (int i = 0; i < Math.min(items.size(), LABELS_PER_PAGE); i++) {
            BufferedImage label = buildLabelImage(items.get(i));
            int x = Math.round(positions[i][0] * scaleX);
            int y = A4_IMAGE_HEIGHT - Math.round((positions[i][1] + labelHeight) * scaleY);
            int width = Math.round(labelWidth * scaleX);
            int height = Math.round(labelHeight * scaleY);
            g.drawImage(label, x, y, width, height, null);
        }
        g.dispose();
        return sheet;
    }

    private void drawLabelPage(PDDocument document, PDPage page, List<LabelRenderItem> items) throws Exception {
        PDRectangle mediaBox = page.getMediaBox();
        float labelWidth = (mediaBox.getWidth() - PDF_MARGIN_X * 2 - PDF_GAP_X) / 2;
        float labelHeight = labelWidth * LABEL_IMAGE_HEIGHT / LABEL_IMAGE_WIDTH;
        float topY = mediaBox.getHeight() - PDF_MARGIN_Y - labelHeight;
        float bottomY = topY - PDF_GAP_Y - labelHeight;
        float rightX = PDF_MARGIN_X + labelWidth + PDF_GAP_X;

        float[][] positions = new float[][]{
                {PDF_MARGIN_X, topY},
                {rightX, topY},
                {PDF_MARGIN_X, bottomY},
                {rightX, bottomY}
        };
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            for (int i = 0; i < items.size(); i++) {
                BufferedImage image = buildLabelImage(items.get(i));
                PDImageXObject pdfImage = LosslessFactory.createFromImage(document, image);
                contentStream.drawImage(pdfImage, positions[i][0], positions[i][1], labelWidth, labelHeight);
            }
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

    private void drawCenteredText(Graphics2D g, String text, Font font, Color color, int centerX, int baselineY) {
        g.setFont(font);
        g.setColor(color);
        FontMetrics metrics = g.getFontMetrics();
        int x = centerX - metrics.stringWidth(text) / 2;
        g.drawString(text, x, baselineY);
    }

    private Font labelFont(int style, int size) {
        return new Font(FONT_FAMILY, style, size);
    }

    private static String registerLabelFonts() {
        registerFont("/fonts/NotoSansSC-Regular.ttf");
        registerFont("/fonts/NotoSansSC-Bold.ttf");
        return "Noto Sans SC";
    }

    private static void registerFont(String resourcePath) {
        try (InputStream inputStream = EntryLabelFileGenerator.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                return;
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, inputStream);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
        } catch (Exception ignored) {
            // Fall back to system font lookup if bundled fonts cannot be loaded.
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    public record LabelRenderItem(String uuid,
                                  String labelCode,
                                  String shortCode,
                                  String scanToken,
                                  String categoryName) {
    }
}
