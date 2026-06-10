package com.beercompetition.common.util;

import com.beercompetition.common.exception.BaseException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class SimpleXlsxBuilder {

    private SimpleXlsxBuilder() {
    }

    public record Sheet(String name, List<List<String>> rows) {
    }

    public static byte[] build(List<Sheet> sheets) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
                writeZipEntry(zip, "[Content_Types].xml", buildContentTypesXml(sheets.size()));
                writeZipEntry(zip, "_rels/.rels", """
                        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                        </Relationships>
                        """);
                writeZipEntry(zip, "xl/workbook.xml", buildWorkbookXml(sheets));
                writeZipEntry(zip, "xl/_rels/workbook.xml.rels", buildWorkbookRelationshipsXml(sheets.size()));
                writeZipEntry(zip, "xl/styles.xml", """
                        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                        <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                          <fonts count="2"><font/><font><b/></font></fonts>
                          <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
                          <borders count="1"><border/></borders>
                          <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
                          <cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0"/></cellXfs>
                        </styleSheet>
                        """);
                for (int index = 0; index < sheets.size(); index++) {
                    writeZipEntry(zip, "xl/worksheets/sheet" + (index + 1) + ".xml", buildSheetXml(sheets.get(index).rows()));
                }
            }
            return output.toByteArray();
        } catch (IOException ex) {
            throw new BaseException("导出 Excel 失败");
        }
    }

    public static String escapeXml(String value) {
        String text = value == null ? "" : value;
        StringBuilder escaped = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&apos;");
                default -> escaped.append(ch);
            }
        }
        return escaped.toString();
    }

    public static void writeZipEntry(ZipOutputStream zip, String name, String content) throws IOException {
        writeZipEntry(zip, name, content.getBytes(StandardCharsets.UTF_8));
    }

    public static void writeZipEntry(ZipOutputStream zip, String name, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }

    private static String buildContentTypesXml(int sheetCount) {
        StringBuilder xml = new StringBuilder();
        xml.append("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                """);
        for (int index = 1; index <= sheetCount; index++) {
            xml.append("  <Override PartName=\"/xl/worksheets/sheet")
                    .append(index)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n");
        }
        xml.append("</Types>");
        return xml.toString();
    }

    private static String buildWorkbookRelationshipsXml(int sheetCount) {
        StringBuilder xml = new StringBuilder();
        xml.append("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                """);
        for (int index = 1; index <= sheetCount; index++) {
            xml.append("  <Relationship Id=\"rId")
                    .append(index)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                    .append(index)
                    .append(".xml\"/>\n");
        }
        xml.append("  <Relationship Id=\"rId")
                .append(sheetCount + 1)
                .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>\n");
        xml.append("</Relationships>");
        return xml.toString();
    }

    private static String buildWorkbookXml(List<Sheet> sheets) {
        StringBuilder xml = new StringBuilder();
        xml.append("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets>
                """);
        for (int index = 0; index < sheets.size(); index++) {
            xml.append("    <sheet name=\"")
                    .append(escapeXml(sheets.get(index).name()))
                    .append("\" sheetId=\"")
                    .append(index + 1)
                    .append("\" r:id=\"rId")
                    .append(index + 1)
                    .append("\"/>\n");
        }
        xml.append("""
                  </sheets>
                </workbook>
                """);
        return xml.toString();
    }

    private static String buildSheetXml(List<List<String>> rows) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
                .append("<sheetData>");
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            int rowNo = rowIndex + 1;
            xml.append("<row r=\"").append(rowNo).append("\">");
            List<String> row = rows.get(rowIndex);
            for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                String ref = columnName(columnIndex + 1) + rowNo;
                xml.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"");
                if (rowIndex == 0) {
                    xml.append(" s=\"1\"");
                }
                xml.append("><is><t>")
                        .append(escapeXml(row.get(columnIndex)))
                        .append("</t></is></c>");
            }
            xml.append("</row>");
        }
        xml.append("</sheetData></worksheet>");
        return xml.toString();
    }

    private static String columnName(int columnNo) {
        StringBuilder name = new StringBuilder();
        int value = columnNo;
        while (value > 0) {
            int remainder = (value - 1) % 26;
            name.insert(0, (char) ('A' + remainder));
            value = (value - 1) / 26;
        }
        return name.toString();
    }
}
