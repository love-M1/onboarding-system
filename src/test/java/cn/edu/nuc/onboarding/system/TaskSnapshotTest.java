package cn.edu.nuc.onboarding.system;

import cn.edu.nuc.onboarding.system.dto.EmployeeCreateDTO;
import cn.edu.nuc.onboarding.system.mapper.EmpTaskMapper;
import cn.edu.nuc.onboarding.system.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TaskSnapshotTest {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmpTaskMapper empTaskMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void generatedTaskKeepsAssignmentAndDueDateWhenTemplateChanges() {
        jdbcTemplate.update("""
                INSERT INTO task_template(taskName, dutyDept, offsetDay)
                VALUES ('提交材料', '研发部', 3)
                """);
        LocalDate entryDate = LocalDate.now().minusDays(2);
        var employee = employeeService.createEmployee(new EmployeeCreateDTO(
                "快照员工",
                "13800000009",
                "研发部",
                "工程师",
                entryDate.atTime(9, 0)
        ));
        Integer taskId = jdbcTemplate.queryForObject(
                "SELECT taskId FROM emp_task WHERE empId = ?",
                Integer.class,
                employee.empId()
        );

        jdbcTemplate.update("""
                UPDATE task_template
                SET dutyDept = '行政部', offsetDay = 10
                WHERE taskName = '提交材料'
                """);

        var task = empTaskMapper.selectTaskDetail(taskId);
        LocalDate expectedDueDate = entryDate.plusDays(3);
        assertThat(task.getAssignedDept()).isEqualTo("研发部");
        assertThat(task.getDutyDept()).isEqualTo("研发部");
        assertThat(task.getBaseDueDate()).isEqualTo(expectedDueDate);
        assertThat(task.getCurrentDueDate()).isEqualTo(expectedDueDate);
        assertThat(task.getDueDate()).isEqualTo(expectedDueDate);
    }
}
