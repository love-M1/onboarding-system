package cn.edu.nuc.onboarding.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.department")
public class DepartmentProperties {

    private List<String> options = new ArrayList<>(List.of("财务部", "市场部", "法务部"));

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }
}
