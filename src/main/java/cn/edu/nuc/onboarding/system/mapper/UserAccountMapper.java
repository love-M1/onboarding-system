package cn.edu.nuc.onboarding.system.mapper;

import cn.edu.nuc.onboarding.system.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserAccountMapper {

    int insert(UserAccount account);

    UserAccount selectById(@Param("accountId") Integer accountId);

    UserAccount selectByPhone(@Param("phone") String phone);

    List<UserAccount> selectDepartmentOwners();

    long countEnabledDepartmentOwner(
            @Param("department") String department,
            @Param("excludeAccountId") Integer excludeAccountId
    );

    int updateDepartmentOwner(UserAccount account);

    int updatePassword(
            @Param("accountId") Integer accountId,
            @Param("passwordHash") String passwordHash,
            @Param("updateTime") LocalDateTime updateTime
    );
}
