package com.example.javafxapp;
import com.example.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.*;
public class EditorController {
    @FXML private TextArea textArea;
    @FXML private Button saveButton;
    @FXML private Button formatButton;
    @FXML private Button upgradeButton;
    @FXML private Label titleLabel;
    @FXML private Label subscriptionLabel;
    @FXML private Label daysRemainingLabel;
    private User currentUser;
    private String currentPlan;
    public void initUser(User user, String planType) {
        this.currentUser = user;
        this.currentPlan = planType;
        System.out.println("Editor initialized. Plan: " + planType + ", User plan: " + user.getSubscriptionPlan());
        setupEditorFeatures();
        updateUI();
    }
    private void setupEditorFeatures() {
        if (formatButton == null || upgradeButton == null) {
            System.out.println("Warning: Some buttons are not initialized!");
            return;
        }
        boolean isFreePlan = "FREE".equals(currentPlan);
        if (formatButton != null) {
            formatButton.setDisable(isFreePlan);
        }
        if (upgradeButton != null) {
            upgradeButton.setVisible(isFreePlan);
        }
    }
    private void updateUI() {
        if (currentUser != null) {
            titleLabel.setText("Text Editor - " + currentUser.getUsername());
            subscriptionLabel.setText("Plan: " + currentPlan);
            textArea.setPromptText("Start typing here...");
            if (!"FREE".equals(currentPlan)) {
                daysRemainingLabel.setText("Premium features active");
            }
        }
    }
    @FXML
    private void handleSave() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File file = fileChooser.showSaveDialog(textArea.getScene().getWindow());
        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(textArea.getText());
            } catch (IOException e) {
                showError("Save Error", e.getMessage());
            }
        }
    }
    @FXML
    private void handleFormat() {
        if (!"FREE".equals(currentPlan)) {
            textArea.setText(formatText(textArea.getText()));
        }
    }
    @FXML
    private void handleInsertImage() {
        if (!"FREE".equals(currentPlan)) {
            showInfo("Info", "Image insertion available in premium version");
        }
    }
    @FXML
    private void handleChangeFont() {
        if (!"FREE".equals(currentPlan)) {
            showInfo("Info", "Font customization available in premium version");
        }
    }
    @FXML
    private void handleUpgrade() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/subscription_plans.fxml"));
            Parent root = loader.load();
            SubscriptionPlansController controller = loader.getController();
            controller.initUser(currentUser);
            Stage stage = (Stage) upgradeButton.getScene().getWindow();
            stage.setScene(new Scene(root, 390, 844));
        } catch (IOException e) {
            showError("Navigation Error", e.getMessage());
        }
    }
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/subscription_plans.fxml"));
            Parent root = loader.load();
            SubscriptionPlansController controller = loader.getController();
            controller.initUser(currentUser);
            Stage stage = (Stage) textArea.getScene().getWindow();
            stage.setScene(new Scene(root, 390, 844));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            if (fxmlPath.contains("subscription_plans")) {
                SubscriptionPlansController controller = loader.getController();
                controller.initUser(currentUser);
            }

            Stage stage = (Stage) textArea.getScene().getWindow();
            stage.setScene(new Scene(root, 390, 844));
        } catch (IOException e) {
            showError("Navigation Error", e.getMessage());
        }
    }

    private String formatText(String text) {
        if (text == null || text.isEmpty()) return text;

        StringBuilder result = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return result.toString().trim();
    }
    private void showError(String title, String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).show();
    }
    private void showInfo(String title, String message) {
        new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK).show();
    }
}