package cn.edu.nuc.onboarding.system.service;

import cn.edu.nuc.onboarding.system.common.PageResult;
import cn.edu.nuc.onboarding.system.vo.EmpTaskVO;

public interface TaskService {

    PageResult<EmpTaskVO> listTasks(
            Integer empId,
            String department,
            String status,
            int pageNum,
            int pageSize
    );

    EmpTaskVO finishTask(Integer taskId);
}
