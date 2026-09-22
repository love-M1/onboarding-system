package cn.edu.nuc.onboarding.system.controller;

import cn.edu.nuc.onboarding.system.common.ApiResponse;
import cn.edu.nuc.onboarding.system.common.PageResult;
import cn.edu.nuc.onboarding.system.service.FileStorageService.StoredFile;
import cn.edu.nuc.onboarding.system.service.StatService;
import cn.edu.nuc.onboarding.system.service.TaskService;
import cn.edu.nuc.onboarding.system.vo.EmpTaskVO;
import cn.edu.nuc.onboarding.system.vo.TaskDetailVO;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final StatService statService;

    public TaskController(TaskService taskService, StatService statService) {
        this.taskService = taskService;
        this.statService = statService;
    }

    @GetMapping
    public ApiResponse<PageResult<EmpTaskVO>> list(
            @RequestParam(required = false) Integer empId,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ApiResponse.success(taskService.listTasks(empId, department, status, pageNum, pageSize));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<TaskDetailVO> detail(@PathVariable Integer taskId) {
        return ApiResponse.success(taskService.getTaskDetail(taskId));
    }

    @PostMapping(value = "/{taskId}/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TaskDetailVO> submit(
            @PathVariable Integer taskId,
            @RequestParam(required = false) String note,
            @RequestParam(value = "files", required = false) List<MultipartFile> files
    ) {
        return ApiResponse.success("任务已提交，等待部门确认", taskService.submitTask(taskId, note, files));
    }

    @PostMapping("/{taskId}/confirm")
    public ApiResponse<TaskDetailVO> confirm(@PathVariable Integer taskId) {
        return ApiResponse.success("任务已确认完成", taskService.confirmTask(taskId));
    }

    @PostMapping("/{taskId}/reject")
    public ApiResponse<TaskDetailVO> reject(
            @PathVariable Integer taskId,
            @RequestBody RejectTaskDTO dto
    ) {
        return ApiResponse.success(
                "任务已退回，等待员工重新提交",
                taskService.rejectTask(taskId, dto.reason(), dto.newDueDate())
        );
    }

    @GetMapping("/{taskId}/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Integer taskId,
            @PathVariable Integer attachmentId
    ) {
        StoredFile storedFile = taskService.openAttachment(taskId, attachmentId);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(storedFile.contentType());
        } catch (IllegalArgumentException exception) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(storedFile.originalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(storedFile.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(storedFile.path()));
    }

    public record RejectTaskDTO(String reason, LocalDate newDueDate) {
    }

    @GetMapping("/overdue")
    public ApiResponse<List<EmpTaskVO>> overdue() {
        return ApiResponse.success(statService.listOverdueTasks());
    }
}
