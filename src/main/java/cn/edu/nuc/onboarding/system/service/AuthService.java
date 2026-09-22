package cn.edu.nuc.onboarding.system.service;

import cn.edu.nuc.onboarding.system.dto.LoginDTO;
import cn.edu.nuc.onboarding.system.dto.RegisterDTO;
import cn.edu.nuc.onboarding.system.vo.AuthUserVO;
import cn.edu.nuc.onboarding.system.vo.LoginResultVO;
import cn.edu.nuc.onboarding.system.vo.VerificationCodeVO;

import java.util.List;

public interface AuthService {

    VerificationCodeVO requestVerificationCode(String phone);

    List<String> listDepartmentOptions();

    LoginResultVO register(RegisterDTO dto);

    LoginResultVO login(LoginDTO dto);

    AuthUserVO currentUser();

    void logout(String rawToken);
}
