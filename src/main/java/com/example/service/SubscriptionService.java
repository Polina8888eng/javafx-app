package com.example.service;

import com.example.model.SubscriptionPlan;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SubscriptionService {
    private final DatabaseService dbService = new DatabaseService();

    public List<SubscriptionPlan> getAllPlans() throws SQLException {
        List<SubscriptionPlan> plans = new ArrayList<>();
        String sql = "SELECT * FROM subscription_plans";
        try (Connection conn = dbService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                SubscriptionPlan plan = new SubscriptionPlan(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getDouble("price"),
                        rs.getInt("duration_days"),
                        rs.getBoolean("has_premium_features")
                );
                plans.add(plan);
            }
        }
        if (plans.isEmpty()) {
            initializeDefaultPlans();
            return getAllPlans();
        }
        return plans;
    }

    private void initializeDefaultPlans() throws SQLException {
        String sql = "INSERT INTO subscription_plans (id, name, description, price, duration_days, has_premium_features) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbService.getConnection()) {

            insertPlan(conn, sql, 1, "FREE", "Basic text editing", 0.0, 0, false);

            insertPlan(conn, sql, 2, "BASIC", "Advanced formatting", 4.99, 30, false);

            insertPlan(conn, sql, 3, "VIP", "All premium features", 9.99, 30, true);
        }
    }
    private void insertPlan(Connection conn, String sql, int id, String name,
                            String description, double price, int days, boolean premium) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.setString(2, name);
            stmt.setString(3, description);
            stmt.setDouble(4, price);
            stmt.setInt(5, days);
            stmt.setBoolean(6, premium);
            stmt.executeUpdate();
        }
    }
}