package com.beercompetition.controller.judge;

import com.beercompetition.common.result.Result;
import com.beercompetition.controller.support.FileResponseHelper;
import com.beercompetition.pojo.dto.JudgeProfileUpdateRequest;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.vo.CompetitionVO;
import com.beercompetition.pojo.vo.CurrentUserResponse;
import com.beercompetition.pojo.vo.JudgeAccountVO;
import com.beercompetition.service.AuthService;
import com.beercompetition.service.JudgeAvatarService;
import com.beercompetition.service.JudgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * 评委账号接口，提供评委登录态、资料维护和参评比赛入口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/judge")
public class JudgeAccountController {

    private final AuthService authService;
    private final JudgeService judgeService;
    private final JudgeAvatarService judgeAvatarService;

    /**
     * 获取当前评委登录身份信息。
     */
    @GetMapping("/me")
    public Result<CurrentUserResponse> me() {
        return Result.success(authService.getCurrentUser(UserRole.JUDGE));
    }

    /**
     * 查询当前评委个人资料。
     */
    @GetMapping("/profile")
    public Result<JudgeAccountVO> profile() {
        return Result.success(judgeService.getMyProfile());
    }

    /**
     * 更新当前评委可自行维护的个人资料。
     */
    @PutMapping("/profile")
    public Result<JudgeAccountVO> updateProfile(@RequestBody @Valid JudgeProfileUpdateRequest request) {
        return Result.success(judgeService.updateMyProfile(request));
    }

    /**
     * 上传当前评委头像。
     */
    @PostMapping("/profile/avatar")
    public Result<Long> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return Result.success(judgeAvatarService.uploadMyAvatar(file));
    }

    /**
     * 读取当前评委头像，文件权限仅限当前登录评委本人。
     */
    @GetMapping("/profile/avatar")
    public ResponseEntity<byte[]> avatar() {
        return FileResponseHelper.inline(judgeAvatarService.downloadMyAvatar());
    }

    /**
     * 查询当前评委参与的比赛列表。
     */
    @GetMapping("/competitions")
    public Result<List<CompetitionVO>> competitions() {
        return Result.success(judgeService.listMyCompetitions());
    }
}
