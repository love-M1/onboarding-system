package cn.edu.nuc.onboarding.system;

import cn.edu.nuc.onboarding.system.common.BizException;
import cn.edu.nuc.onboarding.system.dto.TemplateSaveDTO;
import cn.edu.nuc.onboarding.system.entity.TaskTemplate;
import cn.edu.nuc.onboarding.system.service.TemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TemplateServiceTest {

    @Autowired
    private TemplateService templateService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsAndFiltersTemplatesInTemplateIdOrder() {
        TaskTemplate first = templateService.createTemplate(new TemplateSaveDTO("提交材料", "人事部", 0));
        TaskTemplate second = templateService.createTemplate(new TemplateSaveDTO("开通邮箱", "信息技术部", 1));

        List<TaskTemplate> result = templateService.listTemplates("邮箱", null);

        assertThat(result).extracting(TaskTemplate::getTplId).containsExactly(second.getTplId());
        assertThat(first.getTplId()).isLessThan(second.getTplId());
    }

    @Test
    void rejectsInvalidTemplateFields() {
        assertThatThrownBy(() -> templateService.createTemplate(new TemplateSaveDTO(" ", "人事部", 0)))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(400);

        assertThatThrownBy(() -> templateService.createTemplate(new TemplateSaveDTO("提交材料", " ", 0)))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(400);

        assertThatThrownBy(() -> templateService.createTemplate(new TemplateSaveDTO("提交材料", "人事部", -1)))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(400);
    }

    @Test
    void rejectsUpdatingMissingTemplateWithDocumentErrorCode() {
        assertThatThrownBy(() -> templateService.updateTemplate(99999, new TemplateSaveDTO("材料", "人事部", 0)))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(2001);
    }
}
