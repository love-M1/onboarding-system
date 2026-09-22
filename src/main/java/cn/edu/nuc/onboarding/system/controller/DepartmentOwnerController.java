package cn.edu.nuc.onboarding.system.controller;

import cn.edu.nuc.onboarding.system.common.ApiResponse;
import cn.edu.nuc.onboarding.system.dto.DepartmentOwnerSaveDTO;
import cn.edu.nuc.onboarding.system.dto.DepartmentOwnerUpdateDTO;
import cn.edu.nuc.onboarding.system.dto.PasswordResetDTO;
import cn.edu.nuc.onboarding.system.service.DepartmentOwnerService;
import cn.edu.nuc.onboarding.system.vo.DepartmentOwnerVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/department-owners")
public class DepartmentOwnerController {

    private final DepartmentOwnerService departmentOwnerService;

    public DepartmentOwnerController(DepartmentOwnerService departmentOwnerService) {
        this.departmentOwnerService = departmentOwnerService;
    }

    @GetMapping
    public ApiResponse<List<DepartmentOwnerVO>> list() {
        return ApiResponse.success(departmentOwnerService.listDepartmentOwners());
    }

    @PostMapping
    public ApiResponse<DepartmentOwnerVO> create(@RequestBody DepartmentOwnerSaveDTO dto) {
        return ApiResponse.success("部门责任人新增成功", departmentOwnerService.createDepartmentOwner(dto));
    }

    @PutMapping("/{accountId}")
    public ApiResponse<DepartmentOwnerVO> update(
            @PathVariable Integer accountId,
            @RequestBody DepartmentOwnerUpdateDTO dto
    ) {
        return ApiResponse.success(
                "部门责任人修改成功",
                departmentOwnerService.updateDepartmentOwner(accountId, dto)
        );
    }

    @DeleteMapping("/{accountId}")
    public ApiResponse<Void> disable(@PathVariable Integer accountId) {
        departmentOwnerService.disableDepartmentOwner(accountId);
        return ApiResponse.success("部门责任人已停用", null);
    }

    @PostMapping("/{accountId}/reset-password")
    public ApiResponse<Void> resetPassword(
            @PathVariable Integer accountId,
            @RequestBody PasswordResetDTO dto
    ) {
        departmentOwnerService.resetPassword(accountId, dto);
        return ApiResponse.success("密码重置成功", null);
    }
}
