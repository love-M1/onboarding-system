package cn.edu.nuc.onboarding.system.config;

import cn.edu.nuc.onboarding.system.entity.UserAccount;
import cn.edu.nuc.onboarding.system.mapper.UserAccountMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AuthBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AuthBootstrapRunner.class);

    private final UserAccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    public AuthBootstrapRunner(
            UserAccountMapper accountMapper,
            PasswordEncoder passwordEncoder,
            AuthProperties authProperties
    ) {
        this.accountMapper = accountMapper;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String phone = authProperties.getBootstrapHrPhone();
        if (accountMapper.selectByPhone(phone) != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        UserAccount account = new UserAccount();
        account.setPhone(phone);
        account.setPasswordHash(passwordEncoder.encode(authProperties.getBootstrapHrPassword()));
        account.setRole("HR");
        account.setDisplayName(authProperties.getBootstrapHrName());
        account.setDepartment("人事部");
        account.setStatus("ACTIVE");
        account.setCreateTime(now);
        account.setUpdateTime(now);
        accountMapper.insert(account);
        log.info("Bootstrap HR account created: phone={}", phone);
    }
}
