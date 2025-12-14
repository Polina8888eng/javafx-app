package com.example.javafxapp;

import com.example.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class EditorController {
    @FXML private WebView webView;
    private WebEngine webEngine;

    @FXML private Button saveButton;
    @FXML private Button formatButton;
    @FXML private Button upgradeButton;
    @FXML private Label titleLabel;
    @FXML private Label subscriptionLabel;
    @FXML private Label daysRemainingLabel;
    @FXML private Button insertImageButton;
    @FXML private Button changeFontButton;

    private User currentUser;
    private String currentPlan;

    @FXML
    public void initialize() {
        webEngine = webView.getEngine();
        webEngine.loadContent("<html><body><div id='content' contenteditable='true'></div></body></html>");
    }

    public void initUser(User user, String planType) {
        this.currentUser = user;
        this.currentPlan = planType;
        setupEditorFeatures();
        updateUI();
        setupTooltips();

    }

    private void setupTooltips() {
        if (formatButton != null) {
            Tooltip formatTooltip = new Tooltip(
                    "Сочетания клавиш:\n" +
                            "• Ctrl+B - Жирный текст\n" +
                            "• Ctrl+I - Курсив\n" +
                            "• Ctrl+U - Подчеркнутый\n" +
                            "• Ctrl+Z - Отменить\n" +
                            "• Ctrl+Y - Повторить"
            );
            formatTooltip.setStyle("-fx-font-size: 12px;");
            formatButton.setTooltip(formatTooltip);
        }

        Tooltip insertImageTooltip = new Tooltip(
                "Вставка изображений\n\n" +
                        "Доступны форматы:\n" +
                        "• PNG (.png)\n" +
                        "• JPEG (.jpg, .jpeg)\n" +
                        "• GIF (.gif)\n\n" +
                        "Изображение будет вставлено\nв текущую позицию курсора"
        );
        insertImageTooltip.setStyle("-fx-font-size: 12px;");
        insertImageButton.setTooltip(insertImageTooltip);

        Tooltip changeFontTooltip = new Tooltip(
                "Форматирование текста\n\n" +
                        "Сочетания клавиш:\n" +
                        "• Ctrl+B - Жирный текст\n" +
                        "• Ctrl+I - Курсив\n" +
                        "• Ctrl+U - Подчеркнутый\n\n" +
                        "Используйте правый клик\nдля дополнительных опций"
        );
        changeFontTooltip.setStyle("-fx-font-size: 12px;");
        changeFontButton.setTooltip(changeFontTooltip);
    }

    private void setupEditorFeatures() {
        boolean isFreePlan = "FREE".equals(currentPlan);
        formatButton.setDisable(isFreePlan);
        insertImageButton.setDisable(isFreePlan);
        changeFontButton.setDisable(isFreePlan);

    }

    private void updateUI() {
        subscriptionLabel.setText("Plan: " + currentPlan);
    }

    @FXML
    private void handleInsertImage() {
        if (!"FREE".equals(currentPlan)) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            File imageFile = fileChooser.showOpenDialog(webView.getScene().getWindow());

            if (imageFile != null) {
                insertImageIntoEditor(imageFile);
            }
        } else {
            showAlert("Ошибка", "Добавление изображений доступно только для BASIC и VIP пользователей");
        }
    }

    private void insertImageIntoEditor(File imageFile) {
        try {
            String imagePath = imageFile.toURI().toString();
            String javascript = String.format(
                    "var img = document.createElement('img'); " +
                            "img.src = '%s'; " +
                            "img.style.maxWidth = '300px'; " +
                            "img.style.margin = '10px'; " +
                            "document.getElementById('content').appendChild(img);",
                    imagePath
            );
            webEngine.executeScript(javascript);
            showInfo("Успех", "Изображение добавлено");
        } catch (Exception e) {
            showError("Ошибка", "Не удалось вставить изображение: " + e.getMessage());
        }
    }

    @FXML
    private void handleSave() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("HTML Files", "*.html")
        );
        File file = fileChooser.showSaveDialog(webView.getScene().getWindow());

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                String htmlContent = (String) webEngine.executeScript(
                        "document.getElementById('content').innerHTML"
                );
                writer.write("<html><body>" + htmlContent + "</body></html>");
                showInfo("Успех", "Файл сохранен как HTML");
            } catch (IOException e) {
                showError("Ошибка сохранения", e.getMessage());
            }
        }
    }

    @FXML
    private void handleUpgrade() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/subscription_plans.fxml"));
            Parent root = loader.load();
            SubscriptionPlansController controller = loader.getController();
            controller.initUser(currentUser);
            Stage stage = (Stage) webView.getScene().getWindow();
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
            Stage stage = (Stage) webView.getScene().getWindow();
            stage.setScene(new Scene(root, 390, 844));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleFormat() {
        if (!"FREE".equals(currentPlan)) {
            showInfo("Форматирование", "Используйте сочетания клавиш для форматирования");
        }
    }

    @FXML
    private void handleChangeFont() {
        if (!"FREE".equals(currentPlan)) {
            showInfo("Info", "Используйте сочетания клавиш для смены шрифта");
        }
    }

    private void showAlert(String title, String message) {
        new Alert(Alert.AlertType.WARNING, message, ButtonType.OK).show();
    }

    private void showError(String title, String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).show();
    }

    private void showInfo(String title, String message) {
        new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK).show();
    }
}