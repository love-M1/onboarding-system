package cn.edu.nuc.onboarding.system;

import cn.edu.nuc.onboarding.system.common.BizException;
import cn.edu.nuc.onboarding.system.entity.UserAccount;
import cn.edu.nuc.onboarding.system.security.AuthContext;
import cn.edu.nuc.onboarding.system.security.RoleInterceptor;
import cn.edu.nuc.onboarding.system.security.SessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleInterceptorTest {

    private final SessionService sessionService = mock(SessionService.class);
    private final RoleInterceptor interceptor = new RoleInterceptor(sessionService);

    @AfterEach
    void clearContext() {
        AuthContext.clear();
    }

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
                .thenReturn(account("EMPLOYEE", 7, "研发部"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tasks");
        request.addHeader("Authorization", "Bearer employee-token");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(AuthContext.get().empId()).isEqualTo(7);
        assertThat(AuthContext.get().isEmployee()).isTrue();
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

    @Test
    void hrCannotConfirmEmployeeTask() {
        when(sessionService.authenticate("hr-token"))
                .thenReturn(account("HR", null, "人事部"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/tasks/1/finish");
        request.addHeader("Authorization", "Bearer hr-token");

        assertThatThrownBy(() -> interceptor.preHandle(
                request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(403);
    }

    @Test
    void employeeCannotManageDepartmentOwners() {
        when(sessionService.authenticate("employee-token"))
                .thenReturn(account("EMPLOYEE", 7, "研发部"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/department-owners");
        request.addHeader("Authorization", "Bearer employee-token");

        assertThatThrownBy(() -> interceptor.preHandle(
                request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(403);
    }

    @Test
    void hrCanManageDepartmentOwners() {
        when(sessionService.authenticate("hr-token"))
                .thenReturn(account("HR", null, "人事部"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/department-owners");
        request.addHeader("Authorization", "Bearer hr-token");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
    }

    private UserAccount account(String role, Integer empId, String department) {
        UserAccount account = new UserAccount();
        account.setAccountId(1);
        account.setPhone("13800000001");
        account.setRole(role);
        account.setEmpId(empId);
        account.setDepartment(department);
        account.setDisplayName("测试用户");
        account.setStatus("ACTIVE");
        return account;
    }
}
