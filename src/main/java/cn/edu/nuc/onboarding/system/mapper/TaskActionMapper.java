package cn.edu.nuc.onboarding.system.mapper;

import cn.edu.nuc.onboarding.system.entity.TaskAction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskActionMapper {

    int insert(TaskAction action);

    List<TaskAction> selectByTaskId(@Param("taskId") Integer taskId);
}
