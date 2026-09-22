package cn.edu.nuc.onboarding.system;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DepartmentOwnerManagementTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void hrCreatesDepartmentOwnerAndOwnerCanLogin() throws Exception {
        String hrToken = login("13800000000", "Admin@123");

        int accountId = createOwner(hrToken, "13910000001", "Owner123", "研发部");

        mockMvc.perform(get("/api/department-owners")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[?(@.accountId == %d)].department".formatted(accountId))
                        .value("研发部"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13910000001","password":"Owner123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.user.role").value("DEPARTMENT"))
                .andExpect(jsonPath("$.data.user.department").value("研发部"));
    }

    @Test
    void rejectsSecondEnabledOwnerForSameDepartment() throws Exception {
        String hrToken = login("13800000000", "Admin@123");
        createOwner(hrToken, "13910000002", "Owner123", "研发部");

        mockMvc.perform(post("/api/department-owners")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ownerBody("13910000003", "Owner123", "研发部")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(5013));
    }

    @Test
    void hrDisablesOwnerAndDisabledAccountCannotLogin() throws Exception {
        String hrToken = login("13800000000", "Admin@123");
        int accountId = createOwner(hrToken, "13910000004", "Owner123", "财务部");

        mockMvc.perform(delete("/api/department-owners/{accountId}", accountId)
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13910000004","password":"Owner123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(5011));
    }

    @Test
    void hrUpdatesAndReenablesDepartmentOwner() throws Exception {
        String hrToken = login("13800000000", "Admin@123");
        int accountId = createOwner(hrToken, "13910000005", "Owner123", "市场部");

        mockMvc.perform(delete("/api/department-owners/{accountId}", accountId)
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/department-owners/{accountId}", accountId)
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone":"13910000005",
                                  "displayName":"市场部责任人",
                                  "department":"市场部",
                                  "status":"ACTIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.displayName").value("市场部责任人"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void hrResetsPasswordAndNewPasswordWorks() throws Exception {
        String hrToken = login("13800000000", "Admin@123");
        int accountId = createOwner(hrToken, "13910000006", "Owner123", "行政部");

        mockMvc.perform(post("/api/department-owners/{accountId}/reset-password", accountId)
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPassword":"Changed456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13910000006","password":"Owner123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(5010));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13910000006","password":"Changed456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void employeeAndDepartmentTokensCannotManageOwners() throws Exception {
        String hrToken = login("13800000000", "Admin@123");
        createOwner(hrToken, "13910000007", "Owner123", "法务部");
        String departmentToken = login("13910000007", "Owner123");
        insertEmployeeAccount("13910000008");
        String employeeToken = login("13910000008", "Employee123");

        mockMvc.perform(get("/api/department-owners")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(get("/api/department-owners")
                        .header("Authorization", "Bearer " + departmentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    private int createOwner(
            String token,
            String phone,
            String password,
            String department
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/department-owners")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ownerBody(phone, password, department)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accountId");
    }

    private String ownerBody(String phone, String password, String department) {
        return """
                {
                  "phone":"%s",
                  "password":"%s",
                  "displayName":"%s责任人",
                  "department":"%s"
                }
                """.formatted(phone, password, department, department);
    }

    private String login(String phone, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"%s","password":"%s"}
                                """.formatted(phone, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    private void insertEmployeeAccount(String phone) {
        jdbcTemplate.update("""
                INSERT INTO user_account(phone, passwordHash, role, empId, displayName,
                                         department, status, createTime, updateTime)
                VALUES (?, ?, 'EMPLOYEE', NULL, '员工', '研发部', 'ACTIVE',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, phone, passwordEncoder.encode("Employee123"));
    }
}
