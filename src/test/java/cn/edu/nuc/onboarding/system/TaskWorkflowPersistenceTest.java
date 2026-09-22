package cn.edu.nuc.onboarding.system;

import cn.edu.nuc.onboarding.system.entity.EmpTask;
import cn.edu.nuc.onboarding.system.entity.TaskAction;
import cn.edu.nuc.onboarding.system.entity.TaskAttachment;
import cn.edu.nuc.onboarding.system.mapper.EmpTaskMapper;
import cn.edu.nuc.onboarding.system.mapper.TaskActionMapper;
import cn.edu.nuc.onboarding.system.mapper.TaskAttachmentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TaskWorkflowPersistenceTest {

    @Autowired
    private EmpTaskMapper taskMapper;

    @Autowired
    private TaskActionMapper actionMapper;

    @Autowired
    private TaskAttachmentMapper attachmentMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsTaskSnapshotActionAndAttachment() {
        Integer accountId = createAccount();
        Integer employeeId = createEmployee();
        Integer templateId = createTemplate();

        EmpTask task = new EmpTask();
        task.setEmpId(employeeId);
        task.setTplId(templateId);
        task.setAssignedDept("研发部");
        task.setBaseDueDate(LocalDate.now().plusDays(3));
        task.setCurrentDueDate(task.getBaseDueDate());
        task.setTaskStatus(0);
        task.setVersion(0);
        taskMapper.insertSnapshotTask(task);

        TaskAction action = new TaskAction();
        action.setTaskId(task.getTaskId());
        action.setActionType("SUBMIT");
        action.setActorAccountId(accountId);
        action.setActorNameSnapshot("张三");
        action.setActionTime(LocalDateTime.now());
        actionMapper.insert(action);

        TaskAttachment attachment = new TaskAttachment();
        attachment.setTaskId(task.getTaskId());
        attachment.setActionId(action.getActionId());
        attachment.setOriginalName("身份证.pdf");
        attachment.setStorageName("random.pdf");
        attachment.setRelativePath("2026/09/22/random.pdf");
        attachment.setContentType("application/pdf");
        attachment.setFileSize(128L);
        attachment.setUploaderAccountId(accountId);
        attachment.setUploadTime(LocalDateTime.now());
        attachmentMapper.insertBatch(List.of(attachment));

        assertThat(taskMapper.selectById(task.getTaskId()).getAssignedDept()).isEqualTo("研发部");
        assertThat(actionMapper.selectByTaskId(task.getTaskId())).hasSize(1);
        assertThat(attachmentMapper.selectByTaskId(task.getTaskId())).hasSize(1);
    }

    private Integer createEmployee() {
        jdbcTemplate.update("""
                INSERT INTO employee(empName, empPhone, empDepartment, empPosition,
                                     entryTime, isArchived, createTime)
                VALUES ('张三', '13900000001', '研发部', '工程师',
                        CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP)
                """);
        return jdbcTemplate.queryForObject(
                "SELECT empId FROM employee WHERE empPhone = '13900000001'",
                Integer.class
        );
    }

    private Integer createTemplate() {
        jdbcTemplate.update("""
                INSERT INTO task_template(taskName, dutyDept, offsetDay)
                VALUES ('提交材料', '研发部', 3)
                """);
        return jdbcTemplate.queryForObject(
                "SELECT tplId FROM task_template WHERE taskName = '提交材料'",
                Integer.class
        );
    }

    private Integer createAccount() {
        jdbcTemplate.update("""
                INSERT INTO user_account(phone, passwordHash, role, empId, displayName,
                                         department, status, createTime, updateTime)
                VALUES ('13900000001', 'hash', 'EMPLOYEE', NULL, '张三',
                        '研发部', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        return jdbcTemplate.queryForObject(
                "SELECT accountId FROM user_account WHERE phone = '13900000001'",
                Integer.class
        );
    }
}
