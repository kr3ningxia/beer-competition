package com.beercompetition.service.support;

import com.beercompetition.properties.ExportProperties;
import com.beercompetition.service.support.EntryLabelFileGenerator.LabelRenderItem;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntryLabelFileGeneratorTest {

    @Test
    void buildsFourUpPngAndPdfWithBundledChineseFont() throws Exception {
        ExportProperties properties = new ExportProperties();
        properties.setJudgeH5BaseUrl("https://competitions.beermatters.cn/judge");
        EntryLabelFileGenerator generator = new EntryLabelFileGenerator(properties);
        LabelRenderItem item = new LabelRenderItem(
                "BE-TEST000001",
                "LABEL-TEST000001",
                "7HMYX",
                "scan-token",
                "啤酒组 1.12 IPA New England IPA"
        );

        byte[] png = generator.buildFourUpPng(List.of(item, item, item, item));
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertNotNull(image);
        assertEquals(2480, image.getWidth());
        assertEquals(3508, image.getHeight());

        byte[] pdf = generator.buildFourUpPdf(List.of(item, item, item, item));
        assertTrue(pdf.length > 1024);
        assertEquals('%', pdf[0]);
        assertEquals('P', pdf[1]);
        assertTrue(hasNotoSansScFont());
        assertTrue(new Font("Noto Sans SC", Font.PLAIN, 12).canDisplay('组'));
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
