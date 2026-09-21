package cn.edu.nuc.onboarding.system.mapper;

import cn.edu.nuc.onboarding.system.entity.AuthSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface AuthSessionMapper {

    int insert(AuthSession session);

    AuthSession selectValidByTokenHash(
            @Param("tokenHash") String tokenHash,
            @Param("now") LocalDateTime now
    );

    int touch(
            @Param("sessionId") Integer sessionId,
            @Param("lastAccessTime") LocalDateTime lastAccessTime
    );

    int deleteByTokenHash(@Param("tokenHash") String tokenHash);
}
