package com.example.service;
import com.example.model.User;
import at.favre.lib.crypto.bcrypt.BCrypt;
import java.sql.*;
import java.time.LocalDate;
public class DatabaseService {
    private static final String DB_URL = "jdbc:sqlserver://DESKTOP-OO2R3SG;databaseName=TextEditor;";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "sa445";
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
    public User authenticateUser(String username, String password) {
        String sql = "SELECT id, username, password_hash, created_at, subscription_type FROM users1 WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (storedHash == null || storedHash.isEmpty()) {
                    System.err.println("Empty password hash for user: " + username);
                    return null;
                }
                BCrypt.Result result = BCrypt.verifyer().verify(
                        password.toCharArray(),
                        storedHash.toCharArray()
                );
                if (result.verified) {
                    User user = new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            storedHash
                    );
                    user.setRegisteredAt(rs.getDate("created_at").toLocalDate());
                    String subscriptionPlan = rs.getString("subscription_type");
                    if (subscriptionPlan != null) {
                        user.setSubscriptionPlan(User.SubscriptionPlan.valueOf(subscriptionPlan));
                    } else {
                        user.setSubscriptionPlan(User.SubscriptionPlan.FREE);
                    }
                    return user;
                }
            }
            return null;
        } catch (SQLException e) {
            System.err.println("SQL Error during authentication: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error during authentication: " + e.getMessage());
            return null;
        }
    }
    public boolean registerUser(String username, String password) throws SQLException {
        if (userExists(username)) return false;
        String sql = "INSERT INTO users1 (username, password_hash, subscription_type) VALUES (?, ?, 'FREE')";
        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, hash);
            return stmt.executeUpdate() > 0;
        }
    }
    private boolean userExists(String username) throws SQLException {
        String sql = "SELECT id FROM users1 WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            return stmt.executeQuery().next();
        }
    }
    public static boolean updateUserPlan(int userId, String plan, LocalDate expiryDate) throws SQLException {
        String sql = "UPDATE users1 SET subscription_type = ?, subscription_expiry_date = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, plan);
            stmt.setDate(2, java.sql.Date.valueOf(expiryDate));
            stmt.setInt(3, userId);
            return stmt.executeUpdate() > 0;
        }
    }
}
