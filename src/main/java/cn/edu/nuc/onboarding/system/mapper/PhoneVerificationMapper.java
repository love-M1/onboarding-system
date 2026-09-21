package cn.edu.nuc.onboarding.system.mapper;

import cn.edu.nuc.onboarding.system.entity.PhoneVerification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface PhoneVerificationMapper {

    int insert(PhoneVerification verification);

    PhoneVerification selectLatestActive(
            @Param("phone") String phone,
            @Param("now") LocalDateTime now
    );

    PhoneVerification selectLatestByPhone(@Param("phone") String phone);

    int markUsed(
            @Param("verificationId") Integer verificationId,
            @Param("usedAt") LocalDateTime usedAt
    );

    int incrementAttempt(@Param("verificationId") Integer verificationId);

    int deleteExpired(@Param("now") LocalDateTime now);
}
