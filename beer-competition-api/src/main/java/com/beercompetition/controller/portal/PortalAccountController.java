package com.beercompetition.controller.portal;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.dto.PortalProfileUpdateRequest;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.vo.CurrentUserResponse;
import com.beercompetition.pojo.vo.PortalProfileVO;
import com.beercompetition.service.AuthService;
import com.beercompetition.service.EntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 厂商账号接口，提供当前登录态、资料维护和头像上传。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portal")
public class PortalAccountController {

    private final AuthService authService;
    private final EntryService entryService;

    /**
     * 获取当前厂商登录身份信息。
     */
    @GetMapping("/me")
    public Result<CurrentUserResponse> me() {
        return Result.success(authService.getCurrentUser(UserRole.PORTAL));
    }

    /**
     * 查询当前厂商资料。
     */
    @GetMapping("/profile")
    public Result<PortalProfileVO> profile() {
        return Result.success(entryService.getPortalProfile());
    }

    /**
     * 更新当前厂商资料。
     */
    @PutMapping("/profile")
    public Result<PortalProfileVO> updateProfile(@RequestBody @Valid PortalProfileUpdateRequest request) {
        return Result.success(entryService.updatePortalProfile(request));
    }

    /**
     * 上传当前厂商头像。
     */
    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<PortalProfileVO> uploadProfileAvatar(@RequestParam("file") MultipartFile file) {
        return Result.success(entryService.uploadPortalAvatar(file));
    }
}
