package cn.edu.nuc.onboarding.system.common;

public final class ErrorCode {

    public static final int SUCCESS = 200;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int SERVER_ERROR = 500;

    public static final int EMPLOYEE_NAME_REQUIRED = 1001;
    public static final int EMPLOYEE_DEPARTMENT_REQUIRED = 1002;
    public static final int EMPLOYEE_POSITION_REQUIRED = 1003;
    public static final int EMPLOYEE_ENTRY_TIME_REQUIRED = 1004;
    public static final int EMPLOYEE_ENTRY_TIME_INVALID = 1005;
    public static final int EMPLOYEE_PHONE_REQUIRED = 1006;
    public static final int EMPLOYEE_PHONE_INVALID = 1007;
    public static final int EMPLOYEE_PHONE_DUPLICATE = 1008;

    public static final int TEMPLATE_NOT_FOUND = 2001;

    public static final int TASK_NOT_FOUND = 3001;
    public static final int TASK_ALREADY_FINISHED = 3002;
    public static final int ENTRY_TIME_NOT_REACHED = 3003;
    public static final int EMPLOYEE_ARCHIVED = 3004;
    public static final int TASK_SUBMISSION_NOT_ALLOWED = 3005;
    public static final int TASK_SUBMISSION_FILE_REQUIRED = 3006;
    public static final int TASK_FILE_TYPE_INVALID = 3007;
    public static final int TASK_FILE_TOO_LARGE = 3008;
    public static final int TASK_TOO_MANY_FILES = 3009;
    public static final int TASK_ATTACHMENT_NOT_FOUND = 3010;

    public static final int UNFINISHED_TASKS_ARCHIVE = 4001;
    public static final int UNFINISHED_TASKS_DELETE = 4002;
    public static final int EMPLOYEE_NOT_FOUND = 4003;
    public static final int ARCHIVED_EMPLOYEE_DELETE = 4004;

    public static final int PHONE_INVALID = 5001;
    public static final int EMPLOYEE_NOT_REGISTERABLE = 5002;
    public static final int ACCOUNT_ALREADY_EXISTS = 5003;
    public static final int VERIFICATION_CODE_INVALID = 5004;
    public static final int VERIFICATION_CODE_EXPIRED = 5005;
    public static final int VERIFICATION_CODE_USED = 5006;
    public static final int VERIFICATION_CODE_TOO_MANY_ATTEMPTS = 5007;
    public static final int VERIFICATION_CODE_TOO_FREQUENT = 5008;
    public static final int PASSWORD_INVALID = 5009;
    public static final int LOGIN_FAILED = 5010;
    public static final int ACCOUNT_DISABLED = 5011;
    public static final int DEPARTMENT_OWNER_NOT_FOUND = 5012;
    public static final int DEPARTMENT_OWNER_EXISTS = 5013;
    public static final int DEPARTMENT_OWNER_STATUS_INVALID = 5014;

    private ErrorCode() {
    }
}
