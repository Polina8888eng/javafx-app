package com.example.model;
import java.time.LocalDate;
public class Subscription {
    private int id;
    private int userId;
    private int planId;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isActive;
    public Subscription() {}
    public Subscription(int id, int userId, int planId,
                        LocalDate startDate, LocalDate endDate, boolean isActive) {
        this.id = id;
        this.userId = userId;
        this.planId = planId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = isActive;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getPlanId() { return planId; }
    public void setPlanId(int planId) { this.planId = planId; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}