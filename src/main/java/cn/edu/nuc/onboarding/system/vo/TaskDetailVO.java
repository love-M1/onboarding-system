package cn.edu.nuc.onboarding.system.vo;

import java.util.List;

public record TaskDetailVO(
        EmpTaskVO task,
        List<TaskActionVO> actions
) {
}
