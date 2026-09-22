# HR、员工与部门责任人任务确认闭环 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement HR-managed department owners and a four-state employee-submit, department-confirm/reject task workflow with persisted attachments and complete history.

**Architecture:** Keep the existing Spring Boot and MyBatis layering. Extend `emp_task` with assignment, due-date, review, and optimistic-lock fields; add append-only action history and attachment metadata tables; store files under a configurable local directory; extend the Vue application with HR department-owner management and department review views while preserving the employee workspace layout.

**Tech Stack:** Java 21, Spring Boot 3.2, Spring MVC, Bean Validation, MyBatis XML, MySQL 8, H2, JUnit 5, MockMvc, Vue 3, Vite, Pinia, Axios, Element Plus.

**Spec:** `docs/superpowers/specs/2026-09-22-hr-employee-department-confirmation-design.md`

## Global Constraints

- `emp_task.taskStatus` values are `0=待员工处理`, `1=待部门确认`, `2=已完成`, `3=已退回`.
- HR creates and manages department-owner accounts; each department has at most one enabled owner.
- Department-owner deletion is a soft delete using `status=DISABLED`.
- Task responsibility and due dates are snapshots created during employee registration.
- Employee submission is multipart and requires at least one JPG, JPEG, PNG, or PDF file.
- Each file is at most 10 MB; each submission contains at most 10 files.
- Department rejection requires a reason and a new due date not earlier than today.
- Returned tasks are not automatically overdue. Overdue is derived from original deadline, submission time, new deadline, and current state.
- Attachments are never exposed from a public static directory.
- Existing employee task-workspace styling is preserved.
- Run backend tests with `mvn clean test`; run frontend tests with `npm test` from `frontend/`.
- Run frontend production builds with `npm run build` from `frontend/`.

---

### Task 1: Persistence and Task-Domain Model

**Files:**

- Modify: `sql/onboarding_sys.sql`
- Modify: `src/test/resources/schema-test.sql`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/entity/EmpTask.java`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/vo/EmpTaskVO.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/entity/TaskAction.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/entity/TaskAttachment.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/mapper/TaskActionMapper.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/mapper/TaskAttachmentMapper.java`
- Create: `src/main/resources/mapper/TaskActionMapper.xml`
- Create: `src/main/resources/mapper/TaskAttachmentMapper.xml`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/mapper/EmpTaskMapper.java`
- Modify: `src/main/resources/mapper/EmpTaskMapper.xml`
- Test: `src/test/java/cn/edu/nuc/onboarding/system/TaskWorkflowPersistenceTest.java`

**Interfaces:**

- Produces `EmpTask` fields `assignedDept`, `baseDueDate`, `currentDueDate`, `currentSubmissionId`, `finishByAccountId`, `finishByName`, `finishTime`, `version`.
- Produces `TaskActionMapper.insert(TaskAction)`, `selectByTaskId(Integer)`.
- Produces `TaskAttachmentMapper.insertBatch(List<TaskAttachment>)`, `selectByActionId(Integer)`, `selectByTaskId(Integer)`, `selectById(Integer)`.
- Produces the `task_action` and `task_attachment` tables in MySQL and H2.

- [ ] **Step 1: Write the failing persistence test**

```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TaskWorkflowPersistenceTest {

    @Autowired private EmpTaskMapper taskMapper;
    @Autowired private TaskActionMapper actionMapper;
    @Autowired private TaskAttachmentMapper attachmentMapper;

    @Test
    void persistsTaskSnapshotActionAndAttachment() {
        EmpTask task = new EmpTask();
        task.setEmpId(1);
        task.setTplId(1);
        task.setAssignedDept("研发部");
        task.setBaseDueDate(LocalDate.now().plusDays(3));
        task.setCurrentDueDate(task.getBaseDueDate());
        task.setTaskStatus(0);
        task.setVersion(0);
        taskMapper.insertSnapshotTask(task);

        TaskAction action = new TaskAction();
        action.setTaskId(task.getTaskId());
        action.setActionType("SUBMIT");
        action.setActorAccountId(10);
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
        attachment.setUploaderAccountId(10);
        attachment.setUploadTime(LocalDateTime.now());
        attachmentMapper.insertBatch(List.of(attachment));

        assertThat(actionMapper.selectByTaskId(task.getTaskId())).hasSize(1);
        assertThat(attachmentMapper.selectByTaskId(task.getTaskId())).hasSize(1);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
mvn -Dtest=TaskWorkflowPersistenceTest test
```

Expected: compilation failure because the new model types and SQL columns do not exist.

- [ ] **Step 3: Add schemas, entities, mapper interfaces, and XML**

Use these exact `emp_task` additions:

```sql
assignedDept VARCHAR(50) NOT NULL,
baseDueDate DATE NOT NULL,
currentDueDate DATE NOT NULL,
currentSubmissionId INT DEFAULT NULL,
finishByAccountId INT DEFAULT NULL,
finishByName VARCHAR(50) DEFAULT NULL,
version INT NOT NULL DEFAULT 0
```

Use these exact history tables:

```sql
CREATE TABLE task_action (
    actionId INT PRIMARY KEY AUTO_INCREMENT,
    taskId INT NOT NULL,
    actionType VARCHAR(20) NOT NULL,
    actorAccountId INT NOT NULL,
    actorNameSnapshot VARCHAR(50) NOT NULL,
    actionTime DATETIME NOT NULL,
    reason VARCHAR(500) DEFAULT NULL,
    newDueDate DATE DEFAULT NULL,
    relatedSubmissionId INT DEFAULT NULL,
    INDEX idx_task_action_task_time (taskId, actionTime),
    CONSTRAINT fk_task_action_task FOREIGN KEY (taskId) REFERENCES emp_task(taskId) ON DELETE CASCADE,
    CONSTRAINT fk_task_action_actor FOREIGN KEY (actorAccountId) REFERENCES user_account(accountId)
);

CREATE TABLE task_attachment (
    attachmentId INT PRIMARY KEY AUTO_INCREMENT,
    taskId INT NOT NULL,
    actionId INT NOT NULL,
    originalName VARCHAR(255) NOT NULL,
    storageName VARCHAR(100) NOT NULL,
    relativePath VARCHAR(500) NOT NULL,
    contentType VARCHAR(100) NOT NULL,
    fileSize BIGINT NOT NULL,
    uploaderAccountId INT NOT NULL,
    uploadTime DATETIME NOT NULL,
    UNIQUE KEY uk_task_attachment_storage (storageName),
    INDEX idx_task_attachment_task (taskId),
    CONSTRAINT fk_task_attachment_task FOREIGN KEY (taskId) REFERENCES emp_task(taskId) ON DELETE CASCADE,
    CONSTRAINT fk_task_attachment_action FOREIGN KEY (actionId) REFERENCES task_action(actionId) ON DELETE CASCADE,
    CONSTRAINT fk_task_attachment_uploader FOREIGN KEY (uploaderAccountId) REFERENCES user_account(accountId)
);
```

- [ ] **Step 4: Run the focused test**

Run:

```powershell
mvn -Dtest=TaskWorkflowPersistenceTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```powershell
git add sql src/main src/test
git commit -m "feat: persist task workflow history and attachments"
```

### Task 2: HR Department-Owner Management

**Files:**

- Create: `src/main/java/cn/edu/nuc/onboarding/system/dto/DepartmentOwnerSaveDTO.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/dto/DepartmentOwnerUpdateDTO.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/dto/PasswordResetDTO.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/vo/DepartmentOwnerVO.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/service/DepartmentOwnerService.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/service/impl/DepartmentOwnerServiceImpl.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/controller/DepartmentOwnerController.java`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/mapper/UserAccountMapper.java`
- Modify: `src/main/resources/mapper/UserAccountMapper.xml`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/config/WebConfig.java`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/security/RoleInterceptor.java`
- Test: `src/test/java/cn/edu/nuc/onboarding/system/DepartmentOwnerManagementTest.java`

**Interfaces:**

- Produces endpoints `/api/department-owners`.
- Produces `listDepartmentOwners()`, `createDepartmentOwner(DepartmentOwnerSaveDTO)`, `updateDepartmentOwner(Integer, DepartmentOwnerUpdateDTO)`, `disableDepartmentOwner(Integer)`, `resetPassword(Integer, PasswordResetDTO)`.
- Produces `DepartmentOwnerVO(accountId, phone, displayName, department, status, createdAt, updatedAt)`.

- [ ] **Step 1: Write failing MockMvc tests**

Cover:

1. HR creates a department owner.
2. The owner can log in.
3. A second enabled owner for the same department returns a business error.
4. HR disables the owner and the old account cannot log in.
5. HR resets the password and the new password works.
6. Employee and department tokens receive `403` for management endpoints.

- [ ] **Step 2: Run the tests to verify they fail**

```powershell
mvn -Dtest=DepartmentOwnerManagementTest test
```

Expected: missing endpoint or compilation failure.

- [ ] **Step 3: Implement account management**

In `UserAccountMapper.xml`, add exact operations:

```xml
<select id="selectDepartmentOwners" resultType="cn.edu.nuc.onboarding.system.entity.UserAccount">
    SELECT accountId, phone, passwordHash, role, empId, displayName, department,
           status, createTime, updateTime
    FROM user_account
    WHERE role = 'DEPARTMENT'
    ORDER BY department ASC, accountId ASC
</select>

<select id="countEnabledDepartmentOwner" resultType="long">
    SELECT COUNT(*)
    FROM user_account
    WHERE role = 'DEPARTMENT'
      AND status = 'ACTIVE'
      AND department = #{department}
      <if test="excludeAccountId != null">
        AND accountId != #{excludeAccountId}
      </if>
</select>

<update id="updateDepartmentOwner">
    UPDATE user_account
    SET phone = #{phone},
        displayName = #{displayName},
        department = #{department},
        status = #{status},
        updateTime = #{updateTime}
    WHERE accountId = #{accountId}
      AND role = 'DEPARTMENT'
</update>

<update id="updateStatus">
    UPDATE user_account
    SET status = #{status},
        updateTime = #{updateTime}
    WHERE accountId = #{accountId}
</update>

<update id="updatePassword">
    UPDATE user_account
    SET passwordHash = #{passwordHash},
        updateTime = #{updateTime}
    WHERE accountId = #{accountId}
      AND role = 'DEPARTMENT'
</update>
```

The service must validate the phone pattern, password policy, nonblank name and department, and department uniqueness inside a transaction.

- [ ] **Step 4: Restrict endpoints to HR**

Add `/api/department-owners` to `RoleInterceptor.requiresHr`.

- [ ] **Step 5: Run focused and contract tests**

```powershell
mvn -Dtest=DepartmentOwnerManagementTest,RoleInterceptorTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```powershell
git add src/main src/test
git commit -m "feat: add HR department owner management"
```

### Task 3: Assignment and Due-Date Snapshots

**Files:**

- Modify: `src/main/java/cn/edu/nuc/onboarding/system/service/impl/EmployeeServiceImpl.java`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/mapper/EmpTaskMapper.java`
- Modify: `src/main/resources/mapper/EmpTaskMapper.xml`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/vo/EmpTaskVO.java`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/service/impl/StatServiceImpl.java`
- Modify: `src/test/java/cn/edu/nuc/onboarding/system/EmployeeCreationTest.java`
- Modify: `src/test/java/cn/edu/nuc/onboarding/system/TaskWorkflowTest.java`
- Create: `src/test/java/cn/edu/nuc/onboarding/system/TaskSnapshotTest.java`

**Interfaces:**

- `EmployeeServiceImpl.createEmployee` writes `assignedDept`, `baseDueDate`, `currentDueDate`, and `taskStatus=0` for each generated task.
- Task queries return snapshot values instead of joining current template ownership.

- [ ] **Step 1: Write failing snapshot tests**

Test that:

1. A task generated for `研发部` stores `assignedDept="研发部"`.
2. `baseDueDate` and `currentDueDate` equal `entryTime + offsetDay`.
3. Changing the template afterward does not change the generated task.

- [ ] **Step 2: Run the tests to verify they fail**

```powershell
mvn -Dtest=TaskSnapshotTest test
```

Expected: snapshot fields are null or values still come from the current template.

- [ ] **Step 3: Persist snapshots during task generation**

```java
task.setAssignedDept(template.getDutyDept());
task.setBaseDueDate(employee.getEntryTime().toLocalDate().plusDays(template.getOffsetDay()));
task.setCurrentDueDate(task.getBaseDueDate());
task.setTaskStatus(0);
task.setVersion(0);
```

- [ ] **Step 4: Switch task queries and statistics to snapshot columns**

Replace joins used for ownership and due dates with `et.assignedDept`, `et.baseDueDate`, and `et.currentDueDate`.

- [ ] **Step 5: Run workflow and snapshot tests**

```powershell
mvn -Dtest=TaskSnapshotTest,EmployeeCreationTest,TaskWorkflowTest,ArchiveWorkflowTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```powershell
git add src/main src/test
git commit -m "feat: snapshot task assignment and due dates"
```

### Task 4: Attachment Storage and Employee Submission

**Files:**

- Create: `src/main/java/cn/edu/nuc/onboarding/system/config/StorageProperties.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/service/FileStorageService.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/service/impl/FileStorageServiceImpl.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/vo/TaskDetailVO.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/vo/TaskActionVO.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/vo/TaskAttachmentVO.java`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/service/TaskService.java`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/service/impl/TaskServiceImpl.java`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/controller/TaskController.java`
- Modify: `src/main/resources/application.yml`
- Modify: `src/test/resources/application-test.yml`
- Modify: `.gitignore`
- Test: `src/test/java/cn/edu/nuc/onboarding/system/TaskSubmissionTest.java`

**Interfaces:**

- Produces `TaskService.submitTask(Integer taskId, String note, List<MultipartFile> files): TaskDetailVO`.
- Produces `TaskService.getTaskDetail(Integer taskId): TaskDetailVO`.
- Produces `TaskService.openAttachment(Integer taskId, Integer attachmentId): StoredFile`.
- Produces `POST /api/tasks/{taskId}/submissions` and secure attachment download.

- [ ] **Step 1: Write failing submission tests**

Test successful upload, no-file rejection, unsupported type rejection, oversize rejection, wrong-employee rejection, repeated submission rejection, and persisted action/attachment history.

- [ ] **Step 2: Run the tests to verify they fail**

```powershell
mvn -Dtest=TaskSubmissionTest test
```

Expected: submission endpoint or service method is missing.

- [ ] **Step 3: Implement safe file storage**

Use the configured root:

```yaml
app:
  storage:
    upload-dir: ${ONBOARDING_UPLOAD_DIR:uploads/onboarding}
```

Validate extension and MIME type, enforce size and count, generate `UUID.randomUUID()` storage names, create date directories, and delete written files if the transaction fails.

- [ ] **Step 4: Implement submission state transition**

Within one transaction:

1. Load and authorize the task.
2. Validate state `0` or `3`.
3. Save files.
4. Insert a `SUBMIT` action.
5. Set `currentSubmissionId`, `taskStatus=1`, and increment `version`.
6. Insert attachment rows.

- [ ] **Step 5: Run submission, persistence, and authorization tests**

```powershell
mvn -Dtest=TaskSubmissionTest,TaskWorkflowPersistenceTest,EmployeeTaskAuthorizationTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```powershell
git add .gitignore src/main src/test
git commit -m "feat: persist employee task submissions and attachments"
```

### Task 5: Department Confirmation, Rejection, and Overdue Rules

**Files:**

- Modify: `src/main/java/cn/edu/nuc/onboarding/system/service/TaskService.java`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/service/impl/TaskServiceImpl.java`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/controller/TaskController.java`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/mapper/EmpTaskMapper.java`
- Modify: `src/main/resources/mapper/EmpTaskMapper.xml`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/service/impl/StatServiceImpl.java`
- Modify: `src/main/java/cn/edu/nuc/onboarding/system/common/ErrorCode.java`
- Test: `src/test/java/cn/edu/nuc/onboarding/system/TaskConfirmationTest.java`

**Interfaces:**

- Produces `confirmTask(Integer taskId): TaskDetailVO`.
- Produces `rejectTask(Integer taskId, String reason, LocalDate newDueDate): TaskDetailVO`.
- Produces `POST /api/tasks/{taskId}/confirm`.
- Produces `POST /api/tasks/{taskId}/reject`.

- [ ] **Step 1: Write failing workflow tests**

Cover correct confirmation data, wrong-department rejection, HR confirmation rejection, required rejection reason, invalid new date, early return not overdue, old-deadline return overdue, new-deadline overdue, rejected task resubmission, and preserved history.

- [ ] **Step 2: Run the tests to verify they fail**

```powershell
mvn -Dtest=TaskConfirmationTest test
```

Expected: missing methods, endpoints, or state transitions.

- [ ] **Step 3: Implement confirmation and rejection**

Use conditional updates:

```sql
UPDATE emp_task
SET taskStatus = 2,
    finishByAccountId = #{accountId},
    finishByName = #{finishByName},
    finishTime = #{finishTime},
    version = version + 1
WHERE taskId = #{taskId}
  AND taskStatus = 1
  AND version = #{version}
```

Reject writes `taskStatus=3`, `currentDueDate=#{newDueDate}`, and an append-only `REJECT` action.

- [ ] **Step 4: Implement the agreed overdue calculation**

Return overdue true only when:

- waiting for the employee and today is after the current due date;
- waiting for department confirmation and the submission was late; or
- the task is rejected and either an earlier deadline was missed or the new due date has passed.

- [ ] **Step 5: Run confirmation and statistics tests**

```powershell
mvn -Dtest=TaskConfirmationTest,TaskWorkflowTest,EmployeeTaskAuthorizationTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```powershell
git add src/main src/test
git commit -m "feat: add department task confirmation workflow"
```

### Task 6: Frontend APIs and HR Department-Owner Management

**Files:**

- Create: `frontend/src/api/departmentOwners.js`
- Create: `frontend/src/views/DepartmentOwnerView.vue`
- Modify: `frontend/src/api/tasks.js`
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/layouts/AppLayout.vue`
- Modify: `frontend/src/styles/main.css`
- Test: `frontend/test/departmentOwners.test.js`

**Interfaces:**

- Adds HR navigation and route `/department-owners`.
- Adds CRUD, status, and password-reset API functions.
- Replaces `finishTask` with `submitTask`, `confirmTask`, and `rejectTask`, plus detail and attachment APIs.

- [ ] **Step 1: Write failing frontend utility tests**

Test request payload construction for create, update, disable, password reset, confirm, and reject.

- [ ] **Step 2: Run the tests to verify they fail**

```powershell
npm test
```

Expected: missing exports or failing assertions.

- [ ] **Step 3: Implement API modules and the HR page**

The page must support create, edit, soft delete, enable/disable, and reset password using Element Plus forms and confirmation dialogs.

- [ ] **Step 4: Run frontend tests and build**

```powershell
npm test
npm run build
```

Expected: all tests pass and Vite builds successfully.

- [ ] **Step 5: Commit**

```powershell
git add frontend
git commit -m "feat: add HR department owner management UI"
```

### Task 7: Employee and Department Task Pages

**Files:**

- Modify: `frontend/src/utils/onboardingTasks.js`
- Modify: `frontend/test/onboardingTasks.test.js`
- Modify: `frontend/src/views/TaskView.vue`
- Modify: `frontend/src/views/EmployeeDetailView.vue`
- Modify: `frontend/src/views/StatsView.vue`
- Modify: `frontend/src/utils/stats.js`
- Modify: `frontend/src/styles/main.css`

**Interfaces:**

- Employee task records always come from the backend; no synthetic `demo-*` tasks remain.
- Employee submissions send real `File` objects through multipart.
- Department users receive confirmation and rejection actions.
- Rejection form requires reason and a valid new date.
- Task details display actions and attachments by permission.

- [ ] **Step 1: Write failing roster and status tests**

Verify custom task names become generic task cards, missing backend tasks are not fabricated, and state labels cover all four statuses.

- [ ] **Step 2: Run the tests to verify they fail**

```powershell
npm test
```

Expected: tests fail because the current roster always creates five demo tasks.

- [ ] **Step 3: Make the employee workspace backend-driven**

Keep the current layout and known five task definitions as presentation metadata. Match records by `taskName`; use a generic definition for unknown tasks; never generate a `demo-*` task ID.

- [ ] **Step 4: Implement real submission and rejection display**

Send `FormData` with `note` and all selected `File` objects. Show `待员工处理`, `待部门确认`, `已退回`, and `已完成`; render rejection reason, current due date, and submission history.

- [ ] **Step 5: Implement the department review experience**

Use the same task page for `DEPARTMENT`, hiding employee-only content and showing:

- task materials and history;
- `确认完成`;
- `退回` with required reason and new due date;
- department overdue list.

- [ ] **Step 6: Update HR detail and statistics displays**

Show `assignedDept`, current due date, status, confirmation person, confirmation time, and action history. Keep the task page read-only for HR.

- [ ] **Step 7: Run frontend tests and build**

```powershell
npm test
npm run build
```

Expected: all tests pass and Vite builds successfully.

- [ ] **Step 8: Commit**

```powershell
git add frontend
git commit -m "feat: complete employee and department task workflows"
```

### Task 8: Documentation, Regression, and End-to-End Verification

**Files:**

- Modify: `README.md`
- Modify: `启动说明.md`
- Modify: `docs/TEST-CASES.md`
- Modify: `docs/PROJECT-FOLLOW-UP.md`
- Modify: `sql/onboarding_sys.sql`

**Interfaces:**

- Documentation matches four-state workflow, department-owner management, file storage, and final operating steps.
- Test cases include the full HR -> employee -> department -> HR closure.

- [ ] **Step 1: Update setup and acceptance documentation**

Document `ONBOARDING_UPLOAD_DIR`, supported file types, 10 MB per file, 10 files per submission, department-owner login, confirm/reject flow, and overdue rules.

- [ ] **Step 2: Update test cases**

Add cases for account management, submission, confirmation, rejection, resubmission, overdue scenarios, authorization, attachment access, and concurrency.

- [ ] **Step 3: Run the complete backend suite**

```powershell
mvn clean test
```

Expected: all tests pass.

- [ ] **Step 4: Run the complete frontend suite and build**

```powershell
cd frontend
npm test
npm run build
```

Expected: all tests pass and Vite builds successfully.

- [ ] **Step 5: Run the end-to-end acceptance flow**

1. HR creates `研发部` owner with phone `13900000001`.
2. HR creates an employee with a valid phone and past entry date.
3. Employee logs in, uploads a PDF, and submits.
4. Department owner logs in, sees the task, and rejects it with a future due date.
5. Employee sees `已退回、待补交`, then resubmits.
6. Department owner confirms and HR verifies confirmation person/time.
7. HR archives the employee after all tasks are complete.

- [ ] **Step 6: Commit**

```powershell
git add README.md 启动说明.md docs sql
git commit -m "docs: document final task confirmation workflow"
```
