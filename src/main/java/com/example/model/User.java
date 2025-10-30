package com.example.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class User {
    private int id;
    private String username;
    private String passwordHash;
    private LocalDate registeredAt;
    private SubscriptionPlan subscriptionPlan;
    private LocalDate subscriptionExpiryDate;
    public enum SubscriptionPlan {
        FREE,
        BASIC,
        VIP
    }
    public User() {
        this.registeredAt = LocalDate.now();
        this.subscriptionPlan = SubscriptionPlan.FREE;
    }
    public User(int id, String username, String passwordHash) {
        this();
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public LocalDate getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDate registeredAt) { this.registeredAt = registeredAt; }
    public SubscriptionPlan getSubscriptionPlan() { return subscriptionPlan; }
    public void setSubscriptionPlan(SubscriptionPlan subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
    }
    public LocalDate getSubscriptionExpiryDate() { return subscriptionExpiryDate; }
    public void setSubscriptionExpiryDate(LocalDate subscriptionExpiryDate) {
        this.subscriptionExpiryDate = subscriptionExpiryDate;
    }
    public boolean isSubscriptionActive() {
        if (subscriptionPlan == SubscriptionPlan.FREE) {
            return true;
        }
        return subscriptionExpiryDate != null &&
                LocalDate.now().isBefore(subscriptionExpiryDate);
    }
    public long getDaysUntilExpiry() {
        if (subscriptionPlan == SubscriptionPlan.FREE || subscriptionExpiryDate == null) {
            return -1;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), subscriptionExpiryDate);
    }
    public void upgradeToBasic() {
        this.subscriptionPlan = SubscriptionPlan.BASIC;
        this.subscriptionExpiryDate = LocalDate.now().plusMonths(1);
    }
    public void upgradeToVip() {
        this.subscriptionPlan = SubscriptionPlan.VIP;
        this.subscriptionExpiryDate = LocalDate.now().plusMonths(1);
    }
    public void downgradeToFree() {
        this.subscriptionPlan = SubscriptionPlan.FREE;
        this.subscriptionExpiryDate = null;
    }
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", registeredAt=" + registeredAt +
                ", subscriptionPlan=" + subscriptionPlan +
                ", subscriptionExpiryDate=" + subscriptionExpiryDate +
                ", isActive=" + isSubscriptionActive() +
                '}';
    }
}

