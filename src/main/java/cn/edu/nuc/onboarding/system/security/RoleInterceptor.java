package cn.edu.nuc.onboarding.system.security;

import cn.edu.nuc.onboarding.system.common.BizException;
import cn.edu.nuc.onboarding.system.common.ErrorCode;
import cn.edu.nuc.onboarding.system.entity.UserAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RoleInterceptor implements HandlerInterceptor {

    private final SessionService sessionService;

    public RoleInterceptor(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        String rawToken = authorization.substring("Bearer ".length()).trim();
        UserAccount account = sessionService.authenticate(rawToken);
        String role = account.getRole();
        if (!"HR".equals(role) && !"DEPARTMENT".equals(role) && !"EMPLOYEE".equals(role)) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号角色无效");
        }

        String path = request.getRequestURI();
        if (requiresHr(path) && !"HR".equals(role)) {
            throw new BizException(ErrorCode.FORBIDDEN, "当前角色无权执行此操作");
        }
        if (isTaskFinish(path) && "HR".equals(role)) {
            throw new BizException(ErrorCode.FORBIDDEN, "HR 不能代替员工确认任务");
        }
        String department = account.getDepartment();
        if ("DEPARTMENT".equals(role) && !StringUtils.hasText(department)) {
            throw new BizException(ErrorCode.FORBIDDEN, "部门责任人必须选择所属部门");
        }
        AuthContext.set(new AuthUser(
                account.getAccountId(),
                account.getPhone(),
                role,
                account.getEmpId(),
                department,
                account.getDisplayName()
        ));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        AuthContext.clear();
    }

    private boolean requiresHr(String path) {
        return path.startsWith("/api/templates")
                || path.startsWith("/api/employees")
                || path.startsWith("/api/stats")
                || path.equals("/api/tasks/overdue");
    }

    private boolean isTaskFinish(String path) {
        return path.startsWith("/api/tasks/") && path.endsWith("/finish");
    }

}
