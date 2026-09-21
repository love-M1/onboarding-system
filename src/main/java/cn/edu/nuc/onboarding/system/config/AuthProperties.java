package cn.edu.nuc.onboarding.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private boolean exposeCode = true;
    private int codeExpireMinutes = 5;
    private int codeResendSeconds = 60;
    private int maxAttempts = 5;
    private int sessionHours = 8;
    private String bootstrapHrPhone = "13800000000";
    private String bootstrapHrPassword = "Admin@123";
    private String bootstrapHrName = "人事管理员";

    public boolean getExposeCode() {
        return exposeCode;
    }

    public void setExposeCode(boolean exposeCode) {
        this.exposeCode = exposeCode;
    }

    public int getCodeExpireMinutes() {
        return codeExpireMinutes;
    }

    public void setCodeExpireMinutes(int codeExpireMinutes) {
        this.codeExpireMinutes = codeExpireMinutes;
    }

    public int getCodeResendSeconds() {
        return codeResendSeconds;
    }

    public void setCodeResendSeconds(int codeResendSeconds) {
        this.codeResendSeconds = codeResendSeconds;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getSessionHours() {
        return sessionHours;
    }

    public void setSessionHours(int sessionHours) {
        this.sessionHours = sessionHours;
    }

    public String getBootstrapHrPhone() {
        return bootstrapHrPhone;
    }

    public void setBootstrapHrPhone(String bootstrapHrPhone) {
        this.bootstrapHrPhone = bootstrapHrPhone;
    }

    public String getBootstrapHrPassword() {
        return bootstrapHrPassword;
    }

    public void setBootstrapHrPassword(String bootstrapHrPassword) {
        this.bootstrapHrPassword = bootstrapHrPassword;
    }

    public String getBootstrapHrName() {
        return bootstrapHrName;
    }

    public void setBootstrapHrName(String bootstrapHrName) {
        this.bootstrapHrName = bootstrapHrName;
    }
}
