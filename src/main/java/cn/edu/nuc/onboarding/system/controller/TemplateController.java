package cn.edu.nuc.onboarding.system.controller;

import cn.edu.nuc.onboarding.system.common.ApiResponse;
import cn.edu.nuc.onboarding.system.dto.TemplateSaveDTO;
import cn.edu.nuc.onboarding.system.entity.TaskTemplate;
import cn.edu.nuc.onboarding.system.service.TemplateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public ApiResponse<List<TaskTemplate>> list(
            @RequestParam(required = false) String taskName,
            @RequestParam(required = false) String dutyDept
    ) {
        return ApiResponse.success(templateService.listTemplates(taskName, dutyDept));
    }

    @GetMapping("/{tplId}")
    public ApiResponse<TaskTemplate> get(@PathVariable Integer tplId) {
        return ApiResponse.success(templateService.getTemplate(tplId));
    }

    @PostMapping
    public ApiResponse<TaskTemplate> create(@RequestBody TemplateSaveDTO dto) {
        return ApiResponse.success("模板新增成功", templateService.createTemplate(dto));
    }

    @PutMapping("/{tplId}")
    public ApiResponse<TaskTemplate> update(@PathVariable Integer tplId, @RequestBody TemplateSaveDTO dto) {
        return ApiResponse.success("模板修改成功", templateService.updateTemplate(tplId, dto));
    }
}
