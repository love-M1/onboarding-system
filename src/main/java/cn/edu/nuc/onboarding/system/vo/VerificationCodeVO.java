package cn.edu.nuc.onboarding.system.vo;

public record VerificationCodeVO(int expiresIn, int resendAfter, String devCode) {
}
