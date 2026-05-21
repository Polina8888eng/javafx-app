package com.example.javafxapp;

import com.example.model.User;
import com.example.service.DatabaseService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;

import static com.example.service.DatabaseService.logger;

public class SubscriptionPlansController {

  @FXML
  private Button freeButton;
  @FXML
  private Button basicButton;
  @FXML
  private Button vipButton;
  private User currentUser;
  @FXML
  private Button paintButton;

  public void initUser(User user) {
    this.currentUser = new User(user);
    updateButtonStates();
  }

  @FXML
  private void handleFreePlan() {
    User.SubscriptionPlan currentPlan = currentUser.getSubscriptionPlan();
    System.out.println("Переход на Бесплатный план");
    System.out.println("Текущий план: " + currentPlan);

    if (currentPlan == User.SubscriptionPlan.FREE) {
      System.out.println("Уже Бесплатный план, открываем редактор");
      openEditor("FREE");
      return;
    }
    String message;
    if (currentPlan == User.SubscriptionPlan.BASIC) {
      message = "Вы уверены, что хотите перейти на Бесплатный план?\n\n"
          + "Вы потеряете доступ к:\n"
          + "- Расширенному форматированию текста\n"
          + "- Дополнительным шрифтам\n\n"
          + "Ваша подписка на Базовый план будет отменена.";
    } else if (currentPlan == User.SubscriptionPlan.VIP) {
      message = "Вы уверены, что хотите перейти на Бесплатный план?\n\n"
          + "Вы потеряете доступ к:\n"
          + "- Расширенному форматированию\n"
          + "- Дополнительным шрифтам\n\n"
          + "Ваша подписка на VIP план будет отменена.";
    } else {
      message = "Вы уверены, что хотите перейти на Бесплатный план?";
    }

    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
    confirmAlert.setTitle("Переход на бесплатный план");
    confirmAlert.setHeaderText("Подтверждение действия");
    confirmAlert.setContentText(message);

    ButtonType buttonYes = new ButtonType("Да, перейти на Бесплатный");
    ButtonType buttonNo = new ButtonType("Нет, отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
    confirmAlert.getButtonTypes().setAll(buttonYes, buttonNo);

    Optional<ButtonType> result = confirmAlert.showAndWait();
    if (result.isPresent() && result.get() == buttonYes) {
      try {
        DatabaseService dbService = new DatabaseService();
        boolean updated = dbService.updateUserPlan(
            currentUser.getId(),
            "FREE",
            null);

        if (updated) {
          System.out.println("План изменен на Бесплатный");

          User refreshedUser = dbService.getUserById(currentUser.getId());
          if (refreshedUser != null) {
            currentUser = refreshedUser;
            System.out.println("Пользователь обновлен: " + currentUser.getSubscriptionPlan());

            showAlert("Успех", "Вы перешли на Бесплатный план");
            updateButtonStates();
            openEditor("FREE");
          }
        } else {
          System.out.println("Не удалось обновить план");
          showAlert("Ошибка", "Не удалось изменить план подписки");
        }
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Ошибка при выполнении запроса", e);
        showAlert("Ошибка", "Ошибка базы данных: " + e.getMessage());
      }
    } else {
      System.out.println("Отмена перехода на Бесплатный план");
    }
  }

  @FXML
  private void handleBasicPlan() {
    User.SubscriptionPlan currentPlan = currentUser.getSubscriptionPlan();
    if (currentPlan == User.SubscriptionPlan.BASIC) {
      System.out.println("Уже Базовый план, открываем редактор");
      openEditor("BASIC");
      return;
    }

    if (currentPlan == User.SubscriptionPlan.FREE) {
      openPaymentScreen("Базовый", "Расширенное форматирование, +7 доступных шрифтов", 150.00,
          User.SubscriptionPlan.BASIC);
    } else if (currentPlan == User.SubscriptionPlan.VIP) {
      Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
      confirmAlert.setTitle("Переход на Базовый");
      confirmAlert.setHeaderText("Вы уверены?");
      confirmAlert.setContentText(
          "Вы переходите с VIP на Базовый план. Вы потеряете доступ к вставке изображений и другим функциям VIP-плана.");

      Optional<ButtonType> result = confirmAlert.showAndWait();
      if (result.isPresent() && result.get() == ButtonType.OK) {
        openPaymentScreen("Базовый",
            "Все функции бесплатного плана, расширенное форматирование, сохранение в формате docx и 3 доступных шрифта",
            150.00, User.SubscriptionPlan.BASIC);
      }
    }
  }

  @FXML
  private void handleVipPlan() {
    User.SubscriptionPlan currentPlan = currentUser.getSubscriptionPlan();
    System.out.println("VIP-план выбран");
    System.out.println("Текущий план пользователя: " + currentPlan);

    if (currentPlan == User.SubscriptionPlan.VIP) {
      System.out.println("Уже VIP план, открываем редактор без оплаты");
      openEditor("VIP");
      return;
    }

    if (currentPlan == User.SubscriptionPlan.FREE) {
      System.out.println("FREE -> VIP: переход к оплате");
      openPaymentScreen("VIP",
          "Все функции Базового плана, сохранение в форматах html и docx, вставка изображений",
          300.00, User.SubscriptionPlan.VIP);
    } else if (currentPlan == User.SubscriptionPlan.BASIC) {
      System.out.println("BASIC -> VIP: переход к оплате");

      Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
      confirmAlert.setTitle("Апгрейд до VIP");
      confirmAlert.setHeaderText("Подтверждение");
      confirmAlert.setContentText("Вы уверены, что хотите улучшить план до VIP?");

      Optional<ButtonType> result = confirmAlert.showAndWait();
      if (result.isPresent() && result.get() == ButtonType.OK) {
        openPaymentScreen("VIP",
            "Все функции Базового плана, сохранение в форматах html и docx, вставка изображений",
            300.00, User.SubscriptionPlan.VIP);
      }
    }
  }

  private void openEditor(String planType) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/editor.fxml"));
      Parent root = loader.load();
      EditorController controller = loader.getController();
      controller.initUser(currentUser, planType);
      Stage stage = (Stage) basicButton.getScene().getWindow();
      stage.getScene().setRoot(root);
    } catch (IOException e) {
      showError("Ошибка открытия планов подписки", e);
      logger.log(Level.SEVERE, "Ошибка при выполнении запроса", e);
    }
  }

  @FXML
  private void handleOpenPaint() {
    try {
      Stage paintStage = new Stage();
      paintStage.setTitle("Графический редактор");

      CanvasController paintApp = new CanvasController();
      paintApp.start(paintStage);

    } catch (Exception e) {
      logger.log(Level.SEVERE, "Ошибка при выполнении запроса", e);

      showError("Не удалось открыть редактор", e);
    }
  }

  @FXML
  private void handleBack() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth.fxml"));
      Parent root = loader.load();

      Stage stage = (Stage) basicButton.getScene().getWindow();
      stage.getScene().setRoot(root);

    } catch (IOException e) {
      showError("Ошибка загрузки планов подписки", e);
      logger.log(Level.SEVERE, "Ошибка при выполнении запроса", e);
    }
  }

  private void openPaymentScreen(String planName, String description, double price,
      User.SubscriptionPlan plan) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/payment.fxml"));
      Parent root = loader.load();
      PaymentController controller = loader.getController();
      controller.initData(planName, description, price, currentUser, plan);

      Stage stage = (Stage) basicButton.getScene().getWindow();

      stage.getScene().setRoot(root);

    } catch (IOException e) {
      showError("Ошибка загрузки экрана оплаты", e);
      logger.log(Level.SEVERE, "Ошибка при выполнении запроса", e);
    }
  }

  private void updateButtonStates() {
    resetButtons();

    User.SubscriptionPlan currentPlan = currentUser.getSubscriptionPlan();
    System.out.println("Обновление кнопок для плана: " + currentPlan);

    switch (currentPlan) {
      case FREE :
        freeButton.getStyleClass().add("current-plan");
        freeButton.setText("✓ Текущий план (FREE)");
        freeButton.setUserData("CURRENT_PLAN");
        break;
      case BASIC :
        basicButton.getStyleClass().add("current-plan");
        basicButton.setText("✓ Текущий план (BASIC)");
        basicButton.setUserData("CURRENT_PLAN");
        break;
      case VIP :
        vipButton.getStyleClass().add("current-plan");
        vipButton.setText("✓ Текущий план (VIP)");
        vipButton.setUserData("CURRENT_PLAN");
        break;
    }
  }

  private void resetButtons() {
    freeButton.getStyleClass().remove("current-plan");
    basicButton.getStyleClass().remove("current-plan");
    vipButton.getStyleClass().remove("current-plan");

    freeButton.setText("FREE");
    basicButton.setText("BASIC");
    vipButton.setText("VIP");

    freeButton.setUserData(null);
    basicButton.setUserData(null);
    vipButton.setUserData(null);

    freeButton.setDisable(false);
    basicButton.setDisable(false);
    vipButton.setDisable(false);
  }

  private void showError(String message, Exception e) {
    logger.log(Level.SEVERE, "Ошибка при выполнении запроса", e);
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("Ошибка");
    alert.setHeaderText(message);
    alert.setContentText(e.getMessage());
    alert.showAndWait();
  }

  private void showAlert(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }
}