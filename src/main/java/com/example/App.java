package com.example;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class App extends Application {
    private static App instance;
    private Stage primaryStage;
    private StackPane rootContainer;

    @Override
    public void start(Stage stage) {
        instance = this;
        this.primaryStage = stage;

        rootContainer = new StackPane();
        Scene scene = new Scene(rootContainer);
        stage.setScene(scene);
        stage.setTitle("Текстовый редактор");
        stage.show();

        loadView("/views/auth.fxml");
    }

    public void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            rootContainer.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static App getInstance() {
        return instance;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}