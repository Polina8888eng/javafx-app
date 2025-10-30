package com.example.model;
public class SubscriptionPlan {
    private int id;
    private String name;
    private String description;
    private double price;
    private int durationDays;
    private boolean hasPremiumFeatures;
    public SubscriptionPlan() {}
    public SubscriptionPlan(int id, String name, String description, double price,
                            int durationDays, boolean hasPremiumFeatures) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.durationDays = durationDays;
        this.hasPremiumFeatures = hasPremiumFeatures;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }
    public boolean hasPremiumFeatures() { return hasPremiumFeatures; }
    public void setHasPremiumFeatures(boolean hasPremiumFeatures) { this.hasPremiumFeatures = hasPremiumFeatures; }
}