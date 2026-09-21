package cn.edu.nuc.onboarding.system.entity;

public class TaskTemplate {

    private Integer tplId;
    private String taskName;
    private String dutyDept;
    private Integer offsetDay;

    public Integer getTplId() {
        return tplId;
    }

    public void setTplId(Integer tplId) {
        this.tplId = tplId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getDutyDept() {
        return dutyDept;
    }

    public void setDutyDept(String dutyDept) {
        this.dutyDept = dutyDept;
    }

    public Integer getOffsetDay() {
        return offsetDay;
    }

    public void setOffsetDay(Integer offsetDay) {
        this.offsetDay = offsetDay;
    }
}
