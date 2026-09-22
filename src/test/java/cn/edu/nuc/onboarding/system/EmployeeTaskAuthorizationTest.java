package cn.edu.nuc.onboarding.system;

import cn.edu.nuc.onboarding.system.common.BizException;
import cn.edu.nuc.onboarding.system.dto.EmployeeCreateDTO;
import cn.edu.nuc.onboarding.system.security.AuthContext;
import cn.edu.nuc.onboarding.system.security.AuthUser;
import cn.edu.nuc.onboarding.system.service.EmployeeService;
import cn.edu.nuc.onboarding.system.service.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EmployeeTaskAuthorizationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private TaskService taskService;

    @AfterEach
    void clearAuth() {
        AuthContext.clear();
    }

    @Test
    void employeeOnlySeesAndSubmitsOwnTask() {
        jdbcTemplate.update("INSERT INTO task_template(taskName, dutyDept, offsetDay) VALUES ('办理事项', '研发部', 0)");
        var employeeA = employeeService.createEmployee(new EmployeeCreateDTO(
                "员工甲", "13800000001", "研发部", "工程师", LocalDateTime.now().minusDays(1)));
        var employeeB = employeeService.createEmployee(new EmployeeCreateDTO(
                "员工乙", "13800000002", "研发部", "工程师", LocalDateTime.now().minusDays(1)));
        Integer taskA = taskId(employeeA.empId());
        Integer taskB = taskId(employeeB.empId());

        Integer accountId = accountId("13800000001", employeeA.empId(), "员工甲");
        AuthContext.set(new AuthUser(
                accountId, "13800000001", "EMPLOYEE", employeeA.empId(), "研发部", "员工甲"));

        var page = taskService.listTasks(null, null, null, 1, 20);
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.list()).extracting("taskId").containsExactly(taskA);

        assertThatThrownBy(() -> taskService.submitTask(
                taskB,
                null,
                List.of(validPdf())
        ))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(403);

        assertThat(taskService.submitTask(
                taskA,
                null,
                List.of(validPdf())
        ).task().getTaskStatus()).isEqualTo(1);
    }

    private Integer taskId(Integer empId) {
        return jdbcTemplate.queryForObject(
                "SELECT taskId FROM emp_task WHERE empId = ?", Integer.class, empId);
    }

    private MockMultipartFile validPdf() {
        return new MockMultipartFile(
                "files",
                "材料.pdf",
                "application/pdf",
                "pdf-content".getBytes()
        );
    }

    private Integer accountId(String phone, Integer empId, String displayName) {
        List<Integer> existing = jdbcTemplate.queryForList(
                "SELECT accountId FROM user_account WHERE phone = ?", Integer.class, phone);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        jdbcTemplate.update("""
                INSERT INTO user_account(phone, passwordHash, role, empId, displayName,
                                         department, status, createTime, updateTime)
                VALUES (?, 'hash', 'EMPLOYEE', ?, ?, '研发部', 'ACTIVE',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, phone, empId, displayName);
        return jdbcTemplate.queryForObject(
                "SELECT accountId FROM user_account WHERE phone = ?", Integer.class, phone);
    }
}
