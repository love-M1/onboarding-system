package cn.edu.nuc.onboarding.system.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TaskAction {

    private Integer actionId;
    private Integer taskId;
    private String actionType;
    private Integer actorAccountId;
    private String actorNameSnapshot;
    private LocalDateTime actionTime;
    private String reason;
    private LocalDate newDueDate;
    private Integer relatedSubmissionId;

    public Integer getActionId() {
        return actionId;
    }

    public void setActionId(Integer actionId) {
        this.actionId = actionId;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Integer getActorAccountId() {
        return actorAccountId;
    }

    public void setActorAccountId(Integer actorAccountId) {
        this.actorAccountId = actorAccountId;
    }

    public String getActorNameSnapshot() {
        return actorNameSnapshot;
    }

    public void setActorNameSnapshot(String actorNameSnapshot) {
        this.actorNameSnapshot = actorNameSnapshot;
    }

    public LocalDateTime getActionTime() {
        return actionTime;
    }

    public void setActionTime(LocalDateTime actionTime) {
        this.actionTime = actionTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDate getNewDueDate() {
        return newDueDate;
    }

    public void setNewDueDate(LocalDate newDueDate) {
        this.newDueDate = newDueDate;
    }

    public Integer getRelatedSubmissionId() {
        return relatedSubmissionId;
    }

    public void setRelatedSubmissionId(Integer relatedSubmissionId) {
        this.relatedSubmissionId = relatedSubmissionId;
    }
}
