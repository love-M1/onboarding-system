package cn.edu.nuc.onboarding.system.service;

import cn.edu.nuc.onboarding.system.dto.DepartmentOwnerSaveDTO;
import cn.edu.nuc.onboarding.system.dto.DepartmentOwnerUpdateDTO;
import cn.edu.nuc.onboarding.system.dto.PasswordResetDTO;
import cn.edu.nuc.onboarding.system.vo.DepartmentOwnerVO;

import java.util.List;

public interface DepartmentOwnerService {

    List<DepartmentOwnerVO> listDepartmentOwners();

    DepartmentOwnerVO createDepartmentOwner(DepartmentOwnerSaveDTO dto);

    DepartmentOwnerVO updateDepartmentOwner(Integer accountId, DepartmentOwnerUpdateDTO dto);

    void disableDepartmentOwner(Integer accountId);

    void resetPassword(Integer accountId, PasswordResetDTO dto);
}
