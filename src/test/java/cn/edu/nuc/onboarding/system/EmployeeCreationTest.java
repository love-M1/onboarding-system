package cn.edu.nuc.onboarding.system;

import cn.edu.nuc.onboarding.system.common.BizException;
import cn.edu.nuc.onboarding.system.dto.EmployeeCreateDTO;
import cn.edu.nuc.onboarding.system.service.EmployeeService;
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
class EmployeeCreationTest {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsEmployeeAndOneTaskPerTemplateInOneTransaction() {
        insertTemplate("提交材料", "人事部", 0);
        insertTemplate("开通邮箱", "信息技术部", 1);

        var result = employeeService.createEmployee(new EmployeeCreateDTO(
                "张明", "13800000001", "研发部", "开发工程师", LocalDateTime.now().minusDays(1)));

        assertThat(result.generatedTaskCount()).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM employee WHERE empId = ?", result.empId())).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM emp_task WHERE empId = ?", result.empId())).isEqualTo(2);
    }

    @Test
    void createsEmployeeWithNoTasksWhenTemplateTableIsEmpty() {
        jdbcTemplate.update("DELETE FROM task_template");

        var result = employeeService.createEmployee(new EmployeeCreateDTO(
                "李华", "13800000002", "行政部", "行政专员", LocalDateTime.now()));

        assertThat(result.generatedTaskCount()).isZero();
        assertThat(result.message()).contains("未生成任务");
    }

    @Test
    void rejectsMissingRequiredEmployeeFieldsWithDocumentCodes() {
        assertCode(1001, new EmployeeCreateDTO(" ", null, "行政部", "专员", LocalDateTime.now()));
        assertCode(1002, new EmployeeCreateDTO("李华", null, " ", "专员", LocalDateTime.now()));
        assertCode(1003, new EmployeeCreateDTO("李华", null, "行政部", " ", LocalDateTime.now()));
        assertCode(1004, new EmployeeCreateDTO("李华", null, "行政部", "专员", null));
        assertCode(1006, new EmployeeCreateDTO("李华", null, "行政部", "专员", LocalDateTime.now()));
    }

    @Test
    void rejectsDuplicateAndInvalidEmployeePhone() {
        employeeService.createEmployee(new EmployeeCreateDTO(
                "张明", "13800000001", "研发部", "工程师", LocalDateTime.now().minusDays(1)));

        assertCode(1008, new EmployeeCreateDTO(
                "李华", "13800000001", "行政部", "专员", LocalDateTime.now()));
        assertCode(1007, new EmployeeCreateDTO(
                "王强", "12345", "市场部", "专员", LocalDateTime.now()));
    }

    private void assertCode(int code, EmployeeCreateDTO dto) {
        assertThatThrownBy(() -> employeeService.createEmployee(dto))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(code);
    }

    private void insertTemplate(String taskName, String dutyDept, int offsetDay) {
        jdbcTemplate.update("INSERT INTO task_template(taskName, dutyDept, offsetDay) VALUES (?, ?, ?)",
                taskName, dutyDept, offsetDay);
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }
}
