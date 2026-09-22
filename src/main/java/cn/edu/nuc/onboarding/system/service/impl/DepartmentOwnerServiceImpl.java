package cn.edu.nuc.onboarding.system.service.impl;

import cn.edu.nuc.onboarding.system.common.BizException;
import cn.edu.nuc.onboarding.system.common.ErrorCode;
import cn.edu.nuc.onboarding.system.dto.DepartmentOwnerSaveDTO;
import cn.edu.nuc.onboarding.system.dto.DepartmentOwnerUpdateDTO;
import cn.edu.nuc.onboarding.system.dto.PasswordResetDTO;
import cn.edu.nuc.onboarding.system.entity.UserAccount;
import cn.edu.nuc.onboarding.system.mapper.UserAccountMapper;
import cn.edu.nuc.onboarding.system.service.DepartmentOwnerService;
import cn.edu.nuc.onboarding.system.vo.DepartmentOwnerVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class DepartmentOwnerServiceImpl implements DepartmentOwnerService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";

    private final UserAccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;

    public DepartmentOwnerServiceImpl(
            UserAccountMapper accountMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.accountMapper = accountMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentOwnerVO> listDepartmentOwners() {
        return accountMapper.selectDepartmentOwners().stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional
    public DepartmentOwnerVO createDepartmentOwner(DepartmentOwnerSaveDTO dto) {
        if (dto == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请求参数缺失");
        }
        String phone = validatePhone(dto.phone());
        validatePassword(dto.password());
        String displayName = requireText(dto.displayName(), "责任人姓名不能为空");
        String department = requireText(dto.department(), "所属部门不能为空");
        ensurePhoneAvailable(phone, null);
        ensureDepartmentAvailable(department, null);

        LocalDateTime now = LocalDateTime.now();
        UserAccount account = new UserAccount();
        account.setPhone(phone);
        account.setPasswordHash(passwordEncoder.encode(dto.password()));
        account.setRole("DEPARTMENT");
        account.setDisplayName(displayName);
        account.setDepartment(department);
        account.setStatus(STATUS_ACTIVE);
        account.setCreateTime(now);
        account.setUpdateTime(now);
        accountMapper.insert(account);
        return toVO(account);
    }

    @Override
    @Transactional
    public DepartmentOwnerVO updateDepartmentOwner(
            Integer accountId,
            DepartmentOwnerUpdateDTO dto
    ) {
        UserAccount account = requireDepartmentOwner(accountId);
        if (dto == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请求参数缺失");
        }
        String phone = validatePhone(dto.phone());
        String displayName = requireText(dto.displayName(), "责任人姓名不能为空");
        String department = requireText(dto.department(), "所属部门不能为空");
        String status = normalizeStatus(dto.status());
        ensurePhoneAvailable(phone, accountId);
        if (STATUS_ACTIVE.equals(status)) {
            ensureDepartmentAvailable(department, accountId);
        }

        account.setPhone(phone);
        account.setDisplayName(displayName);
        account.setDepartment(department);
        account.setStatus(status);
        account.setUpdateTime(LocalDateTime.now());
        int updated = accountMapper.updateDepartmentOwner(account);
        if (updated == 0) {
            throw new BizException(ErrorCode.DEPARTMENT_OWNER_NOT_FOUND, "部门责任人不存在");
        }
        return toVO(account);
    }

    @Override
    @Transactional
    public void disableDepartmentOwner(Integer accountId) {
        UserAccount account = requireDepartmentOwner(accountId);
        account.setStatus(STATUS_DISABLED);
        account.setUpdateTime(LocalDateTime.now());
        int updated = accountMapper.updateDepartmentOwner(account);
        if (updated == 0) {
            throw new BizException(ErrorCode.DEPARTMENT_OWNER_NOT_FOUND, "部门责任人不存在");
        }
    }

    @Override
    @Transactional
    public void resetPassword(Integer accountId, PasswordResetDTO dto) {
        requireDepartmentOwner(accountId);
        validatePassword(dto == null ? null : dto.newPassword());
        int updated = accountMapper.updatePassword(
                accountId,
                passwordEncoder.encode(dto.newPassword()),
                LocalDateTime.now()
        );
        if (updated == 0) {
            throw new BizException(ErrorCode.DEPARTMENT_OWNER_NOT_FOUND, "部门责任人不存在");
        }
    }

    private UserAccount requireDepartmentOwner(Integer accountId) {
        if (accountId == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "账号编号不能为空");
        }
        UserAccount account = accountMapper.selectById(accountId);
        if (account == null || !"DEPARTMENT".equals(account.getRole())) {
            throw new BizException(ErrorCode.DEPARTMENT_OWNER_NOT_FOUND, "部门责任人不存在");
        }
        return account;
    }

    private void ensurePhoneAvailable(String phone, Integer excludeAccountId) {
        UserAccount existing = accountMapper.selectByPhone(phone);
        if (existing != null && !existing.getAccountId().equals(excludeAccountId)) {
            throw new BizException(ErrorCode.ACCOUNT_ALREADY_EXISTS, "该手机号已注册");
        }
    }

    private void ensureDepartmentAvailable(String department, Integer excludeAccountId) {
        if (accountMapper.countEnabledDepartmentOwner(department, excludeAccountId) > 0) {
            throw new BizException(ErrorCode.DEPARTMENT_OWNER_EXISTS, "该部门已启用部门责任人");
        }
    }

    private String validatePhone(String value) {
        String phone = value == null ? "" : value.trim();
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BizException(ErrorCode.PHONE_INVALID, "请输入正确的11位手机号");
        }
        return phone;
    }

    private void validatePassword(String password) {
        if (password == null
                || password.length() < 8
                || password.length() > 32
                || !password.matches(".*[A-Za-z].*")
                || !password.matches(".*\\d.*")) {
            throw new BizException(ErrorCode.PASSWORD_INVALID, "密码需为8至32位且同时包含字母和数字");
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BizException(ErrorCode.DEPARTMENT_OWNER_STATUS_INVALID, "账号状态不能为空");
        }
        String normalized = status.trim().toUpperCase();
        if (!STATUS_ACTIVE.equals(normalized) && !STATUS_DISABLED.equals(normalized)) {
            throw new BizException(ErrorCode.DEPARTMENT_OWNER_STATUS_INVALID, "账号状态不正确");
        }
        return normalized;
    }

    private DepartmentOwnerVO toVO(UserAccount account) {
        return new DepartmentOwnerVO(
                account.getAccountId(),
                account.getPhone(),
                account.getDisplayName(),
                account.getDepartment(),
                account.getStatus(),
                account.getCreateTime(),
                account.getUpdateTime()
        );
    }
}
