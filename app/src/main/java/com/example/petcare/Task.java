package com.example.petcare;

public class Task {

    private String taskId;
    private String petId;         // The ID of the pet the task is related to
    private String taskName;      // The name or description of the task
    private String assignedUserId; // The user who is responsible for the task (empty for recurring task templates)
    private String dueDate;       // The date when the task is due
    private String dueTime;       // The time when the task is due
    private String status;        // The status of the task (e.g., "pending", "completed")

    // New fields for recurrence
    private String recurrenceType;  // "none", "daily", or "SUNDAY/MONDAY/.../SATURDAY"

    // Default constructor (required for Firebase)
    public Task() {}

    // Constructor for one-time tasks
    public Task(String taskName, String petId, String assignedUserId, String dueDate, String dueTime, String status) {
        this.taskName = taskName;
        this.petId = petId;
        this.assignedUserId = assignedUserId;
        this.dueDate = dueDate;
        this.dueTime = dueTime;
        this.status = status;
        // Defaults for non-recurring tasks
        this.recurrenceType = "none";
    }

    // Overloaded constructor for recurring tasks
    public Task(String taskName, String petId, String assignedUserId, String dueDate, String dueTime, String status, String recurrenceType) {
        this.taskName = taskName;
        this.petId = petId;
        this.assignedUserId = assignedUserId;
        this.dueDate = dueDate;
        this.dueTime = dueTime;
        this.status = status;
        this.recurrenceType = recurrenceType;
    }

    // Getters and setters for all fields
    public String getTaskId() {
        return taskId;
    }
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    public String getPetId() {
        return petId;
    }
    public void setPetId(String petId) {
        this.petId = petId;
    }
    public String getTaskName() {
        return taskName;
    }
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
    public String getAssignedUserId() {
        return assignedUserId;
    }
    public void setAssignedUserId(String assignedUserId) {
        this.assignedUserId = assignedUserId;
    }
    public String getDueDate() {
        return dueDate;
    }
    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }
    public String getDueTime() {
        return dueTime;
    }
    public void setDueTime(String dueTime) {
        this.dueTime = dueTime;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getRecurrenceType() {
        return recurrenceType;
    }
    public void setRecurrenceType(String recurrenceType) {
        this.recurrenceType = recurrenceType;
    }
}
