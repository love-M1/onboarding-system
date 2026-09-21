package cn.edu.nuc.onboarding.system.controller;

import cn.edu.nuc.onboarding.system.common.ApiResponse;
import cn.edu.nuc.onboarding.system.common.PageResult;
import cn.edu.nuc.onboarding.system.service.StatService;
import cn.edu.nuc.onboarding.system.service.TaskService;
import cn.edu.nuc.onboarding.system.vo.EmpTaskVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/{taskId}/finish")
    public ApiResponse<EmpTaskVO> finish(@PathVariable Integer taskId) {
        return ApiResponse.success("任务已确认完成", taskService.finishTask(taskId));
    }

    @GetMapping("/overdue")
    public ApiResponse<List<EmpTaskVO>> overdue() {
        return ApiResponse.success(statService.listOverdueTasks());
    }
}
