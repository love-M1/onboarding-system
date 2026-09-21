# Onboarding System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the complete, locally runnable onboarding task collaboration system described in `新员工入职任务协同系统系统设计文档.docx`.

**Architecture:** The repository root remains the Spring Boot backend. A standalone Vue 3 and Vite frontend lives under `frontend/`. MySQL 8 stores employee, template, and employee-task records; due dates and overdue status are derived through SQL joins rather than persisted fields.

**Tech Stack:** Java 21, Spring Boot 3.2.x, Spring MVC, Bean Validation, MyBatis XML, MySQL 8, H2 test database, Vue 3, Vite, Vue Router, Pinia, Axios, Element Plus, Maven, npm.

**Spec:** `C:/Users/郭哲/Desktop/新员工入职任务协同系统系统设计文档.docx`

## Global Constraints

- Use table and column names exactly as specified: `employee`, `task_template`, `emp_task`, `empId`, `entryTime`, `isArchived`, `tplId`, `offsetDay`, `taskStatus`, `finishTime`.
- All endpoints use `/api`, JSON, and the `{code,message,data}` response contract.
- Persist only task status `0` and `1`; derive overdue status from `entryTime + offsetDay < CURRENT_DATE`.
- Automatically generate tasks when an employee is created; no tasks are generated when the template list is empty.
- Archive and delete operations must reject an employee that still has unfinished tasks.
- Archived employee tasks cannot be confirmed.
- The final source tree must not include `target/`, `node_modules/`, `dist/`, `__pycache__/`, logs, IDE metadata, or local secrets.
- The README must contain exact Windows setup, database initialization, backend startup, frontend startup, and verification commands.

---

### Task 1: Database and runtime configuration

**Files:**
- Create: `sql/onboarding_sys.sql`
- Create: `src/main/resources/application.yml`
- Create: `src/test/resources/application-test.yml`
- Create: `src/test/resources/schema-test.sql`
- Create: `src/test/resources/data-test.sql`
- Modify: `pom.xml`
- Modify: `.gitignore`

**Interfaces:**
- Produces: MySQL schema named `onboarding_sys`; environment variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`, `LOG_FILE`.
- Produces: H2-backed Spring test profile named `test`.

- [ ] Replace the MyBatis Plus dependency with `mybatis-spring-boot-starter`.
- [ ] Add Bean Validation and H2 test dependencies.
- [ ] Create exact MySQL DDL, indexes, foreign keys, and template seed data.
- [ ] Configure connection values through environment variables with no committed password.
- [ ] Run `mvn test` and verify the test profile boots against H2.

### Task 2: Shared contracts and validation

**Files:**
- Create: `src/main/java/cn/edu/nuc/onboarding/system/common/ApiResponse.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/common/ErrorCode.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/common/BizException.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/common/PageResult.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/common/GlobalExceptionHandler.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/config/WebConfig.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/security/AuthContext.java`
- Create: `src/main/java/cn/edu/nuc/onboarding/system/security/RoleInterceptor.java`

**Interfaces:**
- Produces: `ApiResponse.success(T)`, `ApiResponse.failure(int,String)`, `PageResult<T>`, `ErrorCode`, `BizException`.
- Produces headers `X-Role`, `X-Department`, and `X-Operator`; roles are `HR` and `DEPARTMENT`.

- [ ] Write failing tests for business-error serialization and required-role rejection.
- [ ] Implement the response envelope and English package names.
- [ ] Map document error codes `1001` through `4003` to user-facing Chinese messages.
- [ ] Add CORS for the Vite development server and a request-scoped `ThreadLocal` authentication context.
- [ ] Verify tests and run a context-load test.

### Task 3: Template module

**Files:**
- Create: `entity/TaskTemplate.java`
- Create: `dto/TemplateSaveDTO.java`
- Create: `mapper/TaskTemplateMapper.java`
- Create: `resources/mapper/TaskTemplateMapper.xml`
- Create: `service/TemplateService.java`
- Create: `service/impl/TemplateServiceImpl.java`
- Create: `controller/TemplateController.java`
- Test: `src/test/java/cn/edu/nuc/onboarding/system/TemplateServiceTest.java`

**Interfaces:**
- Produces endpoints `GET /api/templates`, `GET /api/templates/{tplId}`, `POST /api/templates`, `PUT /api/templates/{tplId}`.
- Produces `listTemplates(String taskName, String dutyDept)`, `getTemplate(Integer)`, `createTemplate(TemplateSaveDTO)`, `updateTemplate(Integer, TemplateSaveDTO)`.

- [ ] Write failing tests for non-empty task name, non-empty department, and non-negative offset.
- [ ] Implement XML queries and list ordering by `tplId`.
- [ ] Return error code `2001` when a template ID does not exist.
- [ ] Verify all template tests pass.

### Task 4: Employee creation and automatic task generation

**Files:**
- Create: `entity/Employee.java`
- Create: `entity/EmpTask.java`
- Create: `dto/EmployeeCreateDTO.java`
- Create: `vo/EmployeeDetailVO.java`
- Create: `vo/CreateEmployeeResultVO.java`
- Create: `mapper/EmployeeMapper.java`
- Create: `mapper/EmpTaskMapper.java`
- Create: `resources/mapper/EmployeeMapper.xml`
- Create: `resources/mapper/EmpTaskMapper.xml`
- Create: `service/EmployeeService.java`
- Create: `service/impl/EmployeeServiceImpl.java`
- Create: `controller/EmployeeController.java`
- Test: `src/test/java/cn/edu/nuc/onboarding/system/EmployeeCreationTest.java`

**Interfaces:**
- Produces endpoints `POST /api/employees`, `GET /api/employees`, `GET /api/employees/{empId}`, `DELETE /api/employees/{empId}`, `POST /api/employees/{empId}/archive`.
- Produces `createEmployee`, `listEmployees`, `getEmployeeDetail`, `archiveEmployee`, `deleteEmployee`.
- Produces task records with `empId`, `tplId`, `taskStatus=0`, and no persisted due date.

- [ ] Write failing tests for required fields `1001` through `1004`.
- [ ] Write a failing test proving a successful employee insert and one task per template occur in one transaction.
- [ ] Write a failing test proving an empty template table creates no tasks and returns a clear message.
- [ ] Implement batch insert and derived due-date query mapping.
- [ ] Verify employee creation tests pass.

### Task 5: Task execution and progress module

**Files:**
- Create: `vo/EmpTaskVO.java`
- Create: `vo/EmployeeStatsVO.java`
- Create: `vo/DepartmentStatsVO.java`
- Create: `service/TaskService.java`
- Create: `service/impl/TaskServiceImpl.java`
- Create: `service/StatService.java`
- Create: `service/impl/StatServiceImpl.java`
- Create: `controller/TaskController.java`
- Create: `controller/StatController.java`
- Test: `src/test/java/cn/edu/nuc/onboarding/system/TaskWorkflowTest.java`

**Interfaces:**
- Produces endpoints `GET /api/tasks`, `POST /api/tasks/{taskId}/finish`, `GET /api/tasks/overdue`, `GET /api/stats/employees/{empId}`, `GET /api/stats/departments`.
- Produces the exact overdue rule: unfinished and due date strictly earlier than `CURRENT_DATE`.

- [ ] Write failing tests for task missing `3001`, duplicate finish `3002`, pre-entry finish `3003`, and archived finish `3004`.
- [ ] Write failing tests proving the due-date boundary is not overdue on the due date and is overdue the next day.
- [ ] Write failing tests for employee and department summaries.
- [ ] Implement row-locked conditional update `WHERE taskId=? AND taskStatus=0`.
- [ ] Verify task workflow and statistics tests pass.

### Task 6: Archive and deletion rules

**Files:**
- Modify: `service/impl/EmployeeServiceImpl.java`
- Test: `src/test/java/cn/edu/nuc/onboarding/system/ArchiveWorkflowTest.java`

**Interfaces:**
- Produces error code `4001` for archive with unfinished tasks and `4002` for deletion with unfinished tasks.
- Produces read-only archived employees; deletion of archived employees is rejected with `4004`.

- [ ] Write failing tests for archive rejection and deletion rejection with unfinished tasks.
- [ ] Write a failing test proving archive succeeds after every task is finished.
- [ ] Write a failing test proving archived employees and their tasks cannot be deleted.
- [ ] Implement the checks inside transactions.
- [ ] Verify archive workflow tests pass.

### Task 7: Vue 3 frontend

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.js`
- Create: `frontend/index.html`
- Create: `frontend/src/main.js`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/router/index.js`
- Create: `frontend/src/stores/auth.js`
- Create: `frontend/src/api/http.js`
- Create: `frontend/src/api/templates.js`
- Create: `frontend/src/api/employees.js`
- Create: `frontend/src/api/tasks.js`
- Create: `frontend/src/api/stats.js`
- Create: `frontend/src/layouts/AppLayout.vue`
- Create: `frontend/src/views/LoginView.vue`
- Create: `frontend/src/views/TemplateView.vue`
- Create: `frontend/src/views/EmployeeView.vue`
- Create: `frontend/src/views/EmployeeDetailView.vue`
- Create: `frontend/src/views/TaskView.vue`
- Create: `frontend/src/views/StatsView.vue`
- Create: `frontend/src/views/ArchiveView.vue`
- Create: `frontend/src/styles/main.css`

**Interfaces:**
- Stores login role and department in Pinia and localStorage.
- Axios injects `X-Role`, `X-Department`, and `X-Operator`.
- Routes implement the page list and jump relationships in document table 8-1.

- [ ] Implement role login for `HR` and `DEPARTMENT`.
- [ ] Implement template CRUD, employee creation/detail, task confirmation, statistics, overdue list, archive, and delete.
- [ ] Display pending, overdue, and completed states with distinct tags.
- [ ] Disable invalid task actions and explain the reason.
- [ ] Run `npm run build` and verify the production bundle succeeds.

### Task 8: Documentation and local delivery

**Files:**
- Create: `README.md`
- Create: `scripts/init-db.ps1`
- Create: `scripts/run-backend.ps1`
- Create: `scripts/run-frontend.ps1`
- Create: `docs/TEST-CASES.md`
- Modify: `.gitignore`

**Interfaces:**
- Produces exact Windows commands for database initialization, backend startup, frontend startup, login roles, and test execution.

- [ ] Document Java 21, Maven, Node.js, npm, and MySQL 8 prerequisites.
- [ ] Document environment variables and both HR and department login flows.
- [ ] Provide PowerShell helpers that do not store passwords in source.
- [ ] Run backend tests, frontend production build, and a real MySQL smoke test when credentials are available.
- [ ] Remove `target/`, `frontend/node_modules/`, `frontend/dist/`, logs, and generated caches before delivery.
- [ ] Verify the final tree contains only source, tests, SQL, scripts, and documentation.

## Self-Review

- The plan covers all endpoints from document table 4-3.
- The plan covers all business rules R1 through R10 from document table 6-1.
- The plan uses the exact table and field names from chapter 3.
- The plan covers all pages and role flows from tables 8-1 and 8-2.
- The plan includes local startup, testing, and source-cleanup requirements.
