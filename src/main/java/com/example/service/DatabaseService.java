package com.example.service;
import com.example.model.User;
import at.favre.lib.crypto.bcrypt.BCrypt;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.LocalDate;

public class DatabaseService {
    private static final String SUPABASE_URL = "https://rtnsrxwynaroavgsrugt.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJ0bnNyeHd5bmFyb2F2Z3NydWd0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjMxODkzNzgsImV4cCI6MjA3ODc2NTM3OH0.D7oubRFam3WfVD4iGnImWvq8iXjxabCscaWrq13AL2g";
    private HttpClient httpClient = HttpClient.newHttpClient();

    public User authenticateUser(String username, String password) {
        try {
            String response = sendGetRequest("users1?username=eq." + username);
            JSONArray data = new JSONArray(response);

            if (data.length() > 0) {
                JSONObject userData = data.getJSONObject(0);
                String storedHash = userData.getString("password_hash");

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
                            userData.getInt("id"),
                            userData.getString("username"),
                            storedHash
                    );

                    user.setRegisteredAt(LocalDate.parse(userData.getString("created_at").substring(0, 10)));

                    String subscriptionPlan = userData.getString("subscription_type");
                    if (subscriptionPlan != null) {
                        user.setSubscriptionPlan(User.SubscriptionPlan.valueOf(subscriptionPlan));
                    } else {
                        user.setSubscriptionPlan(User.SubscriptionPlan.FREE);
                    }

                    return user;
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error during authentication: " + e.getMessage());
            return null;
        }
    }

    public boolean registerUser(String username, String password) {
        try {
            System.out.println("=== REGISTRATION DEBUG ===");
            System.out.println("Username: " + username);

            boolean exists = userExists(username);
            System.out.println("User exists check: " + exists);

            if (exists) {
                System.out.println("User already exists, registration failed");
                return false;
            }


            String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
            System.out.println("Password hashed successfully");

            JSONObject userData = new JSONObject();
            userData.put("username", username);
            userData.put("password_hash", hash);
            userData.put("subscription_type", "FREE");
            userData.put("is_active", true);
            userData.put("email", username + "@email.com");

            String jsonBody = userData.toString();
            System.out.println("JSON to send: " + jsonBody);

            String response = sendPostRequest("users1", jsonBody);
            System.out.println("Registration response code: " + response);

            boolean success = response.startsWith("2");
            System.out.println("Registration success: " + success);
            System.out.println("=== END DEBUG ===");

            return success;

        } catch (Exception e) {
            System.err.println("Error during registration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean userExists(String username) {
        try {
            System.out.println("Checking if user exists: " + username);
            String response = sendGetRequest("users1?username=eq." + username + "&select=id");
            System.out.println("User exists response: " + response);

            boolean exists = !response.equals("[]") && response.contains("\"id\"");
            System.out.println("User exists result: " + exists);

            return exists;
        } catch (Exception e) {
            System.err.println("Error checking user existence: " + e.getMessage());
            return false;
        }
    }

    public boolean updateUserPlan(int userId, String plan, LocalDate expiryDate) {
        System.out.println("=== SUPABASE REST API DEBUG ===");

        try {
            JSONObject updateData = new JSONObject();
            updateData.put("subscription_type", plan);
            updateData.put("subscription_expiry_date", expiryDate.toString());
            System.out.println("Update data: " + updateData.toString());

            String url = SUPABASE_URL + "/rest/v1/users1?id=eq." + userId;
            System.out.println("API URL: " + url);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer " + SUPABASE_KEY)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(updateData.toString()))
                    .build();

            System.out.println("Sending PATCH request to Supabase...");

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Response status: " + response.statusCode());
            System.out.println("Response body: " + response.body());

            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            System.out.println("Update successful: " + success);

            return success;

        } catch (Exception e) {
            System.out.println("ERROR in Supabase update: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    private String sendGetRequest(String endpoint) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/rest/v1/" + endpoint))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private String sendPostRequest(String endpoint, String body) throws Exception {
        String fullUrl = SUPABASE_URL + "/rest/v1/" + endpoint;
        System.out.println("POST request to: " + fullUrl);
        System.out.println("POST body: " + body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("POST response status: " + response.statusCode());
        System.out.println("POST response body: " + response.body());

        if (response.statusCode() >= 400) {
            System.err.println("ERROR RESPONSE: " + response.body());
        }

        return String.valueOf(response.statusCode());
    }
}
