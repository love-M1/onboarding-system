package cn.edu.nuc.onboarding.system.vo;

public class DepartmentStatsVO {

    private String dutyDept;
    private Long totalCount;
    private Long finishedCount;
    private Long pendingCount;
    private Long overdueCount;

    public String getDutyDept() {
        return dutyDept;
    }

    public void setDutyDept(String dutyDept) {
        this.dutyDept = dutyDept;
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
}
