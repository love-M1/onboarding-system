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
    private String assignedDept;
    private LocalDateTime entryTime;
    private LocalDate baseDueDate;
    private LocalDate currentDueDate;
    private LocalDate dueDate;
    private Integer currentSubmissionId;
    private Integer taskStatus;
    private Boolean overdue;
    private Boolean archived;
    private Boolean canFinish;
    private Integer finishByAccountId;
    private String finishByName;
    private LocalDateTime finishTime;
    private Integer version;

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

    public String getAssignedDept() {
        return assignedDept;
    }

    public void setAssignedDept(String assignedDept) {
        this.assignedDept = assignedDept;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public LocalDate getBaseDueDate() {
        return baseDueDate;
    }

    public void setBaseDueDate(LocalDate baseDueDate) {
        this.baseDueDate = baseDueDate;
    }

    public LocalDate getCurrentDueDate() {
        return currentDueDate;
    }

    public void setCurrentDueDate(LocalDate currentDueDate) {
        this.currentDueDate = currentDueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Integer getCurrentSubmissionId() {
        return currentSubmissionId;
    }

    public void setCurrentSubmissionId(Integer currentSubmissionId) {
        this.currentSubmissionId = currentSubmissionId;
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

    public Integer getFinishByAccountId() {
        return finishByAccountId;
    }

    public void setFinishByAccountId(Integer finishByAccountId) {
        this.finishByAccountId = finishByAccountId;
    }

    public String getFinishByName() {
        return finishByName;
    }

    public void setFinishByName(String finishByName) {
        this.finishByName = finishByName;
    }

    public LocalDateTime getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(LocalDateTime finishTime) {
        this.finishTime = finishTime;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
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
