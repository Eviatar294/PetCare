package com.example.petcare;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L; // Recommended for Serializable classes

    private String userId;
    private String email;
    private String name;
    private String password;
    private String petId;
    private String petPassword;
    private Integer numOfTasks;
    private String notificationTime;    // when the user will get notification for unassigned tasks for tomorrow

    public User() {
    }

    public User(String userId, String email, String name, String password, String petId, String petPassword, Integer numOfTasks) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.password = password;
        this.petId = petId;
        this.petPassword = petPassword;
        this.numOfTasks = numOfTasks;
        this.notificationTime = "20:00"; // Default time to 8 PM
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPetId() {
        return petId;
    }

    public void setPetId(String petId) {
        this.petId = petId;
    }

    public Integer getNumOfTasks() {
        return numOfTasks;
    }

    public void setNumOfTasks(Integer numOfTasks) {
        this.numOfTasks = numOfTasks;
    }

    public String getPetPassword() {
        return petPassword;
    }

    public void setPetPassword(String petPassword) {
        this.petPassword = petPassword;
    }
    public String getNotificationTime() {
        return notificationTime;
    }

    public void setNotificationTime(String notificationTime) {
        this.notificationTime = notificationTime;
    }
}
