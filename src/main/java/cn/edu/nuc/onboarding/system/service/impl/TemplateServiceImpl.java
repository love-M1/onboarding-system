package cn.edu.nuc.onboarding.system.service.impl;

import cn.edu.nuc.onboarding.system.common.BizException;
import cn.edu.nuc.onboarding.system.common.ErrorCode;
import cn.edu.nuc.onboarding.system.dto.TemplateSaveDTO;
import cn.edu.nuc.onboarding.system.entity.TaskTemplate;
import cn.edu.nuc.onboarding.system.mapper.TaskTemplateMapper;
import cn.edu.nuc.onboarding.system.service.TemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TemplateServiceImpl implements TemplateService {

    private final TaskTemplateMapper templateMapper;

    public TemplateServiceImpl(TaskTemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskTemplate> listTemplates(String taskName, String dutyDept) {
        return templateMapper.selectByCondition(trim(taskName), trim(dutyDept));
    }

    @Override
    @Transactional(readOnly = true)
    public TaskTemplate getTemplate(Integer tplId) {
        TaskTemplate template = templateMapper.selectById(tplId);
        if (template == null) {
            throw new BizException(ErrorCode.TEMPLATE_NOT_FOUND, "任务模板不存在");
        }
        return template;
    }

    @Override
    @Transactional
    public TaskTemplate createTemplate(TemplateSaveDTO dto) {
        validate(dto);
        TaskTemplate template = new TaskTemplate();
        template.setTaskName(dto.taskName().trim());
        template.setDutyDept(dto.dutyDept().trim());
        template.setOffsetDay(dto.offsetDay());
        templateMapper.insert(template);
        return template;
    }

    @Override
    @Transactional
    public TaskTemplate updateTemplate(Integer tplId, TemplateSaveDTO dto) {
        validate(dto);
        TaskTemplate existing = templateMapper.selectById(tplId);
        if (existing == null) {
            throw new BizException(ErrorCode.TEMPLATE_NOT_FOUND, "任务模板不存在");
        }
        existing.setTaskName(dto.taskName().trim());
        existing.setDutyDept(dto.dutyDept().trim());
        existing.setOffsetDay(dto.offsetDay());
        templateMapper.update(existing);
        return existing;
    }

    private void validate(TemplateSaveDTO dto) {
        if (dto == null || !hasText(dto.taskName())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "任务名称不能为空");
        }
        if (!hasText(dto.dutyDept())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "责任部门不能为空");
        }
        if (dto.offsetDay() == null || dto.offsetDay() < 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "偏移天数不能小于0");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
