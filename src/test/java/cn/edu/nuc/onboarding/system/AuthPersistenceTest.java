package cn.edu.nuc.onboarding.system;

import cn.edu.nuc.onboarding.system.entity.AuthSession;
import cn.edu.nuc.onboarding.system.entity.PhoneVerification;
import cn.edu.nuc.onboarding.system.entity.UserAccount;
import cn.edu.nuc.onboarding.system.mapper.AuthSessionMapper;
import cn.edu.nuc.onboarding.system.mapper.PhoneVerificationMapper;
import cn.edu.nuc.onboarding.system.mapper.UserAccountMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthPersistenceTest {

    @Autowired
    private UserAccountMapper accountMapper;

    @Autowired
    private PhoneVerificationMapper verificationMapper;

    @Autowired
    private AuthSessionMapper sessionMapper;

    @Test
    void persistsAndReadsAuthenticationRecords() {
        UserAccount account = new UserAccount();
        account.setPhone("13800000001");
        account.setPasswordHash("$2a$10$test");
        account.setRole("EMPLOYEE");
        account.setDisplayName("张三");
        account.setDepartment("研发部");
        account.setStatus("ACTIVE");
        account.setCreateTime(LocalDateTime.now());
        account.setUpdateTime(LocalDateTime.now());
        accountMapper.insert(account);

        assertThat(accountMapper.selectByPhone("13800000001").getAccountId())
                .isEqualTo(account.getAccountId());

        PhoneVerification verification = new PhoneVerification();
        verification.setPhone("13800000001");
        verification.setCode("123456");
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        verification.setAttemptCount(0);
        verification.setCreateTime(LocalDateTime.now());
        verificationMapper.insert(verification);

        assertThat(verificationMapper.selectLatestActive(
                "13800000001", LocalDateTime.now()).getCode()).isEqualTo("123456");

        AuthSession session = new AuthSession();
        session.setTokenHash("hash-1");
        session.setAccountId(account.getAccountId());
        session.setExpiresAt(LocalDateTime.now().plusHours(8));
        session.setCreateTime(LocalDateTime.now());
        session.setLastAccessTime(LocalDateTime.now());
        sessionMapper.insert(session);

        assertThat(sessionMapper.selectValidByTokenHash(
                "hash-1", LocalDateTime.now()).getAccountId()).isEqualTo(account.getAccountId());
    }
}
