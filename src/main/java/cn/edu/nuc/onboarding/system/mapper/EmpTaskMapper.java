package cn.edu.nuc.onboarding.system.mapper;

import cn.edu.nuc.onboarding.system.entity.EmpTask;
import cn.edu.nuc.onboarding.system.vo.DepartmentStatsVO;
import cn.edu.nuc.onboarding.system.vo.EmpTaskVO;
import cn.edu.nuc.onboarding.system.vo.EmployeeStatsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EmpTaskMapper {

    int batchInsert(@Param("list") List<EmpTask> tasks);

    int insertSnapshotTask(EmpTask task);

    EmpTask selectById(@Param("taskId") Integer taskId);

    List<EmpTaskVO> selectTaskPage(
            @Param("empId") Integer empId,
            @Param("department") String department,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    long countTaskPage(
            @Param("empId") Integer empId,
            @Param("department") String department,
            @Param("status") String status
    );

    List<EmpTaskVO> selectTasksByEmployee(@Param("empId") Integer empId);

    EmpTaskVO selectTaskDetail(@Param("taskId") Integer taskId);

    int finishTask(@Param("taskId") Integer taskId, @Param("finishTime") LocalDateTime finishTime);

    int countUnfinishedByEmpId(@Param("empId") Integer empId);

    int deleteByEmpId(@Param("empId") Integer empId);

    EmployeeStatsVO selectEmployeeStats(@Param("empId") Integer empId);

    List<DepartmentStatsVO> selectDepartmentStats();

    List<EmpTaskVO> selectOverdueTasks();
}
