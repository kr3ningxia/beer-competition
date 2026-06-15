package com.beercompetition.controller.admin;

import com.beercompetition.common.result.Result;
import com.beercompetition.controller.support.FileResponseHelper;
import com.beercompetition.pojo.dto.AwardConfirmRequest;
import com.beercompetition.pojo.vo.AwardResultVO;
import com.beercompetition.pojo.vo.AwardRuleVO;
import com.beercompetition.service.AwardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 后台奖项管理接口，承接奖项规则、获奖结果和证书文件操作。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/competitions/{id}")
public class AdminAwardController {

    private final AwardService awardService;

    /**
     * 查询指定比赛的奖项规则。
     */
    @GetMapping("/award-rules")
    public Result<List<AwardRuleVO>> awardRules(@PathVariable Long id) {
        return Result.success(awardService.listAwardRules(id));
    }

    /**
     * 查询指定比赛的获奖结果。
     */
    @GetMapping("/awards")
    public Result<List<AwardResultVO>> awards(@PathVariable Long id) {
        return Result.success(awardService.listAwardResults(id));
    }

    /**
     * 根据比赛结果生成奖项草稿。
     */
    @PostMapping("/awards/generate")
    public Result<List<AwardResultVO>> generateAwards(@PathVariable Long id) {
        return Result.success(awardService.generateAwardDrafts(id));
    }

    /**
     * 确认并保存后台调整后的奖项结果。
     */
    @PutMapping("/awards/confirm")
    public Result<List<AwardResultVO>> confirmAwards(@PathVariable Long id,
                                                     @RequestBody @Valid AwardConfirmRequest request) {
        return Result.success(awardService.confirmAwards(id, request));
    }

    /**
     * 上传单个获奖结果的奖状证书文件。
     */
    @PostMapping(value = "/awards/{awardId}/certificate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<AwardResultVO> uploadAwardCertificate(@PathVariable Long id,
                                                        @PathVariable Long awardId,
                                                        @RequestParam("file") MultipartFile file) {
        return Result.success(awardService.uploadCertificate(id, awardId, file));
    }

    /**
     * 删除单个获奖结果已绑定的奖状证书。
     */
    @DeleteMapping("/awards/{awardId}/certificate")
    public Result<String> deleteAwardCertificate(@PathVariable Long id,
                                                 @PathVariable Long awardId) {
        awardService.deleteCertificate(id, awardId);
        return Result.success("删除成功");
    }

    /**
     * 下载单个获奖结果的奖状证书。
     */
    @GetMapping("/awards/{awardId}/certificate")
    public ResponseEntity<byte[]> downloadAwardCertificate(@PathVariable Long id,
                                                           @PathVariable Long awardId) {
        return FileResponseHelper.attachment(awardService.downloadCertificate(id, awardId));
    }
}
