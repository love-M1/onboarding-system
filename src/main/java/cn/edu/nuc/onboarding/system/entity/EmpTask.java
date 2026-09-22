package cn.edu.nuc.onboarding.system.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class EmpTask {

    private Integer taskId;
    private Integer empId;
    private Integer tplId;
    private String assignedDept;
    private LocalDate baseDueDate;
    private LocalDate currentDueDate;
    private Integer currentSubmissionId;
    private Integer taskStatus;
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

    public Integer getTplId() {
        return tplId;
    }

    public void setTplId(Integer tplId) {
        this.tplId = tplId;
    }

    public String getAssignedDept() {
        return assignedDept;
    }

    public void setAssignedDept(String assignedDept) {
        this.assignedDept = assignedDept;
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
}
