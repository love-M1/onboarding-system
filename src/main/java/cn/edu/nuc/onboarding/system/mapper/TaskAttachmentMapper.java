package cn.edu.nuc.onboarding.system.mapper;

import cn.edu.nuc.onboarding.system.entity.TaskAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskAttachmentMapper {

    int insertBatch(@Param("list") List<TaskAttachment> attachments);

    List<TaskAttachment> selectByActionId(@Param("actionId") Integer actionId);

    List<TaskAttachment> selectByTaskId(@Param("taskId") Integer taskId);

    TaskAttachment selectById(@Param("attachmentId") Integer attachmentId);
}
