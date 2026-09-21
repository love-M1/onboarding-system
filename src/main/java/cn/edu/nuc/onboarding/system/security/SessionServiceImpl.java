package cn.edu.nuc.onboarding.system.security;

import cn.edu.nuc.onboarding.system.common.BizException;
import cn.edu.nuc.onboarding.system.common.ErrorCode;
import cn.edu.nuc.onboarding.system.config.AuthProperties;
import cn.edu.nuc.onboarding.system.entity.AuthSession;
import cn.edu.nuc.onboarding.system.entity.UserAccount;
import cn.edu.nuc.onboarding.system.mapper.AuthSessionMapper;
import cn.edu.nuc.onboarding.system.mapper.UserAccountMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class SessionServiceImpl implements SessionService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthSessionMapper sessionMapper;
    private final UserAccountMapper accountMapper;
    private final AuthProperties authProperties;

    public SessionServiceImpl(
            AuthSessionMapper sessionMapper,
            UserAccountMapper accountMapper,
            AuthProperties authProperties
    ) {
        this.sessionMapper = sessionMapper;
        this.accountMapper = accountMapper;
        this.authProperties = authProperties;
    }

    @Override
    @Transactional
    public IssuedSession issue(UserAccount account) {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(authProperties.getSessionHours());
        AuthSession session = new AuthSession();
        session.setTokenHash(hashToken(rawToken));
        session.setAccountId(account.getAccountId());
        session.setExpiresAt(expiresAt);
        session.setCreateTime(now);
        session.setLastAccessTime(now);
        sessionMapper.insert(session);
        return new IssuedSession(rawToken, expiresAt);
    }

    @Override
    @Transactional
    public UserAccount authenticate(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            throw unauthorized();
        }
        LocalDateTime now = LocalDateTime.now();
        AuthSession session = sessionMapper.selectValidByTokenHash(hashToken(rawToken), now);
        if (session == null) {
            throw unauthorized();
        }
        UserAccount account = accountMapper.selectById(session.getAccountId());
        if (account == null || !"ACTIVE".equals(account.getStatus())) {
            throw unauthorized();
        }
        sessionMapper.touch(session.getSessionId(), now);
        return account;
    }

    @Override
    @Transactional
    public void revoke(String rawToken) {
        if (StringUtils.hasText(rawToken)) {
            sessionMapper.deleteByTokenHash(hashToken(rawToken));
        }
    }

    private String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private BizException unauthorized() {
        return new BizException(ErrorCode.UNAUTHORIZED, "登录状态已失效，请重新登录");
    }
}
