package cn.edu.nuc.onboarding.system.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class EmpTaskVO {

    private Integer taskId;
    private Integer empId;
    private String empName;
    private Integer tplId;
    private String taskName;
    private String dutyDept;
    private LocalDateTime entryTime;
    private LocalDate dueDate;
    private Integer taskStatus;
    private Boolean overdue;
    private Boolean archived;
    private Boolean canFinish;
    private LocalDateTime finishTime;

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

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

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Integer getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(Integer taskStatus) {
        this.taskStatus = taskStatus;
    }

    public Boolean getOverdue() {
        return overdue;
    }

    public void setOverdue(Boolean overdue) {
        this.overdue = overdue;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public Boolean getCanFinish() {
        return canFinish;
    }

    public void setCanFinish(Boolean canFinish) {
        this.canFinish = canFinish;
    }

    public LocalDateTime getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(LocalDateTime finishTime) {
        this.finishTime = finishTime;
    }

    public Integer taskId() {
        return taskId;
    }

    public Integer taskStatus() {
        return taskStatus;
    }

    public LocalDateTime finishTime() {
        return finishTime;
    }
}
