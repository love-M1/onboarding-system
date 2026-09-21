package cn.edu.nuc.onboarding.system.service;

import cn.edu.nuc.onboarding.system.dto.LoginDTO;
import cn.edu.nuc.onboarding.system.dto.RegisterDTO;
import cn.edu.nuc.onboarding.system.vo.AuthUserVO;
import cn.edu.nuc.onboarding.system.vo.LoginResultVO;
import cn.edu.nuc.onboarding.system.vo.VerificationCodeVO;

public interface AuthService {

    VerificationCodeVO requestVerificationCode(String phone);

    LoginResultVO register(RegisterDTO dto);

    LoginResultVO login(LoginDTO dto);

    AuthUserVO currentUser();

    void logout(String rawToken);
}
