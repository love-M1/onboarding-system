package cn.edu.nuc.onboarding.system.mapper;

import cn.edu.nuc.onboarding.system.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserAccountMapper {

    int insert(UserAccount account);

    UserAccount selectById(@Param("accountId") Integer accountId);

    UserAccount selectByPhone(@Param("phone") String phone);

    int updatePassword(@Param("accountId") Integer accountId, @Param("passwordHash") String passwordHash);

    int updateStatus(@Param("accountId") Integer accountId, @Param("status") String status);
}
