package cn.edu.nuc.onboarding.system.mapper;

import cn.edu.nuc.onboarding.system.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmployeeMapper {

    int insert(Employee employee);

    Employee selectById(@Param("empId") Integer empId);

    Employee selectByPhone(@Param("phone") String phone);

    long countUnarchivedByPhone(@Param("phone") String phone);

    long countByPhone(@Param("phone") String phone);

    List<Employee> selectPage(
            @Param("empName") String empName,
            @Param("department") String department,
            @Param("isArchived") Integer isArchived,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    long count(
            @Param("empName") String empName,
            @Param("department") String department,
            @Param("isArchived") Integer isArchived
    );

    int updateArchiveStatus(@Param("empId") Integer empId, @Param("isArchived") Integer isArchived);

    int deleteById(@Param("empId") Integer empId);
}
