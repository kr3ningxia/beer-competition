package com.beercompetition.controller.support;

import com.beercompetition.pojo.vo.FileDownloadVO;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/**
 * 文件响应工具，统一设置下载文件名、内容类型和响应体。
 */
public final class FileResponseHelper {

    private FileResponseHelper() {
    }

    /**
     * 根据服务层返回的文件信息生成附件下载响应。
     */
    public static ResponseEntity<byte[]> attachment(FileDownloadVO file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.getFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .body(file.getContent());
    }

    /**
     * 生成固定文件名的 Excel 下载响应。
     */
    public static ResponseEntity<byte[]> xlsx(String filename, byte[] content) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }
}
