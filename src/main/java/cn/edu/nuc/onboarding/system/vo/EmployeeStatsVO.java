package cn.edu.nuc.onboarding.system.vo;

public class EmployeeStatsVO {

    private Integer empId;
    private String empName;
    private Long totalCount;
    private Long finishedCount;
    private Long pendingCount;
    private Long overdueCount;

    public Integer getEmpId() {
        return empId;
    }

    public void setEmpId(Integer empId) {
        this.empId = empId;
    }

    public String getEmpName() {
        return empName;
    }

    public void setEmpName(String empName) {
        this.empName = empName;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Long getFinishedCount() {
        return finishedCount;
    }

    public void setFinishedCount(Long finishedCount) {
        this.finishedCount = finishedCount;
    }

    public Long getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(Long pendingCount) {
        this.pendingCount = pendingCount;
    }

    public Long getOverdueCount() {
        return overdueCount;
    }

    public void setOverdueCount(Long overdueCount) {
        this.overdueCount = overdueCount;
    }

    public long totalCount() {
        return totalCount == null ? 0 : totalCount;
    }

    public long finishedCount() {
        return finishedCount == null ? 0 : finishedCount;
    }

    public long pendingCount() {
        return pendingCount == null ? 0 : pendingCount;
    }

    public long overdueCount() {
        return overdueCount == null ? 0 : overdueCount;
    }
}
