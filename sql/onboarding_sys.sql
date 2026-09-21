CREATE DATABASE IF NOT EXISTS onboarding_sys
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE onboarding_sys;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS auth_session;
DROP TABLE IF EXISTS phone_verification;
DROP TABLE IF EXISTS user_account;
DROP TABLE IF EXISTS emp_task;
DROP TABLE IF EXISTS task_template;
DROP TABLE IF EXISTS employee;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE employee (
    empId INT PRIMARY KEY AUTO_INCREMENT COMMENT '员工档案编号',
    empName VARCHAR(50) NOT NULL COMMENT '员工姓名',
    empPhone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    empDepartment VARCHAR(50) DEFAULT NULL COMMENT '所属部门',
    empPosition VARCHAR(50) DEFAULT NULL COMMENT '岗位名称',
    entryTime DATETIME DEFAULT NULL COMMENT '入职时间',
    isArchived TINYINT(1) NOT NULL DEFAULT 0 COMMENT '档案是否归档：0未归档，1已归档',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '档案创建时间',
    UNIQUE KEY uk_employee_phone (empPhone),
    INDEX idx_employee_department (empDepartment),
    INDEX idx_employee_archived (isArchived)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工档案表';

CREATE TABLE user_account (
    accountId INT PRIMARY KEY AUTO_INCREMENT COMMENT '账号编号',
    phone VARCHAR(20) NOT NULL COMMENT '手机号（工号）',
    passwordHash VARCHAR(100) NOT NULL COMMENT 'BCrypt密码哈希',
    role VARCHAR(20) NOT NULL COMMENT 'HR、EMPLOYEE或DEPARTMENT',
    empId INT DEFAULT NULL COMMENT '绑定的员工档案编号',
    displayName VARCHAR(50) NOT NULL COMMENT '显示名称',
    department VARCHAR(50) DEFAULT NULL COMMENT '账号所属部门',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE或DISABLED',
    createTime DATETIME NOT NULL COMMENT '账号创建时间',
    updateTime DATETIME NOT NULL COMMENT '账号更新时间',
    UNIQUE KEY uk_user_account_phone (phone),
    UNIQUE KEY uk_user_account_emp (empId),
    CONSTRAINT fk_user_account_employee
        FOREIGN KEY (empId) REFERENCES employee(empId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统账号表';

CREATE TABLE phone_verification (
    verificationId INT PRIMARY KEY AUTO_INCREMENT COMMENT '验证码编号',
    phone VARCHAR(20) NOT NULL COMMENT '验证手机号',
    code VARCHAR(6) NOT NULL COMMENT '6位验证码',
    expiresAt DATETIME NOT NULL COMMENT '过期时间',
    usedAt DATETIME DEFAULT NULL COMMENT '使用时间',
    attemptCount INT NOT NULL DEFAULT 0 COMMENT '失败校验次数',
    createTime DATETIME NOT NULL COMMENT '发送时间',
    INDEX idx_phone_verification_phone_time (phone, createTime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手机验证码表';

CREATE TABLE auth_session (
    sessionId INT PRIMARY KEY AUTO_INCREMENT COMMENT '会话编号',
    tokenHash VARCHAR(64) NOT NULL COMMENT '令牌SHA-256哈希',
    accountId INT NOT NULL COMMENT '账号编号',
    expiresAt DATETIME NOT NULL COMMENT '过期时间',
    createTime DATETIME NOT NULL COMMENT '创建时间',
    lastAccessTime DATETIME NOT NULL COMMENT '最后访问时间',
    UNIQUE KEY uk_auth_session_token_hash (tokenHash),
    INDEX idx_auth_session_account (accountId),
    CONSTRAINT fk_auth_session_account
        FOREIGN KEY (accountId) REFERENCES user_account(accountId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录会话表';

CREATE TABLE task_template (
    tplId INT PRIMARY KEY AUTO_INCREMENT COMMENT '任务模板编号',
    taskName VARCHAR(100) NOT NULL COMMENT '任务名称',
    dutyDept VARCHAR(50) DEFAULT NULL COMMENT '负责部门',
    offsetDay INT DEFAULT NULL COMMENT '入职后偏移执行天数',
    INDEX idx_template_duty_dept (dutyDept)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入职任务模板表';

CREATE TABLE emp_task (
    taskId INT PRIMARY KEY AUTO_INCREMENT COMMENT '员工任务主键',
    empId INT NOT NULL COMMENT '关联employee员工编号',
    tplId INT NOT NULL COMMENT '关联task_template任务模板编号',
    taskStatus TINYINT(1) NOT NULL DEFAULT 0 COMMENT '任务状态：0未完成，1已完成',
    finishTime DATETIME DEFAULT NULL COMMENT '任务完成时间',
    INDEX idx_emp_task_emp_id (empId),
    INDEX idx_emp_task_tpl_id (tplId),
    INDEX idx_emp_task_status (taskStatus),
    CONSTRAINT fk_emp_task_employee FOREIGN KEY (empId) REFERENCES employee(empId),
    CONSTRAINT fk_emp_task_template FOREIGN KEY (tplId) REFERENCES task_template(tplId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工个人任务表';

INSERT INTO task_template(taskName, dutyDept, offsetDay) VALUES
('提交入职材料', '人事部', 0),
('签署劳动合同', '人事部', 1),
('办理员工工牌', '行政部', 2),
('开通企业邮箱', '信息技术部', 1),
('领取办公设备', '行政部', 3);
