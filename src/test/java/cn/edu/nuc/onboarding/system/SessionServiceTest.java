package cn.edu.nuc.onboarding.system;

import cn.edu.nuc.onboarding.system.common.BizException;
import cn.edu.nuc.onboarding.system.entity.UserAccount;
import cn.edu.nuc.onboarding.system.mapper.UserAccountMapper;
import cn.edu.nuc.onboarding.system.security.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SessionServiceTest {

    @Autowired
    private UserAccountMapper accountMapper;

    @Autowired
    private SessionService sessionService;

    @Test
    void issuesResolvesAndRevokesOpaqueToken() {
        UserAccount account = createAccount("13800000002");
        var issued = sessionService.issue(account);

        assertThat(issued.token()).isNotBlank();
        assertThat(sessionService.authenticate(issued.token()).getAccountId())
                .isEqualTo(account.getAccountId());

        sessionService.revoke(issued.token());
        assertThatThrownBy(() -> sessionService.authenticate(issued.token()))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(401);
    }

    private UserAccount createAccount(String phone) {
        UserAccount account = new UserAccount();
        account.setPhone(phone);
        account.setPasswordHash("hash");
        account.setRole("EMPLOYEE");
        account.setDisplayName("测试员工");
        account.setDepartment("研发部");
        account.setStatus("ACTIVE");
        account.setCreateTime(LocalDateTime.now());
        account.setUpdateTime(LocalDateTime.now());
        accountMapper.insert(account);
        return account;
    }
}
