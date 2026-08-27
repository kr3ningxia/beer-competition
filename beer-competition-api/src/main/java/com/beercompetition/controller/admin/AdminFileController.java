package com.beercompetition.controller.admin;

import com.beercompetition.controller.support.FileResponseHelper;
import com.beercompetition.file.FileAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台受控文件下载入口。具体业务归属和账号范围由文件访问服务判定。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/files")
public class AdminFileController {

    private final FileAccessService fileAccessService;

    @GetMapping("/{fileAssetId}")
    public ResponseEntity<byte[]> download(@PathVariable Long fileAssetId) {
        return FileResponseHelper.attachment(fileAccessService.download(fileAssetId));
    }
}
