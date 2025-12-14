package com.example.javafxapp;

import com.example.model.User;
import com.example.service.DatabaseService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class AuthController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    private double xOffset = 0;
    private double yOffset = 0;
    private final DatabaseService databaseService = new DatabaseService();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter username and password");
            return;
        }

        User user = databaseService.authenticateUser(username, password);
        if (user != null) {
            openSubscriptionPlans(user);
        } else {
            showError("Invalid credentials");
        }
    }
    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter username and password");
            return;
        }
        try {
            if (databaseService.registerUser(username, password)) {
                showError("Registration successful! Please login");
            } else {
                showError("Username already taken");
            }
        } catch (Exception e) {
            showError("Registration error");
            e.printStackTrace();
        }
    }
    @FXML
    private void handleClose() {
        Stage stage = (Stage) loginButton.getScene().getWindow();
        stage.close();
    }
    private void openSubscriptionPlans(User user) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/subscription_plans.fxml"));
                    Parent root = loader.load();
                    SubscriptionPlansController controller = loader.getController();
                    if (user.getSubscriptionPlan() == null) {
                        user.setSubscriptionPlan(User.SubscriptionPlan.FREE);
                    }
                    controller.initUser(user);
                    Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root, 390, 844);
            root.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            root.setOnMouseDragged(event -> {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            });
            stage.setScene(scene);
        } catch (IOException e) {
            showError("Error loading subscription plans");
            e.printStackTrace();
        }
    }
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: red;");
    }
}