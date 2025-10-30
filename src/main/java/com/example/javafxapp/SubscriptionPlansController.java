package com.example.javafxapp;

import com.example.model.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class SubscriptionPlansController {
    @FXML
    private Button freeButton;
    @FXML
    private Button basicButton;
    @FXML
    private Button vipButton;
    private User currentUser;
    @FXML
    private void handleFreePlan() {
        currentUser.setSubscriptionPlan(User.SubscriptionPlan.FREE);
        updateButtonStates();
        openEditor("FREE");
    }
    private void openEditor(String planType) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/editor.fxml"));
            Parent root = loader.load();
            EditorController controller = loader.getController();
            controller.initUser(currentUser, planType);
            Stage stage = (Stage) freeButton.getScene().getWindow();
            stage.setScene(new Scene(root, 390, 844));
        } catch (IOException e) {
            showError("Failed to load editor", e);
        }
    }
    @FXML
    private void handleBasicPlan() {
        openPaymentScreen("BASIC", "Advanced text formatting and 10+ custom fonts", 4.99);
    }
    @FXML
    private void handleVipPlan() {
        openPaymentScreen("VIP", "All features including image insertion and cloud storage", 9.99);
    }
    @FXML
    private void handleBack() {
        loadScene("/views/auth.fxml");
    }
    private void openPaymentScreen(String planName, String description, double price) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/payment.fxml"));
            Parent root = loader.load();
            PaymentController controller = loader.getController();
            controller.initData(planName, description, price, currentUser);
            Stage stage = (Stage) freeButton.getScene().getWindow();
            stage.setScene(new Scene(root, 390, 844));
        } catch (IOException e) {
            showError("Failed to load payment screen", e);
        }
    }
    @FXML
    public void initialize() {
        System.out.println("SubscriptionPlansController initialized");
    }
    private void updateButtonStates() {
        if (currentUser == null) return;
        freeButton.getStyleClass().remove("current-plan");
        basicButton.getStyleClass().remove("current-plan");
        vipButton.getStyleClass().remove("current-plan");
        resetButtons();
        switch (currentUser.getSubscriptionPlan()) {
            case FREE:
                freeButton.getStyleClass().add("current-plan");
                freeButton.setText("Current Plan");
                freeButton.setDisable(false);
                break;
            case BASIC:
                basicButton.getStyleClass().add("current-plan");
                basicButton.setText("Current Plan");
                basicButton.setDisable(true);
                break;
            case VIP:
                vipButton.getStyleClass().add("current-plan");
                vipButton.setText("Current Plan");
                vipButton.setDisable(true);
                break;
        }
        freeButton.applyCss();
        freeButton.layout();
        System.out.println("Updating buttons. FREE button: "
                + "disabled=" + freeButton.isDisabled()
                + ", text=" + freeButton.getText());
    }
    public void initUser(User user) {
        this.currentUser = user;
        updateButtonStates();
    }
    private void resetButtons() {
        String defaultFreeStyle = "-fx-background-color: #e0e0e0; -fx-text-fill: black;";
        String defaultBasicStyle = "-fx-background-color: #4a7dff; -fx-text-fill: white;";
        String defaultVipStyle = "-fx-background-color: #ff6b00; -fx-text-fill: white;";
        freeButton.setStyle(defaultFreeStyle);
        freeButton.setDisable(false);
        freeButton.setText("Select");
        basicButton.setStyle(defaultBasicStyle);
        basicButton.setDisable(false);
        basicButton.setText("Select");
        vipButton.setStyle(defaultVipStyle);
        vipButton.setDisable(false);
        vipButton.setText("Select");
    }
    private void loadScene(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) freeButton.getScene().getWindow();
            stage.setScene(new Scene(root, 390, 844));
        } catch (IOException e) {
            showError("Failed to load scene", e);
        }
    }
    private void showError(String message, Exception e) {
        e.printStackTrace();
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(message);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }
}