package com.example.javafxapp;

import com.example.model.User;
import com.example.service.DatabaseService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.function.UnaryOperator;

/**
 * Контроллер для реализации методов оплаты планов подписки и валидации данных карты
 */
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
    private User.SubscriptionPlan selectedPlan;
    private String planName;
    private String description;
    private double price;

    /**
     * Передаёт необходимые данные экрану оплаты
     * @param planName название тарифного плана
     * @param description описание возможностей плана подписки
     * @param price стоимость
     * @param user текущий пользователь с ещё не изменённым планом
     * @param plan новый план, выбранный пользователем для оплаты
     */
    public void initData(String planName, String description, double price, User user, User.SubscriptionPlan plan) {
        this.planName = planName;
        this.description = description;
        this.price = price;
        this.currentUser = user;
        this.selectedPlan = plan;

        planLabel.setText(planName + " план");
        descriptionLabel.setText(description);
        priceLabel.setText(String.format("%.2f руб./месяц", price));
        totalLabel.setText(String.format("%.2f руб./месяц", price));
        setupExpiryDateFormatter();

        System.out.println("PaymentController инициализирован с планом: " + selectedPlan);
    }

    private void setupExpiryDateFormatter() {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();

            if (!newText.matches("\\d{0,2}/?\\d{0,2}")) {
                return null;
            }

            if (change.isDeleted()) {
                return change;
            }

            if (newText.length() == 2 && !newText.contains("/")) {
                change.setText(change.getText() + "/");
                change.setCaretPosition(3);
                change.setAnchor(3);
            }
            return change;
        };
        expiryDateField.setTextFormatter(new TextFormatter<>(filter));
    }

    /**
     * Инициализирует экран оплаты выбранного плана подписки
     * <p>Обновляет план пользователя в базе данных при успешной проверке данных карты</p>
     * <p>Открывает экран текстового редактора с обновлёнными функциями</p>
     */
    @FXML
    private void handlePayment() {
        if (selectedPlan == null) {
            showAlert("Ошибка", "План не выбран");
            return;
        }

        boolean validationResult = validateCard();
        System.out.println("Валидация карты: " + validationResult);

        if (!validationResult) {
            showAlert("Ошибка", "Введите корректные реквизиты карты");
            return;
        }
        currentUser.setSubscriptionPlan(selectedPlan);
        currentUser.setSubscriptionExpiryDate(LocalDate.now().plusMonths(1));

        System.out.println("Устанавливается план: " + selectedPlan);
        System.out.println("План пользователя после установки: " + currentUser.getSubscriptionPlan());

        try {
            DatabaseService dbService = new DatabaseService();
            boolean updated = dbService.updateUserPlan(
                    currentUser.getId(),
                    selectedPlan.name(),
                    currentUser.getSubscriptionExpiryDate()
            );

            if (updated) {
                System.out.println("План после оплаты: " + currentUser.getSubscriptionPlan());
                showAlert("Успех", "Оплата прошла успешно!");
                openEditor();
            } else {
                showAlert("Ошибка", "Ошибка обновления плана.");
            }
        } catch (Exception e) {
            showAlert("Ошибка", "Ошибка базы данных: " + e.getMessage());
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

            stage.getScene().setRoot(root);

        } catch (IOException e) {
            showError("Ошибка","Ошибка загрузки планов подписки");
            e.printStackTrace();
        }
    }

    /**
     * Валидация реквизитов карты
     * @return возвращает true, если все данные прошли проверку
     */
    private boolean validateCard() {
        String cardNumber = cardNumberField.getText().replaceAll("\\s", "");
        String expiryDate = expiryDateField.getText();
        String cvv = cvvField.getText();

        boolean cardValid = cardNumber.matches("\\d{16}");

        boolean cvvValid = cvv.matches("\\d{3}");

        boolean expiryValid = false;
        if (expiryDate.matches("\\d{2}/\\d{2}")) {
            String[] parts = expiryDate.split("/");
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]);

            if (month >= 1 && month <= 12) {
                java.time.YearMonth now = java.time.YearMonth.now();
                int currentYear = now.getYear() % 100;
                int currentMonth = now.getMonthValue();

                if (year > currentYear || (year == currentYear && month >= currentMonth)) {
                    expiryValid = true;
                }
            }
        }

        return cardValid && expiryValid && cvvValid;
    }

    /**
     * Инициализирует интерфейс текстового редактора после успешной оплаты
     * <p>Передаёт в редактор обновлённые данные пользователя</p>
     */
    private void openEditor() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/subscription_plans.fxml"));
            Parent root = loader.load();
            SubscriptionPlansController controller = loader.getController();
            controller.initUser(currentUser);

            Stage stage = (Stage) payButton.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Ошибка","Ошибка загрузки планов подписки");
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).show();
    }
}
