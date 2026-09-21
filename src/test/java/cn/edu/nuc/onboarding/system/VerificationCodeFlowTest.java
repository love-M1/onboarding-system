package cn.edu.nuc.onboarding.system;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VerificationCodeFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void unknownPhoneCannotRequestCode() throws Exception {
        mockMvc.perform(post("/api/auth/verification-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13800000009"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(5002));
    }

    @Test
    void registeredEmployeeReceivesSixDigitCode() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO employee(empName, empPhone, empDepartment, empPosition,
                                     entryTime, isArchived, createTime)
                VALUES ('张三', '13800000001', '研发部', '工程师',
                        CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP)
                """);

        mockMvc.perform(post("/api/auth/verification-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13800000001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.devCode").value(matchesPattern("\\d{6}")));
    }
}
