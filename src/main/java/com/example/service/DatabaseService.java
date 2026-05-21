package com.example.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.example.model.DocumentNote;
import com.example.model.Favorite;
import com.example.model.HistoryItem;
import com.example.model.User;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class DatabaseService {

  private static final String SUPABASE_URL = "https://rtnsrxwynaroavgsrugt.supabase.co";
  private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJ0bnNyeHd5bmFyb2F2Z3NydWd0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjMxODkzNzgsImV4cCI6MjA3ODc2NTM3OH0.D7oubRFam3WfVD4iGnImWvq8iXjxabCscaWrq13AL2g";
  private final HttpClient httpClient = HttpClient.newHttpClient();
  public static final Logger logger = Logger.getLogger(DatabaseService.class.getName());

  public User authenticateUser(String username, String password) {
    try {
      logger.info("Попытка аутентификации пользователя: " + username);
      String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
      String response = sendGetRequest("users1?username=eq." + encodedUsername); // отправка запроса
                                                                                 // на сервер для
                                                                                 // получения джисон
                                                                                 // ответа
      JSONArray data = new JSONArray(response); // заносим полученный ответ в массив

      if (data.length() > 0) {
        JSONObject userData = data.getJSONObject(0); // получаем первый и единственный элемент в
                                                     // массиве по индексу
        String storedHash = userData.getString("password_hash");// получаем хэш пароля

        if (storedHash == null || storedHash.isEmpty()) {
          System.err.println("Empty password hash for user: " + username);
          return null;
        }

        BCrypt.Result result = BCrypt.verifyer().verify(// берём соль из существующего в базе данных
                                                        // пароля и хэшируем с её помощью новый
                                                        // пароль для сверки
            password.toCharArray(), // пароль из password field
            storedHash.toCharArray()// пароль из бд
        );

        if (result.verified) {
          User user = new User(// при успехе создаём новый экземпляр класса, куда заносим данные
                               // полученные из тела джисон ответа
              userData.getInt("id"),
              userData.getString("username"),
              storedHash);

          user.setRegisteredAt(LocalDate.parse(userData.getString("created_at").substring(0, 10)));// устанавливаем
                                                                                                   // дату

          String subscriptionPlan = userData.getString("subscription_plan");// берём тип подписки из
                                                                            // базы данных
          if (subscriptionPlan != null) {
            user.setSubscriptionPlan(User.SubscriptionPlan.valueOf(subscriptionPlan));// устанавливаем
                                                                                      // тип
                                                                                      // подписки
          } else {
            user.setSubscriptionPlan(User.SubscriptionPlan.FREE);// устаналиваем план по умолчанию
          }

          return user;
        }
      }
      return null;
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Ошибка аутентификации: " + e.getMessage(), e);
      return null;
    }
  }

  public boolean registerUser(String username, String password) {
    try {
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
      userData.put("subscription_plan", "FREE");
      userData.put("is_active", true);
      userData.put("email", username + "@email.com");

      String jsonBody = userData.toString();
      System.out.println("JSON to send: " + jsonBody);

      String response = sendPostRequest("users1", jsonBody);
      System.out.println("Registration response code: " + response);

      boolean success = response.startsWith("2");// код свидетельствует об успешном выполнении
                                                 // запроса
      System.out.println("Registration success: " + success);
      System.out.println("=== END DEBUG ===");

      return success;

    } catch (Exception e) {
      System.err.println("Error during registration: " + e.getMessage());
      logger.log(Level.SEVERE, "Ошибка при выполнении запроса", e);
      return false;
    }
  }

  private boolean userExists(String username) throws Exception {
    String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
    String response = sendGetRequest("users1?username=eq." + encodedUsername);
    JSONArray data = new JSONArray(response);
    return data.length() > 0;
  }

  public boolean updateUserPlan(int userId, String plan, LocalDate expiryDate) {
    System.out.println("=== SUPABASE REST API DEBUG ===");

    try {
      JSONObject updateData = new JSONObject();
      updateData.put("subscription_plan", plan);
      System.out.println("Update data: " + updateData);

      if (expiryDate != null) {
        updateData.put("subscription_expiry_date", expiryDate.toString());
      }
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

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());// запрос
                                                                                                 // через
                                                                                                 // экземпляр
                                                                                                 // класса
                                                                                                 // http
                                                                                                 // client,
                                                                                                 // преобразуемый
                                                                                                 // в
                                                                                                 // строку

      System.out.println("Response status: " + response.statusCode());
      System.out.println("Response body: " + response.body());

      boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
      System.out.println("Update successful: " + success);

      return success;

    } catch (Exception e) {
      System.out.println("ERROR in Supabase update: " + e.getMessage());
      logger.log(Level.SEVERE, "Ошибка при выполнении запроса", e);
      return false;
    }
  }

  public User getUserById(int userId) {
    System.out.println("=== GET USER FROM SUPABASE ===");

    try {
      String url = SUPABASE_URL + "/rest/v1/users1?id=eq." + userId;

      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("apikey", SUPABASE_KEY)
          .header("Authorization", "Bearer " + SUPABASE_KEY)
          .GET()
          .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        JSONArray jsonArray = new JSONArray(response.body());
        if (jsonArray.length() > 0) {
          JSONObject userJson = jsonArray.getJSONObject(0);

          User user = new User();
          user.setId(userJson.getInt("id"));
          user.setUsername(userJson.getString("username"));

          String planStr = userJson.getString("subscription_plan");
          if (planStr != null) {
            user.setSubscriptionPlan(User.SubscriptionPlan.valueOf(planStr));
          }

          if (userJson.has("subscription_expiry_date")
              && !userJson.isNull("subscription_expiry_date")) {
            String expiryStr = userJson.getString("subscription_expiry_date");
            user.setSubscriptionExpiryDate(LocalDate.parse(expiryStr));
          }

          System.out.println("User loaded from Supabase with plan: " + user.getSubscriptionPlan());
          return user;
        }
      } else {
        System.out.println("Error getting user: " + response.statusCode());
        System.out.println("Response: " + response.body());
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Ошибка при выполнении запроса", e);
    }
    return null;
  }

  public String sendGetRequest(String endpoint) throws Exception {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(SUPABASE_URL + "/rest/v1/" + endpoint))
        .header("apikey", SUPABASE_KEY)
        .header("Authorization", "Bearer " + SUPABASE_KEY)
        .GET()
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return response.body();
  }

  public String sendPostRequest(String endpoint, String body) throws Exception {
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

  private String encodeParam(String param) throws UnsupportedEncodingException {
    return URLEncoder.encode(param, StandardCharsets.UTF_8);
  }

  public String sendPatchRequest(String endpoint, String body) throws Exception {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(SUPABASE_URL + "/rest/v1/" + endpoint))
        .header("apikey", SUPABASE_KEY)
        .header("Authorization", "Bearer " + SUPABASE_KEY)
        .header("Content-Type", "application/json")
        .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
        .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return response.body();
  }

  private String sendDeleteRequest(String endpoint) throws Exception {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(SUPABASE_URL + "/rest/v1/" + endpoint))
        .header("apikey", SUPABASE_KEY)
        .header("Authorization", "Bearer " + SUPABASE_KEY)
        .DELETE()
        .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return response.body();
  }

  public void addOrUpdateHistory(int userId, String filePath) throws Exception {
    String encodedPath = encodeParam(filePath);
    String check = sendGetRequest("history?user_id=eq." + userId + "&file_path=eq." + encodedPath);
    if (new JSONArray(check).length() > 0) {
      JSONObject body = new JSONObject();
      body.put("last_opened", "now()");
      sendPatchRequest("history?user_id=eq." + userId + "&file_path=eq." + encodedPath,
          body.toString());
    } else {
      JSONObject body = new JSONObject();
      body.put("user_id", userId);
      body.put("file_path", filePath);
      sendPostRequest("history", body.toString());
    }
  }

  public List<Favorite> getFavorites(int userId) throws Exception {
    String response = sendGetRequest("favorites?user_id=eq." + userId);
    JSONArray arr = new JSONArray(response);
    List<Favorite> list = new ArrayList<>();
    for (int i = 0; i < arr.length(); i++) {
      JSONObject obj = arr.getJSONObject(i);
      list.add(new Favorite(
          obj.getInt("id"),
          obj.getInt("user_id"),
          obj.getString("file_path"),
          obj.getString("title"),
          LocalDateTime.parse(obj.getString("created_at"))));
    }
    return list;
  }

  public List<HistoryItem> getHistory(int userId, int limit) throws Exception {
    String response = sendGetRequest(
        "history?user_id=eq." + userId + "&order=last_opened.desc&limit=" + limit);
    JSONArray arr = new JSONArray(response);
    List<HistoryItem> list = new ArrayList<>();
    for (int i = 0; i < arr.length(); i++) {
      JSONObject obj = arr.getJSONObject(i);
      list.add(new HistoryItem(
          obj.getInt("id"),
          obj.getInt("user_id"),
          obj.getString("file_path"),
          LocalDateTime.parse(obj.getString("last_opened"))));
    }
    return list;
  }

  public DocumentNote getNote(int userId, String filePath) throws Exception {
    String encodedPath = encodeParam(filePath);
    String response = sendGetRequest(
        "document_notes?user_id=eq." + userId + "&file_path=eq." + encodedPath);
    JSONArray arr = new JSONArray(response);
    if (arr.length() > 0) {
      JSONObject obj = arr.getJSONObject(0);
      return new DocumentNote(
          obj.getInt("id"),
          obj.getInt("user_id"),
          obj.getString("file_path"),
          obj.getString("note_text"),
          obj.getString("updated_at") != null ? LocalDateTime.parse(obj.getString("updated_at"))
              : null);
    }
    return null;
  }

  public void saveOrUpdateNote(int userId, String filePath, String noteText) throws Exception {
    String encodedPath = encodeParam(filePath);
    String check = sendGetRequest(
        "document_notes?user_id=eq." + userId + "&file_path=eq." + encodedPath);
    if (new JSONArray(check).length() > 0) {
      JSONObject body = new JSONObject();
      body.put("note_text", noteText);
      body.put("updated_at", "now()");
      sendPatchRequest("document_notes?user_id=eq." + userId + "&file_path=eq." + encodedPath,
          body.toString());
    } else {
      JSONObject body = new JSONObject();
      body.put("user_id", userId);
      body.put("file_path", filePath);
      body.put("note_text", noteText);
      sendPostRequest("document_notes", body.toString());
    }
  }

  public void addFavorite(int userId, String filePath, String title) throws Exception {
    JSONObject body = new JSONObject();
    body.put("user_id", userId);
    body.put("file_path", filePath);
    body.put("title", title);
    sendPostRequest("favorites", body.toString());
  }

  public void deleteNote(int userId, String filePath) throws Exception {
    String encodedPath = encodeParam(filePath);
    sendDeleteRequest("document_notes?user_id=eq." + userId + "&file_path=eq." + encodedPath);
  }

  public void deleteFavorite(int id) throws Exception {
    sendDeleteRequest("favorites?id=eq." + id);
  }
}
