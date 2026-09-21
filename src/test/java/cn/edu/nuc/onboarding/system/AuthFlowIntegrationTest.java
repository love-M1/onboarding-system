package cn.edu.nuc.onboarding.system;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void employeeCanRegisterLoginRestoreAndLogout() throws Exception {
        insertEmployee("13800000001");

        String code = requestCode("13800000001");
        MvcResult registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone":"13800000001",
                                  "code":"%s",
                                  "password":"Onboard123"
                                }
                                """.formatted(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.user.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.data.user.empId").isNumber())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        String registrationToken = jsonString(registration, "$.data.token");
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + registrationToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("13800000001"));

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone":"13800000001",
                                  "password":"Onboard123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.displayName").value("张三"))
                .andReturn();

        String loginToken = jsonString(login, "$.data.token");
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void rejectsWrongPassword() throws Exception {
        insertEmployee("13800000002");
        String code = requestCode("13800000002");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13800000002","code":"%s","password":"Onboard123"}
                                """.formatted(code)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13800000002","password":"Wrong123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(5010));
    }

    @Test
    void rejectsDuplicateRegistration() throws Exception {
        insertEmployee("13800000003");
        String code = requestCode("13800000003");
        String body = """
                {"phone":"13800000003","code":"%s","password":"Onboard123"}
                """.formatted(code);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(5003));
    }

    private void insertEmployee(String phone) {
        jdbcTemplate.update("""
                INSERT INTO employee(empName, empPhone, empDepartment, empPosition,
                                     entryTime, isArchived, createTime)
                VALUES ('张三', ?, '研发部', '工程师',
                        CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP)
                """, phone);
    }

    private String requestCode(String phone) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/verification-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"%s"}
                                """.formatted(phone)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return jsonString(result, "$.data.devCode");
    }

    private String jsonString(MvcResult result, String pointer) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), pointer);
    }
}
