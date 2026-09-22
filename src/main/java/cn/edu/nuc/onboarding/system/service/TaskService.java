package cn.edu.nuc.onboarding.system.service;

import cn.edu.nuc.onboarding.system.common.PageResult;
import cn.edu.nuc.onboarding.system.service.FileStorageService.StoredFile;
import cn.edu.nuc.onboarding.system.vo.EmpTaskVO;
import cn.edu.nuc.onboarding.system.vo.TaskDetailVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.time.LocalDate;

public interface TaskService {

    PageResult<EmpTaskVO> listTasks(
            Integer empId,
            String department,
            String status,
            int pageNum,
            int pageSize
    );

    TaskDetailVO confirmTask(Integer taskId);

    TaskDetailVO rejectTask(Integer taskId, String reason, LocalDate newDueDate);

    TaskDetailVO getTaskDetail(Integer taskId);

    TaskDetailVO submitTask(Integer taskId, String note, List<MultipartFile> files);

    StoredFile openAttachment(Integer taskId, Integer attachmentId);
}
