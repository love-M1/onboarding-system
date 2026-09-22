DROP TABLE IF EXISTS task_attachment;
DROP TABLE IF EXISTS task_action;
DROP TABLE IF EXISTS auth_session;
DROP TABLE IF EXISTS phone_verification;
DROP TABLE IF EXISTS user_account;
DROP TABLE IF EXISTS emp_task;
DROP TABLE IF EXISTS task_template;
DROP TABLE IF EXISTS employee;

CREATE TABLE employee (
    empId INT AUTO_INCREMENT PRIMARY KEY,
    empName VARCHAR(50) NOT NULL,
    empPhone VARCHAR(20),
    empDepartment VARCHAR(50),
    empPosition VARCHAR(50),
    entryTime TIMESTAMP,
    isArchived TINYINT NOT NULL DEFAULT 0,
    createTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (empPhone)
);

CREATE TABLE user_account (
    accountId INT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    passwordHash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    empId INT,
    displayName VARCHAR(50) NOT NULL,
    department VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    createTime TIMESTAMP NOT NULL,
    updateTime TIMESTAMP NOT NULL,
    UNIQUE (phone),
    UNIQUE (empId),
    CONSTRAINT fk_user_account_employee FOREIGN KEY (empId) REFERENCES employee(empId) ON DELETE CASCADE
);

CREATE TABLE phone_verification (
    verificationId INT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    code VARCHAR(6) NOT NULL,
    expiresAt TIMESTAMP NOT NULL,
    usedAt TIMESTAMP,
    attemptCount INT NOT NULL DEFAULT 0,
    createTime TIMESTAMP NOT NULL
);

CREATE INDEX idx_phone_verification_phone_time ON phone_verification(phone, createTime);

CREATE TABLE auth_session (
    sessionId INT AUTO_INCREMENT PRIMARY KEY,
    tokenHash VARCHAR(64) NOT NULL,
    accountId INT NOT NULL,
    expiresAt TIMESTAMP NOT NULL,
    createTime TIMESTAMP NOT NULL,
    lastAccessTime TIMESTAMP NOT NULL,
    UNIQUE (tokenHash),
    CONSTRAINT fk_auth_session_account FOREIGN KEY (accountId) REFERENCES user_account(accountId) ON DELETE CASCADE
);

CREATE INDEX idx_auth_session_account ON auth_session(accountId);

CREATE TABLE task_template (
    tplId INT AUTO_INCREMENT PRIMARY KEY,
    taskName VARCHAR(100) NOT NULL,
    dutyDept VARCHAR(50),
    offsetDay INT
);

CREATE TABLE emp_task (
    taskId INT AUTO_INCREMENT PRIMARY KEY,
    empId INT NOT NULL,
    tplId INT NOT NULL,
    assignedDept VARCHAR(50) NOT NULL,
    baseDueDate DATE NOT NULL,
    currentDueDate DATE NOT NULL,
    currentSubmissionId INT,
    taskStatus TINYINT NOT NULL DEFAULT 0,
    finishByAccountId INT,
    finishByName VARCHAR(50),
    finishTime TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_emp_task_employee FOREIGN KEY (empId) REFERENCES employee(empId),
    CONSTRAINT fk_emp_task_template FOREIGN KEY (tplId) REFERENCES task_template(tplId)
);

CREATE INDEX idx_emp_task_emp_id ON emp_task(empId);
CREATE INDEX idx_emp_task_tpl_id ON emp_task(tplId);
CREATE INDEX idx_emp_task_assigned_dept ON emp_task(assignedDept);
CREATE INDEX idx_emp_task_status ON emp_task(taskStatus);
CREATE INDEX idx_emp_task_current_due_date ON emp_task(currentDueDate);

CREATE TABLE task_action (
    actionId INT AUTO_INCREMENT PRIMARY KEY,
    taskId INT NOT NULL,
    actionType VARCHAR(20) NOT NULL,
    actorAccountId INT NOT NULL,
    actorNameSnapshot VARCHAR(50) NOT NULL,
    actionTime TIMESTAMP NOT NULL,
    reason VARCHAR(500),
    newDueDate DATE,
    relatedSubmissionId INT,
    CONSTRAINT fk_task_action_task FOREIGN KEY (taskId) REFERENCES emp_task(taskId) ON DELETE CASCADE,
    CONSTRAINT fk_task_action_actor FOREIGN KEY (actorAccountId) REFERENCES user_account(accountId)
);

CREATE INDEX idx_task_action_task_time ON task_action(taskId, actionTime);

CREATE TABLE task_attachment (
    attachmentId INT AUTO_INCREMENT PRIMARY KEY,
    taskId INT NOT NULL,
    actionId INT NOT NULL,
    originalName VARCHAR(255) NOT NULL,
    storageName VARCHAR(100) NOT NULL,
    relativePath VARCHAR(500) NOT NULL,
    contentType VARCHAR(100) NOT NULL,
    fileSize BIGINT NOT NULL,
    uploaderAccountId INT NOT NULL,
    uploadTime TIMESTAMP NOT NULL,
    UNIQUE (storageName),
    CONSTRAINT fk_task_attachment_task FOREIGN KEY (taskId) REFERENCES emp_task(taskId) ON DELETE CASCADE,
    CONSTRAINT fk_task_attachment_action FOREIGN KEY (actionId) REFERENCES task_action(actionId) ON DELETE CASCADE,
    CONSTRAINT fk_task_attachment_uploader FOREIGN KEY (uploaderAccountId) REFERENCES user_account(accountId)
);

CREATE INDEX idx_task_attachment_task ON task_attachment(taskId);
CREATE INDEX idx_task_attachment_action ON task_attachment(actionId);
