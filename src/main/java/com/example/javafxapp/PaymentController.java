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
import java.sql.SQLException;
import java.time.LocalDate;

public class PaymentController {
    @FXML private Label planLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label priceLabel;
    @FXML private Label totalLabel;
    @FXML private TextField cardNumberField;
    @FXML private TextField expiryDateField;
    @FXML private TextField cvvField;
    @FXML private Button payButton;

    private User currentUser;
    private String planName;
    private String description;
    private double price;
    public void initData(String planName, String description, double price, User user) {
        this.planName = planName;
        this.description = description;
        this.price = price;
        this.currentUser = user;
        planLabel.setText(planName + " Plan");
        descriptionLabel.setText(description);
        priceLabel.setText(String.format("$%.2f/month", price));
        totalLabel.setText(String.format("$%.2f", price));
    }
    @FXML
    private void handlePayment() {
        boolean validationResult = validateCard();
        System.out.println("Card validation: " + validationResult);
        if (!validateCard()) {
            showAlert("Error", "Please enter valid card details");
            return;
        }
        currentUser.setSubscriptionPlan(User.SubscriptionPlan.VIP);
        currentUser.setSubscriptionExpiryDate(LocalDate.now().plusMonths(1));
        try {
            DatabaseService dbService = new DatabaseService();
            boolean updated = dbService.updateUserPlan(
                    currentUser.getId(),
                    "VIP",
                    currentUser.getSubscriptionExpiryDate()
            );
            if (updated) {
                System.out.println("User plan after payment: " + currentUser.getSubscriptionPlan());
                showAlert("Success", "Payment processed successfully!");
                openEditor();
            } else {
                showAlert("Error", "Failed to update subscription plan.");
            }
        } catch (Exception e) {
            showAlert("Error", "Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/subscription_plans.fxml"));
            Parent root = loader.load();
            SubscriptionPlansController controller = loader.getController();
            controller.initUser(currentUser);
            Stage stage = (Stage) payButton.getScene().getWindow();
            stage.setScene(new Scene(root, 390, 844));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load subscription plans screen");
        }
    }
    private boolean validateCard() {
        String cardNumber = cardNumberField.getText().replaceAll("\\s", "");
        String expiryDate = expiryDateField.getText();
        String cvv = cvvField.getText();
        return cardNumber.matches("\\d{16}") &&
                expiryDate.matches("\\d{2}/\\d{2}") &&
                cvv.matches("\\d{3}");
    }
    private void openEditor() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/editor.fxml"));
            Parent root = loader.load();
            EditorController controller = loader.getController();
            controller.initUser(currentUser, currentUser.getSubscriptionPlan().name());
            Stage stage = (Stage) payButton.getScene().getWindow();
            stage.setScene(new Scene(root, 390, 844));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load editor screen");
        }
    }
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
