package cn.edu.nuc.onboarding.system.service;

import cn.edu.nuc.onboarding.system.dto.TemplateSaveDTO;
import cn.edu.nuc.onboarding.system.entity.TaskTemplate;

import java.util.List;

public interface TemplateService {

    List<TaskTemplate> listTemplates(String taskName, String dutyDept);

    TaskTemplate getTemplate(Integer tplId);

    TaskTemplate createTemplate(TemplateSaveDTO dto);

    TaskTemplate updateTemplate(Integer tplId, TemplateSaveDTO dto);
}
