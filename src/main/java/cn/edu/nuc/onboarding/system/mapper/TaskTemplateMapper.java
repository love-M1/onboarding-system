package cn.edu.nuc.onboarding.system.mapper;

import cn.edu.nuc.onboarding.system.entity.TaskTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskTemplateMapper {

    List<TaskTemplate> selectByCondition(
            @Param("taskName") String taskName,
            @Param("dutyDept") String dutyDept
    );

    TaskTemplate selectById(@Param("tplId") Integer tplId);

    int insert(TaskTemplate template);

    int update(TaskTemplate template);
}
