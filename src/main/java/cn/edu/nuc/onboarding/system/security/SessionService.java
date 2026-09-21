package cn.edu.nuc.onboarding.system.security;

import cn.edu.nuc.onboarding.system.entity.UserAccount;

import java.time.LocalDateTime;

public interface SessionService {

    IssuedSession issue(UserAccount account);

    UserAccount authenticate(String rawToken);

    void revoke(String rawToken);

    record IssuedSession(String token, LocalDateTime expiresAt) {
    }
}
