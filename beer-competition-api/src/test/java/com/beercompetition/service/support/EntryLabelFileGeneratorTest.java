package com.beercompetition.service.support;

import com.beercompetition.properties.ExportProperties;
import com.beercompetition.service.support.EntryLabelFileGenerator.LabelRenderItem;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntryLabelFileGeneratorTest {

    @Test
    void buildsNineUpLabelSheetsWithBundledChineseFont() throws Exception {
        ExportProperties properties = new ExportProperties();
        properties.setJudgeH5BaseUrl("https://competitions.beermatters.cn/judge");
        EntryLabelFileGenerator generator = new EntryLabelFileGenerator(properties);
        LabelRenderItem item = new LabelRenderItem(
                "BE-TEST000001",
                "LABEL-TEST000001",
                "7HMYX",
                "scan-token",
                "3. Fermentis · 100% 国产酒花组（酒花型啤酒）· 中低酒精度啤酒 NABLAB: No- and Low-Alcohol Beers"
        );
        List<LabelRenderItem> fullPage = Collections.nCopies(EntryLabelFileGenerator.LABELS_PER_PAGE, item);

        byte[] png = generator.buildLabelSheetPng(fullPage);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertNotNull(image);
        assertEquals(2480, image.getWidth());
        assertEquals(3508, image.getHeight());

        byte[] pdf = generator.buildLabelSheetPdf(fullPage);
        assertTrue(pdf.length > 1024);
        assertEquals('%', pdf[0]);
        assertEquals('P', pdf[1]);
        assertEquals(1, pdfPageCount(pdf));

        byte[] overflowPdf = generator.buildLabelSheetPdf(
                Collections.nCopies(EntryLabelFileGenerator.LABELS_PER_PAGE + 1, item));
        assertEquals(2, pdfPageCount(overflowPdf));
        assertTrue(hasNotoSansScFont());
        assertTrue(new Font("Noto Sans SC", Font.PLAIN, 12).canDisplay('组'));
    }

    private int pdfPageCount(byte[] pdf) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getNumberOfPages();
        }
    }

    @Test
    void wrapsLongCategoryTextWithinTheLabelArea() {
        BufferedImage image = new BufferedImage(1040, 1440, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            EntryLabelFileGenerator.CategoryTextLayout layout = EntryLabelFileGenerator.layoutCategoryText(graphics,
                    "组别：3. Fermentis · 100% 国产酒花组（酒花型啤酒）· 中低酒精度啤酒 NABLAB: No- and Low-Alcohol Beers");

            assertTrue(layout.lines().size() > 1);
            assertTrue(layout.lines().size() <= 3);
            graphics.setFont(layout.font());
            for (String line : layout.lines()) {
                assertTrue(graphics.getFontMetrics().stringWidth(line) <= 820);
            }
        } finally {
            graphics.dispose();
        }
    }

    private boolean hasNotoSansScFont() {
        for (String familyName : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if ("Noto Sans SC".equals(familyName)) {
                return true;
            }
        }
        return false;
    }
}
