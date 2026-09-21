# Employee Authentication Closed Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add verified phone registration, password login, token authorization, and employee-owned task confirmation so HR can onboard an employee and that employee can complete the generated tasks from their own account.

**Architecture:** Add an opaque Bearer-token authentication subsystem backed by three tables. Public registration binds an account to an existing HR-created employee record; the session interceptor resolves the token into `AuthContext`, and task services enforce employee ownership.

**Tech Stack:** Java 21, Spring Boot 3.2, MyBatis 3, MySQL 8/H2, BCrypt from `spring-security-crypto`, Vue 3, Pinia, Axios, Element Plus.

**Spec:** `docs/superpowers/specs/2026-09-21-employee-auth-closed-loop-design.md`

## Global Constraints

- Phone number format is exactly `1[3-9]` followed by 9 digits.
- Employee phone is required and unique.
- Password length is 8 to 32 characters and must contain at least one letter and one digit.
- Passwords are stored only as BCrypt hashes.
- Verification codes are 6 digits, expire after 5 minutes, cannot be resent within 60 seconds, and allow at most 5 failed checks.
- Sessions expire after 8 hours.
- `AUTH_EXPOSE_CODE` defaults to `true` for the course demo and must be `false` in production.
- Bootstrap HR defaults are phone `13800000000`, password `Admin@123`, name `人事管理员`; environment variables must be able to override all three.
- Public registration creates only `EMPLOYEE` accounts bound to a non-archived employee record.
- `EMPLOYEE` can list and finish only tasks whose `empId` matches the account binding.
- `HR` can list all tasks but cannot finish tasks.
- `DEPARTMENT` remains supported by backend authorization for compatibility but has no registration UI.
- `X-Role`, `X-Department`, and `X-Operator` must not authorize any request after this change.
- The current directory is not a Git repository. Do not initialize one; use passing tests and focused diff review as task checkpoints.
- Keep existing `ApiResponse` response wrapping and MyBatis XML patterns.

---

### Task 1: Authentication Schema and Persistence

**Files:**

- Modify: `pom.xml`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/common/ErrorCode.java`
- Modify: `sql/onboarding_sys.sql`
- Modify: `src/test/resources/schema-test.sql`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/entity/UserAccount.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/entity/PhoneVerification.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/entity/AuthSession.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/mapper/UserAccountMapper.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/mapper/PhoneVerificationMapper.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/mapper/AuthSessionMapper.java`
- Create: `src/main/resources/mapper/UserAccountMapper.xml`
- Create: `src/main/resources/mapper/PhoneVerificationMapper.xml`
- Create: `src/main/resources/mapper/AuthSessionMapper.xml`
- Create: `src/test/java/cn/edu/nuc/onboarding/system/AuthPersistenceTest.java`

**Interfaces:**

- Consumes: existing MyBatis and H2 test configuration.
- Produces:
  - `UserAccount` getters/setters for `accountId`, `phone`, `passwordHash`, `role`, `empId`, `displayName`, `department`, `status`, `createTime`, `updateTime`.
  - `PhoneVerification` getters/setters for `verificationId`, `phone`, `code`, `expiresAt`, `usedAt`, `attemptCount`, `createTime`.
  - `AuthSession` getters/setters for `sessionId`, `tokenHash`, `accountId`, `expiresAt`, `createTime`, `lastAccessTime`.
  - `UserAccountMapper.insert(UserAccount)`, `selectById(Integer)`, `selectByPhone(String)`, `updatePassword(Integer, String)`, `updateStatus(Integer, String)`.
  - `PhoneVerificationMapper.insert(PhoneVerification)`, `selectLatestActive(String, LocalDateTime)`, `markUsed(Integer, LocalDateTime)`, `incrementAttempt(Integer)`, `deleteExpired(LocalDateTime)`.
  - `AuthSessionMapper.insert(AuthSession)`, `selectValidByTokenHash(String, LocalDateTime)`, `touch(Integer, LocalDateTime)`, `deleteByTokenHash(String)`.

- [ ] **Step 1: Add the failing persistence test**

Create `AuthPersistenceTest.java`:

```java
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
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
mvn -Dtest=AuthPersistenceTest test
```

Expected: compilation failure because the three entity and mapper types do not exist.

- [ ] **Step 3: Add the BCrypt dependency and error codes**

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

Append these constants to `ErrorCode`:

```java
public static final int UNAUTHORIZED = 401;

public static final int EMPLOYEE_PHONE_REQUIRED = 1006;
public static final int EMPLOYEE_PHONE_INVALID = 1007;
public static final int EMPLOYEE_PHONE_DUPLICATE = 1008;

public static final int PHONE_INVALID = 5001;
public static final int EMPLOYEE_NOT_REGISTERABLE = 5002;
public static final int ACCOUNT_ALREADY_EXISTS = 5003;
public static final int VERIFICATION_CODE_INVALID = 5004;
public static final int VERIFICATION_CODE_EXPIRED = 5005;
public static final int VERIFICATION_CODE_USED = 5006;
public static final int VERIFICATION_CODE_TOO_MANY_ATTEMPTS = 5007;
public static final int VERIFICATION_CODE_TOO_FREQUENT = 5008;
public static final int PASSWORD_INVALID = 5009;
public static final int LOGIN_FAILED = 5010;
public static final int ACCOUNT_DISABLED = 5011;
```

- [ ] **Step 4: Add the MySQL and H2 schema**

In `sql/onboarding_sys.sql`, drop auth tables before dropping business tables:

```sql
DROP TABLE IF EXISTS auth_session;
DROP TABLE IF EXISTS phone_verification;
DROP TABLE IF EXISTS user_account;
```

After creating `employee`, add:

```sql
CREATE TABLE user_account (
    accountId INT PRIMARY KEY AUTO_INCREMENT COMMENT '账号编号',
    phone VARCHAR(20) NOT NULL COMMENT '手机号（工号）',
    passwordHash VARCHAR(100) NOT NULL COMMENT 'BCrypt密码哈希',
    role VARCHAR(20) NOT NULL COMMENT 'HR、EMPLOYEE或DEPARTMENT',
    empId INT DEFAULT NULL COMMENT '绑定的员工档案编号',
    displayName VARCHAR(50) NOT NULL COMMENT '显示名称',
    department VARCHAR(50) DEFAULT NULL COMMENT '账号所属部门',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE或DISABLED',
    createTime DATETIME NOT NULL,
    updateTime DATETIME NOT NULL,
    UNIQUE KEY uk_user_account_phone (phone),
    UNIQUE KEY uk_user_account_emp (empId),
    CONSTRAINT fk_user_account_employee
        FOREIGN KEY (empId) REFERENCES employee(empId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统账号表';

CREATE TABLE phone_verification (
    verificationId INT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(20) NOT NULL,
    code VARCHAR(6) NOT NULL,
    expiresAt DATETIME NOT NULL,
    usedAt DATETIME DEFAULT NULL,
    attemptCount INT NOT NULL DEFAULT 0,
    createTime DATETIME NOT NULL,
    INDEX idx_phone_verification_phone_time (phone, createTime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手机验证码表';

CREATE TABLE auth_session (
    sessionId INT PRIMARY KEY AUTO_INCREMENT,
    tokenHash VARCHAR(64) NOT NULL,
    accountId INT NOT NULL,
    expiresAt DATETIME NOT NULL,
    createTime DATETIME NOT NULL,
    lastAccessTime DATETIME NOT NULL,
    UNIQUE KEY uk_auth_session_token_hash (tokenHash),
    INDEX idx_auth_session_account (accountId),
    CONSTRAINT fk_auth_session_account
        FOREIGN KEY (accountId) REFERENCES user_account(accountId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录会话表';
```

Apply equivalent H2-compatible DDL to `src/test/resources/schema-test.sql`. Drop auth tables before employee tables and add `UNIQUE(empPhone)` to the employee table.

- [ ] **Step 5: Add entity classes and mapper interfaces**

Create the three Java entities with fields exactly matching the mapper result columns and ordinary getters/setters. Create mapper interfaces with the signatures listed in **Interfaces**.

- [ ] **Step 6: Add mapper XML**

Implement `UserAccountMapper.xml` with `useGeneratedKeys="true" keyProperty="accountId"`, exact column mappings, and these conditions:

```xml
<select id="selectByPhone" resultType="cn.edu.nuc.onboarding.system.entity.UserAccount">
    SELECT accountId, phone, passwordHash, role, empId, displayName, department,
           status, createTime, updateTime
    FROM user_account
    WHERE phone = #{phone}
</select>
```

Implement `PhoneVerificationMapper.xml` with:

```xml
<select id="selectLatestActive" resultType="cn.edu.nuc.onboarding.system.entity.PhoneVerification">
    SELECT verificationId, phone, code, expiresAt, usedAt, attemptCount, createTime
    FROM phone_verification
    WHERE phone = #{phone}
      AND usedAt IS NULL
      AND expiresAt &gt; #{now}
    ORDER BY verificationId DESC
    LIMIT 1
</select>
```

Implement `AuthSessionMapper.xml` with:

```xml
<select id="selectValidByTokenHash" resultType="cn.edu.nuc.onboarding.system.entity.AuthSession">
    SELECT sessionId, tokenHash, accountId, expiresAt, createTime, lastAccessTime
    FROM auth_session
    WHERE tokenHash = #{tokenHash}
      AND expiresAt &gt; #{now}
</select>
```

- [ ] **Step 7: Run persistence tests**

Run:

```powershell
mvn -Dtest=AuthPersistenceTest test
```

Expected: `BUILD SUCCESS`.

---

### Task 2: Password Hashing, Authentication Properties, and Session Service

**Files:**

- Create: `src/main/java/cn/edu/nuc/onboarding/system/config/AuthProperties.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/config/SecurityBeansConfig.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/security/SessionService.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/security/SessionServiceImpl.java`
- Create: `src/test/java/cn/edu/nuc/onboarding/system/SessionServiceTest.java`
- Modify: `src/main/resources/application.yml`
- Modify: `src/test/resources/application-test.yml`

**Interfaces:**

- Consumes: `UserAccountMapper`, `AuthSessionMapper`.
- Produces:
  - `AuthProperties.getExposeCode()`, `getCodeExpireMinutes()`, `getCodeResendSeconds()`, `getMaxAttempts()`, `getSessionHours()`, `getBootstrapHrPhone()`, `getBootstrapHrPassword()`, `getBootstrapHrName()`.
  - `SessionServiceImpl.issue(UserAccount account): IssuedSession`.
  - `SessionServiceImpl.authenticate(String rawToken): UserAccount`.
  - `SessionServiceImpl.revoke(String rawToken): void`.
  - `IssuedSession(String token, LocalDateTime expiresAt)`.

- [ ] **Step 1: Write failing session tests**

Create `SessionServiceTest.java`:

```java
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
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
mvn -Dtest=SessionServiceTest test
```

Expected: compilation failure because `SessionService` and `AuthProperties` do not exist.

- [ ] **Step 3: Add configuration properties**

Add to both application YAML files:

```yaml
app:
  auth:
    expose-code: ${AUTH_EXPOSE_CODE:true}
    code-expire-minutes: 5
    code-resend-seconds: 60
    max-attempts: 5
    session-hours: 8
    bootstrap-hr-phone: ${AUTH_HR_PHONE:13800000000}
    bootstrap-hr-password: ${AUTH_HR_PASSWORD:Admin@123}
    bootstrap-hr-name: ${AUTH_HR_NAME:人事管理员}
```

Implement `AuthProperties` as a `@Component` with `@ConfigurationProperties(prefix = "app.auth")` and the exact getters/setters needed by these fields.

- [ ] **Step 4: Add BCrypt bean**

Create `SecurityBeansConfig`:

```java
@Configuration
public class SecurityBeansConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 5: Implement session service**

Use `SecureRandom` to generate 32 random bytes, encode with URL-safe Base64 without padding, and store a lowercase 64-character SHA-256 hex hash. `issue` inserts an `AuthSession`, `authenticate` loads the token hash and associated active account, updates `lastAccessTime`, and `revoke` deletes by token hash.

The invalid-token exception must be:

```java
throw new BizException(ErrorCode.UNAUTHORIZED, "登录状态已失效，请重新登录");
```

- [ ] **Step 6: Run session tests**

Run:

```powershell
mvn -Dtest=SessionServiceTest test
```

Expected: `BUILD SUCCESS`.

---

### Task 3: Verification Code Request Flow

**Files:**

- Modify: `src/main/java/cn/edu/nuc/onboarding/system/mapper/EmployeeMapper.java`
- Modify: `src/main/resources/mapper/EmployeeMapper.xml`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/dto/VerificationCodeRequestDTO.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/vo/VerificationCodeVO.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/service/AuthService.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/service/impl/AuthServiceImpl.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/controller/AuthController.java`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/config/WebConfig.java`
- Create: `src/test/java/cn/edu/nuc/onboarding/system/VerificationCodeFlowTest.java`

**Interfaces:**

- Consumes: `EmployeeMapper.selectByPhone`, `PhoneVerificationMapper`, `AuthProperties`.
- Produces:
  - `AuthService.requestVerificationCode(String phone): VerificationCodeVO`.
  - `VerificationCodeVO(int expiresIn, int resendAfter, String devCode)`.
  - Public endpoint `POST /api/auth/verification-codes`.

- [ ] **Step 1: Write failing MockMvc tests**

Create `VerificationCodeFlowTest.java` with two tests:

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VerificationCodeFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void unknownPhoneCannotRequestCode() throws Exception {
        mockMvc.perform(post("/api/auth/verification-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13800000009"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(5002));
    }

    @Test
    void registeredEmployeeReceivesSixDigitCode() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO employee(empName, empPhone, empDepartment, empPosition,
                                     entryTime, isArchived, createTime)
                VALUES ('张三', '13800000001', '研发部', '工程师',
                        CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP)
                """);

        mockMvc.perform(post("/api/auth/verification-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13800000001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.devCode").value(org.hamcrest.Matchers.matchesPattern("\\d{6}")));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
mvn -Dtest=VerificationCodeFlowTest test
```

Expected: `POST /api/auth/verification-codes` returns 404 or the required types are missing.

- [ ] **Step 3: Add employee lookup and DTO/VO**

Add to `EmployeeMapper`:

```java
Employee selectByPhone(@Param("phone") String phone);

long countUnarchivedByPhone(@Param("phone") String phone);
```

Implement both in `EmployeeMapper.xml`. `selectByPhone` returns `NULL` when more than one row exists by using `ORDER BY empId DESC LIMIT 1` only after `countUnarchivedByPhone` proves uniqueness.

Create:

```java
public record VerificationCodeRequestDTO(String phone) {
}

public record VerificationCodeVO(int expiresIn, int resendAfter, String devCode) {
}
```

- [ ] **Step 4: Implement requestVerificationCode**

Validate the phone with `^1[3-9]\\d{9}$`. Reject an existing account with `ACCOUNT_ALREADY_EXISTS`. Require exactly one unarchived employee record for the phone; otherwise throw `EMPLOYEE_NOT_REGISTERABLE` with `请先由 HR 完成员工建档`.

Enforce the 60-second resend cooldown using the latest verification row. Generate a code with `SecureRandom.nextInt(1_000_000)` formatted as six digits and insert the verification row. Set `devCode` only when `AuthProperties.getExposeCode()` is true.

- [ ] **Step 5: Add the public controller method**

Create `AuthController` with:

```java
@PostMapping("/verification-codes")
public ApiResponse<VerificationCodeVO> requestVerificationCode(
        @RequestBody VerificationCodeRequestDTO dto) {
    return ApiResponse.success("验证码已发送", authService.requestVerificationCode(dto.phone()));
}
```

Exclude only `/api/auth/verification-codes`, `/api/auth/register`, and `/api/auth/login` from `RoleInterceptor`.

- [ ] **Step 6: Run verification tests**

Run:

```powershell
mvn -Dtest=VerificationCodeFlowTest test
```

Expected: `BUILD SUCCESS`.

---

### Task 4: Registration, Login, Current User, Logout, and HR Bootstrap

**Files:**

- Create: `src/main/java/cn/edu/nuc/onboarding/system/dto/RegisterDTO.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/dto/LoginDTO.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/vo/AuthUserVO.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/vo/LoginResultVO.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/config/AuthBootstrapRunner.java`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/service/AuthService.java`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/service/impl/AuthServiceImpl.java`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/controller/AuthController.java`
- Create: `src/test/java/cn/edu/nuc/onboarding/system/AuthFlowIntegrationTest.java`

**Interfaces:**

- Consumes: Task 1 mappers, Task 2 `SessionService` and `PasswordEncoder`, Task 3 verification rows, and `EmployeeMapper`.
- Produces:
  - `AuthService.register(RegisterDTO): LoginResultVO`.
  - `AuthService.login(LoginDTO): LoginResultVO`.
  - `AuthService.currentUser(): AuthUserVO`.
  - `AuthService.logout(String rawToken): void`.
  - `LoginResultVO(String token, LocalDateTime expiresAt, AuthUserVO user)`.
  - `AuthUserVO(Integer accountId, String phone, String displayName, String role, Integer empId, String department)`.
  - Public endpoints `/api/auth/register`, `/api/auth/login`, authenticated endpoints `/api/auth/me`, `/api/auth/logout`.
  - Bootstrap HR account created at application startup when absent.

- [ ] **Step 1: Write the failing auth flow test**

Create `AuthFlowIntegrationTest` using `MockMvc` and these assertions in one test:

1. Insert an employee with phone `13800000001`.
2. Request a code and read `$.data.devCode`.
3. Register with that code and password `Onboard123`.
4. Assert role is `EMPLOYEE`, `empId` is non-null, and token is non-blank.
5. Login again with phone and password.
6. Call `/api/auth/me` with the login token and assert the phone.
7. Logout with the token.
8. Call `/api/auth/me` again and assert code `401`.

Add a second test asserting wrong password returns `5010`.

Add a third test asserting a duplicate registration returns `5003`.

Use this helper to parse the token:

```java
private String jsonString(MvcResult result, String pointer) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(), pointer);
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
mvn -Dtest=AuthFlowIntegrationTest test
```

Expected: compilation failure or HTTP 404 for registration/login endpoints.

- [ ] **Step 3: Implement registration validation**

Use these exact validation rules:

- Phone format: `^1[3-9]\\d{9}$`.
- Password: 8 to 32 characters, contains `[A-Za-z]` and `[0-9]`.
- Employee must exist, be unarchived, and not already have an account.
- Latest unused verification must exist, be unexpired, have `attemptCount < 5`, and match the code.
- Wrong code increments attempts and throws `VERIFICATION_CODE_INVALID`.
- Successful registration marks the verification used.
- Account fields are copied from the employee record.

Registration must be transactional and must insert the account before issuing a session.

- [ ] **Step 4: Implement login, current user, and logout**

Login rejects missing accounts, disabled accounts, and password mismatches with `LOGIN_FAILED` and `手机号或密码错误`. Password matching uses `PasswordEncoder.matches(raw, hash)`.

`currentUser()` reads `AuthContext`; if absent, throw `UNAUTHORIZED`. `logout(rawToken)` calls `SessionService.revoke`.

- [ ] **Step 5: Implement the controller methods**

Add:

```java
@PostMapping("/register")
public ApiResponse<LoginResultVO> register(@RequestBody RegisterDTO dto) {
    return ApiResponse.success("注册成功", authService.register(dto));
}

@PostMapping("/login")
public ApiResponse<LoginResultVO> login(@RequestBody LoginDTO dto) {
    return ApiResponse.success("登录成功", authService.login(dto));
}

@GetMapping("/me")
public ApiResponse<AuthUserVO> me() {
    return ApiResponse.success(authService.currentUser());
}

@PostMapping("/logout")
public ApiResponse<Void> logout(@RequestHeader("Authorization") String authorization) {
    authService.logout(authorization.substring("Bearer ".length()));
    return ApiResponse.success("已退出登录", null);
}
```

Validate the authorization header before substring.

- [ ] **Step 6: Implement HR bootstrap**

Create an `ApplicationRunner` bean that checks `UserAccountMapper.selectByPhone(bootstrapHrPhone)`. When absent, insert an `HR` account with a BCrypt hash generated from `bootstrapHrPassword`, display name from properties, `status = ACTIVE`, and current timestamps. When present, do not update the password.

- [ ] **Step 7: Run auth flow tests**

Run:

```powershell
mvn -Dtest=AuthFlowIntegrationTest test
```

Expected: `BUILD SUCCESS`.

---

### Task 5: Token Interceptor Replaces Header Authorization

**Files:**

- Modify: `src/main/java/cn/edu/nuc/onboarding/system/security/AuthUser.java`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/security/RoleInterceptor.java`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/config/WebConfig.java`
- Modify: `src/test/java/cn/edu/nuc/onboarding/system/RoleInterceptorTest.java`
- Modify: `src/test/java/cn/edu/nuc/onboarding/system/ApiContractTest.java`
- Modify: `src/test/java/cn/edu/nuc/onboarding/system/TaskWorkflowTest.java`
- Modify: `src/test/java/cn/edu/nuc/onboarding/system/ArchiveWorkflowTest.java`

**Interfaces:**

- Consumes: `SessionService.authenticate(String)`.
- Produces:
  - `AuthUser(Integer accountId, String phone, String role, Integer empId, String department, String operator)`.
  - Compatibility constructor `AuthUser(String role, String department, String operator)`.
  - `AuthUser.isHr()`, `isEmployee()`, `isDepartment()`.
  - Bearer-token interceptor for every `/api/**` path except the three public auth endpoints.

- [ ] **Step 1: Rewrite `RoleInterceptorTest` to assert token behavior**

Use a mocked `SessionService` and these cases:

```java
@Test
void missingBearerTokenIsRejected() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tasks");
    assertThatThrownBy(() -> interceptor.preHandle(
            request, new MockHttpServletResponse(), new Object()))
            .isInstanceOf(BizException.class)
            .extracting("code")
            .isEqualTo(401);
}

@Test
void employeeTokenPopulatesOwnEmployeeId() {
    when(sessionService.authenticate("employee-token"))
            .thenReturn(account("EMPLOYEE", 7));
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tasks");
    request.addHeader("Authorization", "Bearer employee-token");

    assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
    assertThat(AuthContext.get().empId()).isEqualTo(7);
}

@Test
void xRoleHeaderWithoutTokenIsRejected() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/employees");
    request.addHeader("X-Role", "HR");
    assertThatThrownBy(() -> interceptor.preHandle(
            request, new MockHttpServletResponse(), new Object()))
            .isInstanceOf(BizException.class)
            .extracting("code")
            .isEqualTo(401);
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
mvn -Dtest=RoleInterceptorTest test
```

Expected: compilation failure or old header behavior does not produce 401.

- [ ] **Step 3: Extend `AuthUser` and rewrite interceptor**

Add the six-component record and a compatibility constructor:

```java
public record AuthUser(
        Integer accountId,
        String phone,
        String role,
        Integer empId,
        String department,
        String operator
) {
    public AuthUser(String role, String department, String operator) {
        this(null, null, role, null, department, operator);
    }

    public boolean isHr() {
        return "HR".equals(role);
    }

    public boolean isEmployee() {
        return "EMPLOYEE".equals(role);
    }

    public boolean isDepartment() {
        return "DEPARTMENT".equals(role);
    }
}
```

Rewrite `RoleInterceptor` to:

- Allow `OPTIONS`.
- Extract exactly one Bearer token.
- Call `sessionService.authenticate(rawToken)`.
- Reject `HR` finishing a task.
- Require `DEPARTMENT` accounts to have a nonblank department.
- Set `AuthContext` from the account.
- Clear context in `afterCompletion`.

- [ ] **Step 4: Configure public paths**

In `WebConfig`, register the interceptor for `/api/**` and exclude:

```java
"/api/auth/verification-codes",
"/api/auth/register",
"/api/auth/login"
```

`/api/auth/me` and `/api/auth/logout` must pass through the interceptor.

- [ ] **Step 5: Update API contract authentication**

In `ApiContractTest`, login through the bootstrap HR account and use the returned token instead of `X-Role`.

Run:

```powershell
mvn -Dtest=ApiContractTest,RoleInterceptorTest test
```

Expected: `BUILD SUCCESS`.

---

### Task 6: Employee Task Ownership and Phone Uniqueness

**Files:**

- Modify: `src/main/java/cn/edu/nuc/onboarding/system/service/impl/EmployeeServiceImpl.java`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/service/impl/TaskServiceImpl.java`
- Modify: `src/main/resources/mapper/EmployeeMapper.xml`
- Modify: `src/test/java/cn/edu/nuc/onboarding/system/EmployeeCreationTest.java`
- Modify: `src/test/java/cn/edu/nuc/onboarding/system/TaskWorkflowTest.java`
- Create: `src/test/java/cn/edu/nuc/onboarding/system/EmployeeTaskAuthorizationTest.java`

**Interfaces:**

- Consumes: `AuthContext.get().empId()`, `AuthContext.get().isEmployee()`.
- Produces:
  - Employee task queries force `empId` from the authenticated account.
  - Employee finish checks task ownership before update.
  - Employee creation validates phone format and uniqueness.

- [ ] **Step 1: Write failing employee ownership tests**

Create tests that:

1. Create two employees with distinct valid phones and one task each.
2. Set `AuthContext` to an `EMPLOYEE` bound to employee A.
3. Assert `listTasks(null, null, null, 1, 20)` returns only employee A's task.
4. Assert finishing employee B's task throws `FORBIDDEN`.
5. Assert finishing employee A's task succeeds before the employee's entry date only when allowed by existing date rules.

Update `EmployeeCreationTest` so every creation uses a valid phone and add:

```java
@Test
void rejectsDuplicateAndInvalidEmployeePhone() {
    var first = new EmployeeCreateDTO(
            "张明", "13800000001", "研发部", "工程师", LocalDateTime.now().minusDays(1));
    employeeService.createEmployee(first);

    assertCode(1008, new EmployeeCreateDTO(
            "李华", "13800000001", "行政部", "专员", LocalDateTime.now()));
    assertCode(1007, new EmployeeCreateDTO(
            "王强", "12345", "市场部", "专员", LocalDateTime.now()));
}
```

- [ ] **Step 2: Run ownership tests to verify they fail**

Run:

```powershell
mvn -Dtest=EmployeeTaskAuthorizationTest,EmployeeCreationTest test
```

Expected: employee sees unrelated tasks, employee can finish unrelated tasks, or invalid/duplicate phones are accepted.

- [ ] **Step 3: Enforce phone validation and uniqueness**

In `EmployeeServiceImpl.validateEmployee`:

- Require nonblank phone with `EMPLOYEE_PHONE_REQUIRED`.
- Match `^1[3-9]\\d{9}$` or throw `EMPLOYEE_PHONE_INVALID`.
- Call `employeeMapper.countByPhone(phone)` and throw `EMPLOYEE_PHONE_DUPLICATE` when greater than zero.

Add `countByPhone` to `EmployeeMapper` and implement `SELECT COUNT(*) FROM employee WHERE empPhone = #{phone}`.

- [ ] **Step 4: Enforce employee task ownership**

In `TaskServiceImpl.listTasks`, when the current user is `EMPLOYEE`, replace any request `empId` with `user.empId()`. If `user.empId()` is null, throw `FORBIDDEN`.

In `finishTask`, add:

```java
if (user != null && user.isEmployee()
        && !Objects.equals(user.empId(), task.getEmpId())) {
    throw new BizException(ErrorCode.FORBIDDEN, "无权确认其他员工的任务");
}
```

Keep the existing department permission and date rules.

- [ ] **Step 5: Run focused task tests**

Run:

```powershell
mvn -Dtest=EmployeeTaskAuthorizationTest,EmployeeCreationTest,TaskWorkflowTest test
```

Expected: `BUILD SUCCESS`.

---

### Task 7: Frontend Authentication Experience

**Files:**

- Create: `frontend/src/api/auth.js`
- Modify: `frontend/src/stores/auth.js`
- Modify: `frontend/src/api/http.js`
- Modify: `frontend/src/views/LoginView.vue`
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/styles/main.css`

**Interfaces:**

- Consumes: `/api/auth/verification-codes`, `/api/auth/register`, `/api/auth/login`, `/api/auth/me`, `/api/auth/logout`.
- Produces:
  - Auth API functions `requestCode(phone)`, `register(payload)`, `login(payload)`, `getCurrentUser()`, `logout()`.
  - Store state `token`, `accountId`, `phone`, `displayName`, `role`, `empId`, `department`.
  - Store actions `login(payload)`, `register(payload)`, `logout()`, `restore()`.
  - Computed properties `isHr`, `isEmployee`, `isDepartment`, `isLoggedIn`, `homePath`.
  - Bearer token request header and 401 cleanup.

- [ ] **Step 1: Add the frontend auth API**

Create `frontend/src/api/auth.js`:

```js
import http from './http'

export function requestCode(phone) {
  return http.post('/auth/verification-codes', { phone })
}

export function register(payload) {
  return http.post('/auth/register', payload)
}

export function login(payload) {
  return http.post('/auth/login', payload)
}

export function getCurrentUser() {
  return http.get('/auth/me')
}

export function logout() {
  return http.post('/auth/logout')
}
```

- [ ] **Step 2: Replace the Pinia auth store**

Persist the full auth result under `onboarding-auth`. Implement `homePath` as:

```js
const homePath = computed(() => (role.value === 'HR' ? '/templates' : '/tasks'))
```

`restore()` returns immediately when no token exists; otherwise calls `getCurrentUser()`, updates the user fields, and clears auth if the request fails.

`logout()` calls the API best-effort, then always clears local state.

- [ ] **Step 3: Switch Axios to Bearer authentication**

Replace role headers with:

```js
if (auth.token) {
  config.headers.Authorization = `Bearer ${auth.token}`
}
```

On a response code or HTTP status of 401, clear the auth store and redirect with `window.location.replace('/login')`. Avoid importing the router into `http.js`.

- [ ] **Step 4: Rebuild the login page**

Use Element Plus segmented buttons or tabs for `登录` and `注册`.

Login fields:

- `手机号（工号）`
- `密码`

Registration fields:

- `手机号（工号）`
- `短信验证码`
- `密码`
- `确认密码`

The send-code button must show a 60-second countdown and be disabled for an invalid phone. If `devCode` is returned, fill the code field and show a success message containing the code.

Registration validates that both password fields match before submission. Successful login or registration calls the store and routes to `auth.homePath`.

- [ ] **Step 5: Update route guards**

Replace role selection checks with:

- Public login route redirects logged-in users to `auth.homePath`.
- Unauthenticated protected routes redirect to login.
- HR route roles remain `['HR']`.
- `/tasks` accepts `['HR', 'EMPLOYEE', 'DEPARTMENT']`.
- Unauthorized roles redirect to `auth.homePath`.

- [ ] **Step 6: Build the frontend**

Run:

```powershell
cd frontend
npm run build
```

Expected: Vite build succeeds with no unresolved imports or template errors.

---

### Task 8: Employee Task Workspace and Navigation

**Files:**

- Modify: `frontend/src/views/TaskView.vue`
- Modify: `frontend/src/layouts/AppLayout.vue`
- Modify: `frontend/src/styles/main.css`

**Interfaces:**

- Consumes: auth store `isHr`, `isEmployee`, `isDepartment`, `displayName`, `department`.
- Produces:
  - Employee-only navigation labeled `我的任务`.
  - Employee task list with no identity filters.
  - Confirm-finish button enabled only when `row.canFinish` is true.
  - HR task view remains read-only for task completion.

- [ ] **Step 1: Make TaskView role-aware**

Set the page heading and filters as follows:

```js
const pageTitle = computed(() => (auth.isEmployee ? '我的任务' : '部门任务'))
const showFilters = computed(() => !auth.isEmployee)
```

For employees, call `getTasks` without `empId` and `department`; backend ownership supplies the employee filter. Render the confirmation button only for employees or department users. HR must see a read-only status column.

- [ ] **Step 2: Update the application shell**

For employee navigation return:

```js
[
  { path: '/tasks', label: '我的任务', icon: Calendar }
]
```

Use `auth.displayName` for avatars and labels. Display `员工任务工作台` for employees and `人事工作台` for HR. Logout calls `await auth.logout()` before routing to login.

- [ ] **Step 3: Adjust responsive styles**

Ensure the login panel, verification-code row, employee table actions, and navigation remain usable at 375 px, 768 px, and desktop widths without overlapping text or controls.

- [ ] **Step 4: Run the production build**

Run:

```powershell
cd frontend
npm run build
```

Expected: `BUILD SUCCESS` equivalent from Vite with generated assets.

---

### Task 9: Documentation and Full Closed-Loop Verification

**Files:**

- Modify: `README.md`
- Modify: `启动说明.md`
- Modify: `docs/TEST-CASES.md`

**Interfaces:**

- Consumes: all prior tasks.
- Produces: current setup instructions, demo credentials, verification-code behavior, and documented end-to-end acceptance evidence.

- [ ] **Step 1: Update setup documentation**

Document:

- New auth tables in the initialization warning.
- HR bootstrap variables `AUTH_HR_PHONE`, `AUTH_HR_PASSWORD`, and `AUTH_HR_NAME`.
- Default demo HR login `13800000000` / `Admin@123`.
- `AUTH_EXPOSE_CODE=false` requirement in production.
- New login and registration flow.
- Employee registration requirement that HR must create the employee record first.

- [ ] **Step 2: Add acceptance cases**

Append the scenario from the spec to `docs/TEST-CASES.md`, including expected task count, ownership isolation, finish time update, and HR visibility after employee confirmation.

- [ ] **Step 3: Run all backend tests**

Run:

```powershell
mvn clean test
```

Expected: all tests pass, including the new auth and ownership tests.

- [ ] **Step 4: Run the frontend build**

Run:

```powershell
cd frontend
npm run build
```

Expected: successful production build.

- [ ] **Step 5: Verify the complete scenario with the running application**

Start MySQL, initialize the database, start backend and frontend, then verify:

1. Log in as `13800000000` / `Admin@123`.
2. Create an employee with a valid unique phone and past entry date.
3. Confirm the success message reports the generated task count.
4. Log out.
5. Request a registration code with the employee phone.
6. Register with a compliant password.
7. Confirm the employee only sees that employee's tasks.
8. Finish one task and confirm the status changes to completed.
9. Log in as HR and verify the same task appears completed in the employee detail and statistics.

If MySQL is unavailable, record that environment limitation explicitly; do not claim the manual browser scenario passed.
