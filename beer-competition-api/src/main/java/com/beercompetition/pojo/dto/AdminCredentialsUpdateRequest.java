package com.beercompetition.pojo.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 当前管理员修改登录账号和密码的请求。
 *
 * <p>首次设置时不要求再次输入初始密码；普通设置时由业务层校验当前密码。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCredentialsUpdateRequest {

    @Size(max = 64, message = "登录账号不能超过64个字符")
    private String username;

    private String oldPassword;

    @Size(max = 32, message = "密码长度不能超过32位")
    private String newPassword;
}
