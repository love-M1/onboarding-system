package cn.edu.nuc.onboarding.system.vo;

import java.time.LocalDateTime;

public record LoginResultVO(String token, LocalDateTime expiresAt, AuthUserVO user) {
}
