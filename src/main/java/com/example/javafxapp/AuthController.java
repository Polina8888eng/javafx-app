package com.example.javafxapp;

import com.example.model.User;
import com.example.service.DatabaseService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;

import static com.example.service.DatabaseService.logger;

/**
 * класс для регистрации новых пользователей и авторизации пользователей, уже
 * существующих в базе данных.
 *
 * @see DatabaseService
 */
public class AuthController {

  @FXML
  private TextField usernameField;
  @FXML
  private PasswordField passwordField;
  @FXML
  private Button loginButton;
  @FXML
  private Label errorLabel;

  private final DatabaseService databaseService = new DatabaseService();

  /**
   * Выполняет вход в систему уже зарегистрированных пользователей, в качестве
   * входных параметров принимает логин и пароль, введённые пользователем
   * <p>
   * В случае успешной проверки открывает экран выбора планов подписки, в ином
   * случае выдаёт сообщение об ошибке
   * </p>
   */
  @FXML
  private void handleLogin() {
    String username = usernameField.getText().trim();
    String password = passwordField.getText().trim();

    String error = validateInput(username, password, false);
    if (error != null) {
      showError(error);
      return;
    }

    User user = databaseService.authenticateUser(username, password);
    if (user != null) {
      openSubscriptionPlans(user);
    } else {
      showError("Неверные учётные данные");
    }
  }

  /**
   * Выполняет регистрацию новых пользователей, в качестве входных параметров
   * принимает логин и пароль.
   * <p>
   * Пароль должен состоять из минимум 8 символов, одной цифры и одного
   * специального символа
   * </p>
   * <p>
   * В случае корректной отправки данных на сервер выдаёт сообщение об успешной
   * регистрации, в противном случае выдаёт ошибку
   * </p>
   */
  @FXML
  private void handleRegister() {
    String username = usernameField.getText().trim();
    String password = passwordField.getText().trim();

    String error = validateInput(username, password, true);
    if (error != null) {
      showError(error);
      return;
    }

    try {
      if (databaseService.registerUser(username, password)) {
        showError("Успешная регистрация! Теперь войдите.");
      } else {
        showError("Пользователь с таким именем уже существует");
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Ошибка при выполнении запроса", e);
    }
  }

  private String validateInput(String username, String password, boolean isRegistration) {
    if (username == null || username.trim().isEmpty()) {
      return "Имя пользователя не может быть пустым";
    }
    if (username.length() < 3 || username.length() > 20) {
      return "Имя пользователя должно быть от 3 до 20 символов";
    }
    if (password == null || password.isEmpty()) {
      return "Пароль не может быть пустым";
    }
    if (password.length() < 6) {
      return "Пароль должен содержать минимум 6 символов";
    }
    if (isRegistration) {
      if (!password.matches(".*\\d.*")) {
        return "Пароль должен содержать хотя бы одну цифру";
      }
      if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
        return "Пароль должен содержать хотя бы один специальный символ";
      }
    }
    return null;
  }

  @FXML
  private void handleClose() {
    Stage stage = (Stage) loginButton.getScene().getWindow();
    stage.close();
  }

  /**
   * Выполняет открытие экрана планов подписки после успешной авторизации
   * пользователя. Сразу показывает текущий план,
   * установленный у пользователя в базе данных
   *
   * @param user Принимает объект user, созданный в системе при удачной
   *          авторизации и содержащий все поля, существующие в базе данных
   */
  private void openSubscriptionPlans(User user) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/subscription_plans.fxml"));
      Parent root = loader.load();
      SubscriptionPlansController controller = loader.getController();
      controller.initUser(user);

      Stage stage = (Stage) loginButton.getScene().getWindow();
      stage.getScene().setRoot(root);

    } catch (IOException e) {
      logger.log(Level.SEVERE, "Ошибка при выполнении запроса", e);
    }
  }

  /**
   * Отображает сообщение об ошибке
   *
   * @param message Принимает текст пойманной ошибки. Показывает её, выделенную
   *          красным цветом, пользователю
   */
  private void showError(String message) {
    errorLabel.setText(message);
    errorLabel.setStyle("-fx-text-fill: red;");
  }
}
