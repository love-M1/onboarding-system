package cn.edu.nuc.onboarding.system.service.impl;

import cn.edu.nuc.onboarding.system.common.BizException;
import cn.edu.nuc.onboarding.system.common.ErrorCode;
import cn.edu.nuc.onboarding.system.common.PageResult;
import cn.edu.nuc.onboarding.system.entity.TaskAction;
import cn.edu.nuc.onboarding.system.entity.TaskAttachment;
import cn.edu.nuc.onboarding.system.mapper.EmpTaskMapper;
import cn.edu.nuc.onboarding.system.mapper.TaskActionMapper;
import cn.edu.nuc.onboarding.system.mapper.TaskAttachmentMapper;
import cn.edu.nuc.onboarding.system.security.AuthContext;
import cn.edu.nuc.onboarding.system.security.AuthUser;
import cn.edu.nuc.onboarding.system.service.FileStorageService;
import cn.edu.nuc.onboarding.system.service.FileStorageService.StoredFile;
import cn.edu.nuc.onboarding.system.service.TaskService;
import cn.edu.nuc.onboarding.system.vo.EmpTaskVO;
import cn.edu.nuc.onboarding.system.vo.TaskActionVO;
import cn.edu.nuc.onboarding.system.vo.TaskAttachmentVO;
import cn.edu.nuc.onboarding.system.vo.TaskDetailVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class TaskServiceImpl implements TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);
    private static final int MAX_ATTACHMENT_COUNT = 10;

    private final EmpTaskMapper empTaskMapper;
    private final TaskActionMapper taskActionMapper;
    private final TaskAttachmentMapper taskAttachmentMapper;
    private final FileStorageService fileStorageService;

    public TaskServiceImpl(
            EmpTaskMapper empTaskMapper,
            TaskActionMapper taskActionMapper,
            TaskAttachmentMapper taskAttachmentMapper,
            FileStorageService fileStorageService
    ) {
        this.empTaskMapper = empTaskMapper;
        this.taskActionMapper = taskActionMapper;
        this.taskAttachmentMapper = taskAttachmentMapper;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<EmpTaskVO> listTasks(
            Integer empId,
            String department,
            String status,
            int pageNum,
            int pageSize
    ) {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = pageSize < 1 ? 10 : Math.min(pageSize, 100);
        String safeStatus = normalizeStatus(status);
        Integer safeEmpId = resolveEmployeeId(empId);
        String safeDepartment = resolveDepartment(department);
        int offset = (safePageNum - 1) * safePageSize;
        List<EmpTaskVO> records = empTaskMapper.selectTaskPage(
                safeEmpId, safeDepartment, safeStatus, offset, safePageSize);
        records.forEach(this::applyDepartmentPermission);
        long total = empTaskMapper.countTaskPage(safeEmpId, safeDepartment, safeStatus);
        return new PageResult<>(safePageNum, safePageSize, total, records);
    }

    @Override
    @Transactional
    public EmpTaskVO finishTask(Integer taskId) {
        EmpTaskVO task = empTaskMapper.selectTaskDetail(taskId);
        if (task == null) {
            throw new BizException(ErrorCode.TASK_NOT_FOUND, "任务不存在");
        }
        if (Boolean.TRUE.equals(task.getArchived())) {
            throw new BizException(ErrorCode.EMPLOYEE_ARCHIVED, "档案已归档，不能修改任务状态");
        }
        if (Integer.valueOf(1).equals(task.getTaskStatus())) {
            throw new BizException(ErrorCode.TASK_ALREADY_FINISHED, "该任务已确认完成，不能重复确认");
        }

        AuthUser user = AuthContext.get();
        if (user != null && user.isDepartment()
                && !user.department().equals(task.getDutyDept())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权确认其他部门的任务");
        }
        if (user != null && user.isEmployee()
                && !Objects.equals(user.empId(), task.getEmpId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权确认其他员工的任务");
        }
        if (task.getEntryTime() != null
                && task.getEntryTime().toLocalDate().isAfter(LocalDate.now())) {
            throw new BizException(ErrorCode.ENTRY_TIME_NOT_REACHED, "入职日期尚未到来，暂不能确认完成");
        }

        int updated = empTaskMapper.finishTask(taskId, LocalDateTime.now());
        if (updated == 0) {
            throw new BizException(ErrorCode.TASK_ALREADY_FINISHED, "该任务已确认完成，不能重复确认");
        }
        EmpTaskVO result = empTaskMapper.selectTaskDetail(taskId);
        log.info("Task finished: taskId={}, operator={}", taskId, user == null ? "unknown" : user.operator());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDetailVO getTaskDetail(Integer taskId) {
        EmpTaskVO task = requireTask(taskId);
        requireTaskAccess(task);
        return toTaskDetail(task);
    }

    @Override
    @Transactional
    public TaskDetailVO submitTask(Integer taskId, String note, List<MultipartFile> files) {
        EmpTaskVO task = requireTask(taskId);
        AuthUser user = requireEmployeeOwner(task);
        if (Boolean.TRUE.equals(task.getArchived())) {
            throw new BizException(ErrorCode.EMPLOYEE_ARCHIVED, "档案已归档，不能提交任务");
        }
        if (!Integer.valueOf(0).equals(task.getTaskStatus())
                && !Integer.valueOf(3).equals(task.getTaskStatus())) {
            throw new BizException(ErrorCode.TASK_SUBMISSION_NOT_ALLOWED, "当前任务状态不能提交");
        }
        if (task.getEntryTime() != null
                && task.getEntryTime().toLocalDate().isAfter(LocalDate.now())) {
            throw new BizException(ErrorCode.ENTRY_TIME_NOT_REACHED, "入职日期尚未到来，暂不能提交");
        }
        if (files == null || files.isEmpty()) {
            throw new BizException(ErrorCode.TASK_SUBMISSION_FILE_REQUIRED, "请至少上传一个附件");
        }
        if (files.size() > MAX_ATTACHMENT_COUNT) {
            throw new BizException(ErrorCode.TASK_TOO_MANY_FILES, "单次最多上传10个附件");
        }

        List<StoredFile> storedFiles = new ArrayList<>(files.size());
        for (MultipartFile file : files) {
            storedFiles.add(fileStorageService.store(file));
        }

        LocalDateTime now = LocalDateTime.now();
        TaskAction action = new TaskAction();
        action.setTaskId(taskId);
        action.setActionType("SUBMIT");
        action.setActorAccountId(user.accountId());
        action.setActorNameSnapshot(user.operator());
        action.setActionTime(now);
        action.setReason(note == null || note.isBlank() ? null : note.trim());
        taskActionMapper.insert(action);

        int updated = empTaskMapper.submitTask(
                taskId,
                action.getActionId(),
                task.getVersion() == null ? 0 : task.getVersion()
        );
        if (updated == 0) {
            throw new BizException(ErrorCode.TASK_SUBMISSION_NOT_ALLOWED, "任务状态已变化，请刷新后重试");
        }

        List<TaskAttachment> attachments = storedFiles.stream()
                .map(storedFile -> toAttachment(taskId, action.getActionId(), user.accountId(), storedFile))
                .toList();
        taskAttachmentMapper.insertBatch(attachments);
        log.info("Task submitted: taskId={}, accountId={}, fileCount={}",
                taskId, user.accountId(), attachments.size());
        return toTaskDetail(requireTask(taskId));
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFile openAttachment(Integer taskId, Integer attachmentId) {
        EmpTaskVO task = requireTask(taskId);
        requireTaskAccess(task);
        TaskAttachment attachment = taskAttachmentMapper.selectById(attachmentId);
        if (attachment == null || !Objects.equals(attachment.getTaskId(), taskId)) {
            throw new BizException(ErrorCode.TASK_ATTACHMENT_NOT_FOUND, "附件不存在");
        }
        return fileStorageService.open(
                attachment.getRelativePath(),
                attachment.getOriginalName(),
                attachment.getContentType()
        );
    }

    private EmpTaskVO requireTask(Integer taskId) {
        EmpTaskVO task = empTaskMapper.selectTaskDetail(taskId);
        if (task == null) {
            throw new BizException(ErrorCode.TASK_NOT_FOUND, "任务不存在");
        }
        return task;
    }

    private AuthUser requireEmployeeOwner(EmpTaskVO task) {
        AuthUser user = AuthContext.get();
        if (user == null || !user.isEmployee() || !Objects.equals(user.empId(), task.getEmpId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权提交其他员工的任务");
        }
        return user;
    }

    private void requireTaskAccess(EmpTaskVO task) {
        AuthUser user = AuthContext.get();
        if (user == null || user.isHr()) {
            return;
        }
        if (user.isEmployee() && Objects.equals(user.empId(), task.getEmpId())) {
            return;
        }
        if (user.isDepartment() && Objects.equals(user.department(), task.getAssignedDept())) {
            return;
        }
        throw new BizException(ErrorCode.FORBIDDEN, "无权查看该任务");
    }

    private TaskDetailVO toTaskDetail(EmpTaskVO task) {
        List<TaskActionVO> actions = taskActionMapper.selectByTaskId(task.getTaskId()).stream()
                .map(action -> new TaskActionVO(
                        action.getActionId(),
                        action.getActionType(),
                        action.getActorAccountId(),
                        action.getActorNameSnapshot(),
                        action.getActionTime(),
                        action.getReason(),
                        action.getNewDueDate(),
                        action.getRelatedSubmissionId(),
                        taskAttachmentMapper.selectByActionId(action.getActionId()).stream()
                                .map(attachment -> toAttachmentVO(task.getTaskId(), attachment))
                                .toList()
                ))
                .toList();
        return new TaskDetailVO(task, actions);
    }

    private TaskAttachmentVO toAttachmentVO(Integer taskId, TaskAttachment attachment) {
        return new TaskAttachmentVO(
                attachment.getAttachmentId(),
                attachment.getTaskId(),
                attachment.getActionId(),
                attachment.getOriginalName(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getUploadTime(),
                "/api/tasks/" + taskId + "/attachments/" + attachment.getAttachmentId()
        );
    }

    private TaskAttachment toAttachment(
            Integer taskId,
            Integer actionId,
            Integer accountId,
            StoredFile storedFile
    ) {
        TaskAttachment attachment = new TaskAttachment();
        attachment.setTaskId(taskId);
        attachment.setActionId(actionId);
        attachment.setOriginalName(storedFile.originalName());
        attachment.setStorageName(storedFile.storageName());
        attachment.setRelativePath(storedFile.relativePath());
        attachment.setContentType(storedFile.contentType());
        attachment.setFileSize(storedFile.fileSize());
        attachment.setUploaderAccountId(accountId);
        attachment.setUploadTime(LocalDateTime.now());
        return attachment;
    }

    private void applyDepartmentPermission(EmpTaskVO task) {
        AuthUser user = AuthContext.get();
        if (user != null && user.isDepartment()
                && !user.department().equals(task.getDutyDept())) {
            task.setCanFinish(false);
        }
    }

    private String resolveDepartment(String department) {
        AuthUser user = AuthContext.get();
        if (user != null && user.isDepartment()) {
            return user.department();
        }
        return department == null ? null : department.trim();
    }

    private Integer resolveEmployeeId(Integer requestedEmpId) {
        AuthUser user = AuthContext.get();
        if (user == null || !user.isEmployee()) {
            return requestedEmpId;
        }
        if (user.empId() == null) {
            throw new BizException(ErrorCode.FORBIDDEN, "员工账号未绑定档案");
        }
        return user.empId();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return switch (status.trim().toLowerCase()) {
            case "0", "pending" -> "0";
            case "1", "finished", "completed" -> "1";
            case "overdue" -> "overdue";
            default -> throw new BizException(ErrorCode.BAD_REQUEST, "任务状态参数不正确");
        };
    }
}
