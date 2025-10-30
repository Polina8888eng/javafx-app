package com.example;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;
public class App extends Application {
    @Override
    public void start(Stage stage) {
        try {
            System.out.println("Поиск FXML файла...");
            URL fxmlUrl = getClass().getResource( "/views/auth.fxml");
            if (fxmlUrl == null) {
                System.err.println("Файл не найден! Проверьте:");
                System.err.println("1. Расположение файла в папке resources/views/");
                System.err.println("2. Настройки сборки (maven/gradle)");
                return;
            }
            System.out.println("Файл найден: " + fxmlUrl);
            System.out.println("Попытка загрузки FXML...");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            System.out.println("FXML загружен успешно");
            stage.setScene(new Scene(root, 600, 400));
            stage.setTitle("Text Editor");
            stage.show();
        } catch (Exception e) {
            System.err.println("\n=== ОШИБКА ЗАГРУЗКИ ===");
            System.err.println("Тип ошибки: " + e.getClass().getName());
            System.err.println("Сообщение: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Причина: " + e.getCause().getMessage());
            }
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        System.out.println("Запуск приложения...");
        launch(args);
    }
}