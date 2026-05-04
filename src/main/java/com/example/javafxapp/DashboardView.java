package com.example.javafxapp;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

public class DashboardView {

    public static GridPane createDashboard(int totalDocuments, int premiumDocuments) {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: #f5f5f5;");


        VBox totalCard = createCard(
                "📄 Всего документов",
                String.valueOf(totalDocuments),
                "сохраненных файлов",
                "#3b82f6"
        );


        VBox premiumCard = createCard(
                "✨ Premium документы",
                premiumDocuments + " / " + totalDocuments,
                "HTML формат с картинками",
                "#f59e0b"
        );


        VBox dateCard = createCard(
                "📅 Текущая дата",
                java.time.LocalDate.now().toString(),
                "последнее обновление",
                "#10b981"
        );


        VBox upgradeCard = createCard(
                "💎 Премиум возможности",
                "HTML + Paint",
                "Оформи подписку, чтобы сохранять в HTML",
                "#8b5cf6"
        );

        grid.add(totalCard, 0, 0);
        grid.add(premiumCard, 1, 0);
        grid.add(dateCard, 0, 1);
        grid.add(upgradeCard, 1, 1);

        return grid;
    }

    private static VBox createCard(String title, String value, String subtitle, String colorHex) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);" +
                        "-fx-padding: 16;"
        );
        card.setPrefWidth(260);
        card.setMinHeight(140);

        Rectangle colorBar = new Rectangle(240, 4);
        colorBar.setFill(Color.web(colorHex));
        colorBar.setArcWidth(4);
        colorBar.setArcHeight(4);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");
        valueLabel.setWrapText(true);

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");

        card.getChildren().addAll(colorBar, titleLabel, valueLabel, subtitleLabel);
        return card;
    }
}