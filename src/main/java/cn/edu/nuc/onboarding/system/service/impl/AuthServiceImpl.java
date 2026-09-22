package cn.edu.nuc.onboarding.system.service.impl;

import cn.edu.nuc.onboarding.system.common.BizException;
import cn.edu.nuc.onboarding.system.common.ErrorCode;
import cn.edu.nuc.onboarding.system.config.AuthProperties;
import cn.edu.nuc.onboarding.system.config.DepartmentProperties;
import cn.edu.nuc.onboarding.system.dto.LoginDTO;
import cn.edu.nuc.onboarding.system.dto.RegisterDTO;
import cn.edu.nuc.onboarding.system.entity.Employee;
import cn.edu.nuc.onboarding.system.entity.PhoneVerification;
import cn.edu.nuc.onboarding.system.entity.UserAccount;
import cn.edu.nuc.onboarding.system.mapper.EmployeeMapper;
import cn.edu.nuc.onboarding.system.mapper.PhoneVerificationMapper;
import cn.edu.nuc.onboarding.system.mapper.TaskTemplateMapper;
import cn.edu.nuc.onboarding.system.mapper.UserAccountMapper;
import cn.edu.nuc.onboarding.system.security.AuthContext;
import cn.edu.nuc.onboarding.system.security.AuthUser;
import cn.edu.nuc.onboarding.system.security.SessionService;
import cn.edu.nuc.onboarding.system.service.AuthService;
import cn.edu.nuc.onboarding.system.vo.AuthUserVO;
import cn.edu.nuc.onboarding.system.vo.LoginResultVO;
import cn.edu.nuc.onboarding.system.vo.VerificationCodeVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserAccountMapper accountMapper;
    private final EmployeeMapper employeeMapper;
    private final TaskTemplateMapper templateMapper;
    private final PhoneVerificationMapper verificationMapper;
    private final AuthProperties authProperties;
    private final DepartmentProperties departmentProperties;
    private final SessionService sessionService;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(
            UserAccountMapper accountMapper,
            EmployeeMapper employeeMapper,
            TaskTemplateMapper templateMapper,
            PhoneVerificationMapper verificationMapper,
            AuthProperties authProperties,
            DepartmentProperties departmentProperties,
            SessionService sessionService,
            PasswordEncoder passwordEncoder
    ) {
        this.accountMapper = accountMapper;
        this.employeeMapper = employeeMapper;
        this.templateMapper = templateMapper;
        this.verificationMapper = verificationMapper;
        this.authProperties = authProperties;
        this.departmentProperties = departmentProperties;
        this.sessionService = sessionService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public VerificationCodeVO requestVerificationCode(String phone) {
        String safePhone = phone == null ? "" : phone.trim();
        if (!PHONE_PATTERN.matcher(safePhone).matches()) {
            throw new BizException(ErrorCode.PHONE_INVALID, "请输入正确的11位手机号");
        }
        if (accountMapper.selectByPhone(safePhone) != null) {
            throw new BizException(ErrorCode.ACCOUNT_ALREADY_EXISTS, "该手机号已注册，请直接登录");
        }

        LocalDateTime now = LocalDateTime.now();
        if (employeeMapper.countUnarchivedByPhone(safePhone) != 1) {
            throw new BizException(
                    ErrorCode.EMPLOYEE_NOT_REGISTERABLE,
                    "请先由 HR 完成员工建档，或联系管理员检查档案状态"
            );
        }
        Employee employee = employeeMapper.selectByPhone(safePhone);
        if (employee == null) {
            throw new BizException(ErrorCode.EMPLOYEE_NOT_REGISTERABLE, "员工档案不存在");
        }

        PhoneVerification latest = verificationMapper.selectLatestByPhone(safePhone);
        if (latest != null && latest.getCreateTime() != null) {
            LocalDateTime resendAt = latest.getCreateTime()
                    .plusSeconds(authProperties.getCodeResendSeconds());
            if (now.isBefore(resendAt)) {
                throw new BizException(ErrorCode.VERIFICATION_CODE_TOO_FREQUENT, "验证码发送过于频繁，请稍后再试");
            }
        }

        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        PhoneVerification verification = new PhoneVerification();
        verification.setPhone(safePhone);
        verification.setCode(code);
        verification.setExpiresAt(now.plusMinutes(authProperties.getCodeExpireMinutes()));
        verification.setAttemptCount(0);
        verification.setCreateTime(now);
        verificationMapper.insert(verification);
        verificationMapper.deleteExpired(now.minusMinutes(authProperties.getCodeExpireMinutes()));

        String devCode = authProperties.getExposeCode() ? code : null;
        return new VerificationCodeVO(
                authProperties.getCodeExpireMinutes() * 60,
                authProperties.getCodeResendSeconds(),
                devCode
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listDepartmentOptions() {
        TreeSet<String> departments = new TreeSet<>();
        addDepartments(departments, departmentProperties.getOptions());
        addDepartments(departments, employeeMapper.selectDistinctDepartments());
        addDepartments(departments, templateMapper.selectDistinctDutyDepartments());
        return List.copyOf(departments);
    }

    @Override
    @Transactional
    public LoginResultVO register(RegisterDTO dto) {
        String phone = dto == null || dto.phone() == null ? "" : dto.phone().trim();
        String code = dto == null || dto.code() == null ? "" : dto.code().trim();
        String password = dto == null || dto.password() == null ? "" : dto.password();
        String department = dto == null || dto.department() == null ? "" : dto.department().trim();

        validatePhone(phone);
        validatePassword(password);
        if (accountMapper.selectByPhone(phone) != null) {
            throw new BizException(ErrorCode.ACCOUNT_ALREADY_EXISTS, "该手机号已注册，请直接登录");
        }
        if (employeeMapper.countUnarchivedByPhone(phone) != 1) {
            throw new BizException(
                    ErrorCode.EMPLOYEE_NOT_REGISTERABLE,
                    "请先由 HR 完成员工建档，或联系管理员检查档案状态"
            );
        }
        Employee employee = employeeMapper.selectByPhone(phone);
        if (employee == null) {
            throw new BizException(ErrorCode.EMPLOYEE_NOT_REGISTERABLE, "员工档案不存在");
        }
        if (!department.isEmpty() && !department.equals(employee.getEmpDepartment())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "所选部门与员工档案不一致");
        }

        LocalDateTime now = LocalDateTime.now();
        PhoneVerification verification = verificationMapper.selectLatestActive(phone, now);
        if (verification == null) {
            throw new BizException(ErrorCode.VERIFICATION_CODE_EXPIRED, "验证码已过期，请重新获取");
        }
        if (verification.getAttemptCount() != null
                && verification.getAttemptCount() >= authProperties.getMaxAttempts()) {
            throw new BizException(
                    ErrorCode.VERIFICATION_CODE_TOO_MANY_ATTEMPTS,
                    "验证码错误次数过多，请重新获取"
            );
        }
        if (!verification.getCode().equals(code)) {
            verificationMapper.incrementAttempt(verification.getVerificationId());
            throw new BizException(ErrorCode.VERIFICATION_CODE_INVALID, "验证码不正确");
        }

        verificationMapper.markUsed(verification.getVerificationId(), now);

        UserAccount account = new UserAccount();
        account.setPhone(phone);
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setRole("EMPLOYEE");
        account.setEmpId(employee.getEmpId());
        account.setDisplayName(employee.getEmpName());
        account.setDepartment(employee.getEmpDepartment());
        account.setStatus("ACTIVE");
        account.setCreateTime(now);
        account.setUpdateTime(now);
        accountMapper.insert(account);
        return issueLoginResult(account);
    }

    @Override
    @Transactional
    public LoginResultVO login(LoginDTO dto) {
        String phone = dto == null || dto.phone() == null ? "" : dto.phone().trim();
        String password = dto == null || dto.password() == null ? "" : dto.password();
        UserAccount account = accountMapper.selectByPhone(phone);
        if (account == null || !passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new BizException(ErrorCode.LOGIN_FAILED, "手机号或密码错误");
        }
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED, "账号已停用，请联系管理员");
        }
        return issueLoginResult(account);
    }

    @Override
    public AuthUserVO currentUser() {
        AuthUser user = AuthContext.get();
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }
        return new AuthUserVO(
                user.accountId(),
                user.phone(),
                user.operator(),
                user.role(),
                user.empId(),
                user.department()
        );
    }

    @Override
    public void logout(String rawToken) {
        sessionService.revoke(rawToken);
    }

    private LoginResultVO issueLoginResult(UserAccount account) {
        SessionService.IssuedSession session = sessionService.issue(account);
        return new LoginResultVO(session.token(), session.expiresAt(), toUserVO(account));
    }

    private AuthUserVO toUserVO(UserAccount account) {
        return new AuthUserVO(
                account.getAccountId(),
                account.getPhone(),
                account.getDisplayName(),
                account.getRole(),
                account.getEmpId(),
                account.getDepartment()
        );
    }

    private void validatePhone(String phone) {
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BizException(ErrorCode.PHONE_INVALID, "请输入正确的11位手机号");
        }
    }

    private void validatePassword(String password) {
        if (password.length() < 8
                || password.length() > 32
                || !password.matches(".*[A-Za-z].*")
                || !password.matches(".*\\d.*")) {
            throw new BizException(ErrorCode.PASSWORD_INVALID, "密码需为8至32位且同时包含字母和数字");
        }
    }

    private void addDepartments(TreeSet<String> departments, List<String> candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                departments.add(candidate.trim());
            }
        }
    }
}
