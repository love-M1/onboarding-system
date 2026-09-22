package cn.edu.nuc.onboarding.system;

import cn.edu.nuc.onboarding.system.common.BizException;
import cn.edu.nuc.onboarding.system.config.StorageProperties;
import cn.edu.nuc.onboarding.system.dto.EmployeeCreateDTO;
import cn.edu.nuc.onboarding.system.entity.TaskAttachment;
import cn.edu.nuc.onboarding.system.mapper.TaskAttachmentMapper;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TaskSubmissionTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private TaskAttachmentMapper attachmentMapper;

    @Autowired
    private StorageProperties storageProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearAuth() {
        AuthContext.clear();
    }

    @Test
    void employeeSubmissionPersistsFileActionAndPendingStatus() throws Exception {
        TestTask testTask = createTask("13800000001");
        AuthContext.set(employee(testTask.accountId(), testTask.empId(), "13800000001"));
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "身份证明.pdf",
                "application/pdf",
                "pdf-content".getBytes()
        );

        TaskDetailVO detail = taskService.submitTask(
                testTask.taskId(),
                "材料已提交",
                List.of(file)
        );

        assertThat(detail.task().getTaskStatus()).isEqualTo(1);
        assertThat(detail.task().getCurrentSubmissionId()).isNotNull();
        assertThat(detail.actions()).hasSize(1);
        assertThat(detail.actions().get(0).actionType()).isEqualTo("SUBMIT");
        assertThat(detail.actions().get(0).attachments()).hasSize(1);

        List<TaskAttachment> attachments = attachmentMapper.selectByTaskId(testTask.taskId());
        assertThat(attachments).hasSize(1);
        assertThat(attachments.get(0).getOriginalName()).isEqualTo("身份证明.pdf");
        Path storedPath = Path.of(storageProperties.getUploadDir())
                .resolve(attachments.get(0).getRelativePath());
        assertThat(Files.exists(storedPath)).isTrue();
    }

    @Test
    void submissionRequiresAtLeastOneAttachment() {
        TestTask testTask = createTask("13800000002");
        AuthContext.set(employee(testTask.accountId(), testTask.empId(), "13800000002"));

        assertThatThrownBy(() -> taskService.submitTask(testTask.taskId(), null, List.of()))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(3006);
    }

    @Test
    void submissionRejectsUnsupportedFileType() {
        TestTask testTask = createTask("13800000003");
        AuthContext.set(employee(testTask.accountId(), testTask.empId(), "13800000003"));
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "材料.exe",
                "application/octet-stream",
                "content".getBytes()
        );

        assertThatThrownBy(() -> taskService.submitTask(testTask.taskId(), null, List.of(file)))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(3007);
    }

    @Test
    void submissionRejectsFileLargerThanTenMegabytes() {
        TestTask testTask = createTask("13800000004");
        AuthContext.set(employee(testTask.accountId(), testTask.empId(), "13800000004"));
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "大文件.pdf",
                "application/pdf",
                new byte[10 * 1024 * 1024 + 1]
        );

        assertThatThrownBy(() -> taskService.submitTask(testTask.taskId(), null, List.of(file)))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(3008);
    }

    @Test
    void employeeCannotSubmitAnotherEmployeesTask() {
        TestTask testTask = createTask("13800000005");
        AuthContext.set(employee(999, testTask.empId() + 1, "13800000006"));
        MockMultipartFile file = validPdf();

        assertThatThrownBy(() -> taskService.submitTask(testTask.taskId(), null, List.of(file)))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(403);
    }

    @Test
    void submittedTaskCannotBeSubmittedAgainBeforeDepartmentConfirmation() {
        TestTask testTask = createTask("13800000007");
        AuthContext.set(employee(testTask.accountId(), testTask.empId(), "13800000007"));

        taskService.submitTask(testTask.taskId(), null, List.of(validPdf()));

        assertThatThrownBy(() -> taskService.submitTask(
                testTask.taskId(),
                null,
                List.of(validPdf())
        ))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(3005);
    }

    private TestTask createTask(String phone) {
        jdbcTemplate.update("""
                INSERT INTO task_template(taskName, dutyDept, offsetDay)
                VALUES ('提交材料', '研发部', 0)
                """);
        String employeeName = "员工" + phone.substring(phone.length() - 2);
        var employee = employeeService.createEmployee(new EmployeeCreateDTO(
                employeeName,
                phone,
                "研发部",
                "工程师",
                LocalDateTime.now().minusDays(1)
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
        Integer accountId = jdbcTemplate.queryForObject(
                "SELECT accountId FROM user_account WHERE phone = ?",
                Integer.class,
                phone
        );
        return new TestTask(accountId, employee.empId(), taskId);
    }

    private AuthUser employee(Integer accountId, Integer empId, String phone) {
        return new AuthUser(accountId, phone, "EMPLOYEE", empId, "研发部", "测试员工");
    }

    private MockMultipartFile validPdf() {
        return new MockMultipartFile(
                "files",
                "材料.pdf",
                "application/pdf",
                "pdf-content".getBytes()
        );
    }

    private record TestTask(Integer accountId, Integer empId, Integer taskId) {
    }
}
