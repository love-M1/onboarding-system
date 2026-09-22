package cn.edu.nuc.onboarding.system;

import cn.edu.nuc.onboarding.system.common.BizException;
import cn.edu.nuc.onboarding.system.dto.EmployeeCreateDTO;
import cn.edu.nuc.onboarding.system.security.AuthContext;
import cn.edu.nuc.onboarding.system.security.AuthUser;
import cn.edu.nuc.onboarding.system.service.EmployeeService;
import cn.edu.nuc.onboarding.system.service.TaskService;
import cn.edu.nuc.onboarding.system.vo.TaskDetailVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.mybatis.spring.SqlSessionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TaskConfirmationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    @AfterEach
    void clearAuth() {
        AuthContext.clear();
    }

    @Test
    void departmentConfirmationRecordsReviewerAndHistory() {
        TestTask task = createTask("13800000001", LocalDateTime.now().minusDays(1), 3);
        TaskDetailVO submitted = submitAsEmployee(task);

        AuthContext.set(department(task.departmentAccountId(), "研发部"));
        TaskDetailVO confirmed = taskService.confirmTask(task.taskId());

        assertThat(confirmed.task().getTaskStatus()).isEqualTo(2);
        assertThat(confirmed.task().getFinishByAccountId()).isEqualTo(task.departmentAccountId());
        assertThat(confirmed.task().getFinishByName()).isEqualTo("研发部责任人");
        assertThat(confirmed.task().getFinishTime()).isNotNull();
        assertThat(confirmed.actions()).extracting("actionType")
                .containsExactly("SUBMIT", "CONFIRM");
        assertThat(confirmed.actions().get(1).relatedSubmissionId())
                .isEqualTo(submitted.task().getCurrentSubmissionId());
    }

    @Test
    void wrongDepartmentCannotConfirmTask() {
        TestTask task = createTask("13800000002", LocalDateTime.now().minusDays(1), 3);
        submitAsEmployee(task);
        AuthContext.set(department(task.departmentAccountId(), "行政部"));

        assertThatThrownBy(() -> taskService.confirmTask(task.taskId()))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(403);
    }

    @Test
    void hrCannotConfirmTask() {
        TestTask task = createTask("13800000003", LocalDateTime.now().minusDays(1), 3);
        submitAsEmployee(task);
        AuthContext.set(new AuthUser(1, "13800000000", "HR", null, "人事部", "人事管理员"));

        assertThatThrownBy(() -> taskService.confirmTask(task.taskId()))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(403);
    }

    @Test
    void rejectionRequiresReasonAndValidNewDueDate() {
        TestTask task = createTask("13800000004", LocalDateTime.now().minusDays(1), 3);
        submitAsEmployee(task);
        AuthContext.set(department(task.departmentAccountId(), "研发部"));

        assertThatThrownBy(() -> taskService.rejectTask(
                task.taskId(), " ", LocalDate.now().plusDays(2)))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(3012);
        assertThatThrownBy(() -> taskService.rejectTask(
                task.taskId(), "材料不清晰", LocalDate.now().minusDays(1)))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(3012);
    }

    @Test
    void earlyReturnIsNotOverdue() {
        TestTask task = createTask("13800000005", LocalDateTime.now().minusDays(1), 3);
        submitAsEmployee(task);
        AuthContext.set(department(task.departmentAccountId(), "研发部"));

        TaskDetailVO rejected = taskService.rejectTask(
                task.taskId(),
                "请补充清晰材料",
                LocalDate.now().plusDays(2)
        );

        assertThat(rejected.task().getTaskStatus()).isEqualTo(3);
        assertThat(rejected.task().getOverdue()).isFalse();
        assertThat(rejected.actions().get(1).reason()).isEqualTo("请补充清晰材料");
        assertThat(rejected.actions().get(1).newDueDate()).isEqualTo(LocalDate.now().plusDays(2));
    }

    @Test
    void lateSubmissionRemainsOverdueAfterAnEarlyReturn() {
        TestTask task = createTask("13800000006", LocalDateTime.now().minusDays(2), 0);
        submitAsEmployee(task);
        AuthContext.set(department(task.departmentAccountId(), "研发部"));

        TaskDetailVO rejected = taskService.rejectTask(
                task.taskId(),
                "材料不清晰",
                LocalDate.now().plusDays(2)
        );

        assertThat(rejected.task().getOverdue()).isTrue();
    }

    @Test
    void rejectedTaskBecomesOverdueWhenNewDeadlinePasses() {
        TestTask task = createTask("13800000007", LocalDateTime.now().minusDays(1), 3);
        submitAsEmployee(task);
        AuthContext.set(department(task.departmentAccountId(), "研发部"));
        taskService.rejectTask(task.taskId(), "材料不清晰", LocalDate.now());
        jdbcTemplate.update(
                "UPDATE emp_task SET currentDueDate = ? WHERE taskId = ?",
                LocalDate.now().minusDays(1),
                task.taskId()
        );
        sqlSessionTemplate.clearCache();

        TaskDetailVO detail = taskService.getTaskDetail(task.taskId());
        assertThat(detail.task().getOverdue()).isTrue();
    }

    @Test
    void rejectedTaskCanBeResubmittedAndKeepsHistory() {
        TestTask task = createTask("13800000008", LocalDateTime.now().minusDays(1), 3);
        TaskDetailVO firstSubmission = submitAsEmployee(task);
        AuthContext.set(department(task.departmentAccountId(), "研发部"));
        taskService.rejectTask(task.taskId(), "材料不清晰", LocalDate.now().plusDays(2));

        AuthContext.set(employee(task));
        TaskDetailVO resubmitted = taskService.submitTask(
                task.taskId(),
                "已补充",
                List.of(validPdf("补充材料.pdf"))
        );

        assertThat(resubmitted.task().getTaskStatus()).isEqualTo(1);
        assertThat(resubmitted.task().getCurrentSubmissionId())
                .isNotEqualTo(firstSubmission.task().getCurrentSubmissionId());
        assertThat(resubmitted.actions()).extracting("actionType")
                .containsExactly("SUBMIT", "REJECT", "SUBMIT");
        assertThat(resubmitted.actions().get(0).attachments()).hasSize(1);
    }

    private TestTask createTask(String phone, LocalDateTime entryTime, int offsetDay) {
        jdbcTemplate.update("""
                INSERT INTO task_template(taskName, dutyDept, offsetDay)
                VALUES ('提交材料', '研发部', ?)
                """, offsetDay);
        String employeeName = "员工" + phone.substring(phone.length() - 2);
        var employee = employeeService.createEmployee(new EmployeeCreateDTO(
                employeeName,
                phone,
                "研发部",
                "工程师",
                entryTime
        ));
        Integer taskId = jdbcTemplate.queryForObject(
                "SELECT taskId FROM emp_task WHERE empId = ?",
                Integer.class,
                employee.empId()
        );
        jdbcTemplate.update("""
                INSERT INTO user_account(phone, passwordHash, role, empId, displayName,
                                         department, status, createTime, updateTime)
                VALUES (?, 'hash', 'EMPLOYEE', ?, ?, '研发部', 'ACTIVE',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, phone, employee.empId(), employeeName);
        Integer employeeAccountId = jdbcTemplate.queryForObject(
                "SELECT accountId FROM user_account WHERE phone = ?",
                Integer.class,
                phone
        );
        String departmentPhone = "139" + phone.substring(3);
        jdbcTemplate.update("""
                INSERT INTO user_account(phone, passwordHash, role, empId, displayName,
                                         department, status, createTime, updateTime)
                VALUES (?, 'hash', 'DEPARTMENT', NULL, '研发部责任人', '研发部',
                        'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, departmentPhone);
        Integer departmentAccountId = jdbcTemplate.queryForObject(
                "SELECT accountId FROM user_account WHERE phone = ?",
                Integer.class,
                departmentPhone
        );
        return new TestTask(employeeAccountId, departmentAccountId, employee.empId(), taskId);
    }

    private TaskDetailVO submitAsEmployee(TestTask task) {
        AuthContext.set(employee(task));
        return taskService.submitTask(
                task.taskId(),
                "首次提交",
                List.of(validPdf("材料.pdf"))
        );
    }

    private AuthUser employee(TestTask task) {
        return new AuthUser(
                task.employeeAccountId(),
                "13800000001",
                "EMPLOYEE",
                task.empId(),
                "研发部",
                "测试员工"
        );
    }

    private AuthUser department(Integer accountId, String department) {
        return new AuthUser(
                accountId,
                "13900000001",
                "DEPARTMENT",
                null,
                department,
                department + "责任人"
        );
    }

    private MockMultipartFile validPdf(String name) {
        return new MockMultipartFile(
                "files",
                name,
                "application/pdf",
                "pdf-content".getBytes()
        );
    }

    private record TestTask(
            Integer employeeAccountId,
            Integer departmentAccountId,
            Integer empId,
            Integer taskId
    ) {
    }
}
