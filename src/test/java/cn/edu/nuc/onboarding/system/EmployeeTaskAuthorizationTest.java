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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
    void employeeOnlySeesAndFinishesOwnTask() {
        jdbcTemplate.update("INSERT INTO task_template(taskName, dutyDept, offsetDay) VALUES ('办理事项', '研发部', 0)");
        var employeeA = employeeService.createEmployee(new EmployeeCreateDTO(
                "员工甲", "13800000001", "研发部", "工程师", LocalDateTime.now().minusDays(1)));
        var employeeB = employeeService.createEmployee(new EmployeeCreateDTO(
                "员工乙", "13800000002", "研发部", "工程师", LocalDateTime.now().minusDays(1)));
        Integer taskA = taskId(employeeA.empId());
        Integer taskB = taskId(employeeB.empId());

        AuthContext.set(new AuthUser(
                10, "13800000001", "EMPLOYEE", employeeA.empId(), "研发部", "员工甲"));

        var page = taskService.listTasks(null, null, null, 1, 20);
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.list()).extracting("taskId").containsExactly(taskA);

        assertThatThrownBy(() -> taskService.finishTask(taskB))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(403);

        assertThat(taskService.finishTask(taskA).taskStatus()).isEqualTo(1);
    }

    private Integer taskId(Integer empId) {
        return jdbcTemplate.queryForObject(
                "SELECT taskId FROM emp_task WHERE empId = ?", Integer.class, empId);
    }
}
