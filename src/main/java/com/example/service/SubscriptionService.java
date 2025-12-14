package com.example.service;

import com.example.model.SubscriptionPlan;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class SubscriptionService {
    private static final String SUPABASE_URL = "https://rtnsrxwynaroavgsrugt.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJ0bnNyeHd5bmFyb2F2Z3NydWd0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjMxODkzNzgsImV4cCI6MjA3ODc2NTM3OH0.D7oubRFam3WfVD4iGnImWvq8iXjxabCscaWrq13AL2g";
    private HttpClient httpClient = HttpClient.newHttpClient();

    public List<SubscriptionPlan> getAllPlans() {
        List<SubscriptionPlan> plans = new ArrayList<>();
        try {
            String response = sendGetRequest("subscription_plans?order=id");


            if (response.contains("\"id\"")) {
                plans.add(new SubscriptionPlan(1, "FREE", "Basic text editing", 0.0, 0, false));
                plans.add(new SubscriptionPlan(2, "BASIC", "Advanced formatting", 4.99, 30, false));
                plans.add(new SubscriptionPlan(3, "VIP", "All premium features", 9.99, 30, true));
            } else {
                initializeDefaultPlans();
                return getAllPlans();
            }
        } catch (Exception e) {
            e.printStackTrace();
            plans.add(new SubscriptionPlan(1, "FREE", "Basic text editing", 0.0, 0, false));
            plans.add(new SubscriptionPlan(2, "BASIC", "Advanced formatting", 4.99, 30, false));
            plans.add(new SubscriptionPlan(3, "VIP", "All premium features", 9.99, 30, true));
        }
        return plans;
    }

    private void initializeDefaultPlans() {
        try {
            createPlan(1, "FREE", "Basic text editing", 0.0, 0, false);
            createPlan(2, "BASIC", "Advanced formatting", 4.99, 30, false);
            createPlan(3, "VIP", "All premium features", 9.99, 30, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createPlan(int id, String name, String description, double price, int days, boolean premium) {
        try {
            String jsonBody = String.format(
                    "{\"id\":%d,\"name\":\"%s\",\"description\":\"%s\",\"price\":%.2f,\"duration_days\":%d,\"has_premium_features\":%s}",
                    id, name, description, price, days, premium
            );

            sendPostRequest("subscription_plans", jsonBody);
        } catch (Exception e) {
            e.printStackTrace();
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
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/rest/v1/" + endpoint))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}