package cn.edu.nuc.onboarding.system.controller;

import cn.edu.nuc.onboarding.system.common.ApiResponse;
import cn.edu.nuc.onboarding.system.dto.LoginDTO;
import cn.edu.nuc.onboarding.system.dto.RegisterDTO;
import cn.edu.nuc.onboarding.system.dto.VerificationCodeRequestDTO;
import cn.edu.nuc.onboarding.system.service.AuthService;
import cn.edu.nuc.onboarding.system.vo.AuthUserVO;
import cn.edu.nuc.onboarding.system.vo.LoginResultVO;
import cn.edu.nuc.onboarding.system.vo.VerificationCodeVO;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/verification-codes")
    public ApiResponse<VerificationCodeVO> requestVerificationCode(
            @RequestBody VerificationCodeRequestDTO dto
    ) {
        return ApiResponse.success("验证码已发送", authService.requestVerificationCode(dto.phone()));
    }

    @GetMapping("/departments")
    public ApiResponse<List<String>> departments() {
        return ApiResponse.success(authService.listDepartmentOptions());
    }

    @PostMapping("/register")
    public ApiResponse<LoginResultVO> register(@RequestBody RegisterDTO dto) {
        return ApiResponse.success("注册成功", authService.register(dto));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResultVO> login(@RequestBody LoginDTO dto) {
        return ApiResponse.success("登录成功", authService.login(dto));
    }

    @GetMapping("/me")
    public ApiResponse<AuthUserVO> me() {
        return ApiResponse.success(authService.currentUser());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new cn.edu.nuc.onboarding.system.common.BizException(
                    cn.edu.nuc.onboarding.system.common.ErrorCode.UNAUTHORIZED,
                    "登录状态已失效，请重新登录"
            );
        }
        authService.logout(authorization.substring("Bearer ".length()).trim());
        return ApiResponse.success("已退出登录", null);
    }
}
