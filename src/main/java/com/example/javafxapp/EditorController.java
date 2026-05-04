package com.example.javafxapp;

import com.example.model.User;
import com.example.service.DatabaseService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;


import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import static com.example.model.User.SubscriptionPlan.BASIC;
import static com.example.model.User.SubscriptionPlan.VIP;

/**
 * Контроллер для обеспечения функционирования текстового редактора на базе WebView,
 * содержащий все необходимые методы для написания, форматирования, сохранения текста
 */
public class EditorController {
    @FXML private WebView webView;
    private WebEngine webEngine;
    @FXML
    private ComboBox<String> fontComboBox;

    private List<String> allFonts;
    private List<String> availableFonts;
    private ChangeListener<Worker.State> loadListener;
    private boolean handlingTagInsert = false;

    @FXML private Button saveButton;
    @FXML private Button formatButton;
    @FXML private Button upgradeButton;
    @FXML private Button handleOpen;
    @FXML private Label titleLabel;
    @FXML private Label subscriptionLabel;
    @FXML private Label daysRemainingLabel;
    @FXML private Button insertImageButton;
    @FXML private TextField findField;
    @FXML private TextField replaceField;
    @FXML private Button boldButton;
    @FXML private Button italicButton;
    @FXML private Button underlineButton;
    @FXML private Button strikeButton;
    @FXML private Button alignLeftButton;
    @FXML private Button alignCenterButton;
    @FXML private Button alignRightButton;
    @FXML private Button orderedListButton;
    @FXML private Button unorderedListButton;
    @FXML private ColorPicker textColorPicker;
    @FXML private ComboBox<Integer> fontSizeCombo;
    @FXML private ComboBox<TagItem> tagCombo;
    @FXML private Button sourceToggleButton;

    private User currentUser;
    private String currentPlan;
    private Timer autoSaveTimer;
    private File tempFile;
    private static String pendingContent = null;


    /**
     * Инициализирует WebView - редактируемую область (contenteditable='true') для написания текста
     */
    @FXML
        public void initialize() {
            webEngine = webView.getEngine();//получение движка, управляющего отображением html внутри web view
            initEditor();//формирование html с редактируемым блоком
            fontSizeCombo.getItems().addAll(8, 10, 12, 14, 16, 18, 20, 24, 28, 32, 36, 48, 72);
            fontSizeCombo.setValue(12);
            textColorPicker.setOnAction(event -> handleTextColor());
            fontSizeCombo.setOnAction(event -> handleFontSize());
        // Инициализация ComboBox с тегами
        tagCombo.getItems().addAll(
                new TagItem("h1", "Заголовок 1 уровня"),
                new TagItem("h2", "Заголовок 2 уровня"),
                new TagItem("h3", "Заголовок 3 уровня"),
                new TagItem("h4", "Заголовок 4 уровня"),
                new TagItem("h5", "Заголовок 5 уровня"),
                new TagItem("h6", "Заголовок 6 уровня"),
                new TagItem("p", "Абзац"),
                new TagItem("a", "Гиперссылка"),
                new TagItem("img", "Изображение"),
                new TagItem("div", "Блок (контейнер)"),
                new TagItem("span", "Строчный контейнер"),
                new TagItem("table", "Таблица")
        );

        tagCombo.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(TagItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item.getTag());
                    setTooltip(new Tooltip(item.getDescription()));
                }
            }
        });

        tagCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(TagItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTag());
                }
            }
        });

        tagCombo.setValue(null);
        }

        private void initEditor() {
            String html = "<!DOCTYPE html>"// загрузка html в web view
                + "<html>"
                + "<head><meta charset='UTF-8'></head>"
                + "<body>"
                + "<div id='content' contenteditable='true' spellcheck='true' style='font-family: System;'>"
                + "</div>"
                + "</body>"
                + "</html>";
        webEngine.loadContent(html);
    }

    private void initTempFile() {
        if (currentUser != null) {
            tempFile = new File(System.getProperty("user.home"),
                    ".text_editor_autosave_" + currentUser.getId() + ".html");
            System.out.println("Создание temp file: " + tempFile.getAbsolutePath());
        } else {
            tempFile = new File(System.getProperty("user.home"), ".text_editor_autosave.html");
            System.out.println("Создание temp file: " + tempFile.getAbsolutePath());
        }
    }
    /**
     * Инициализирует пользователя с типом плана подписки. В соответствии с планом включает необходимые функции,
     * обновляет интерфейс и добавляет всплывающие подсказки в редакторе
     * @param user Объект пользователь, содержащий полную информацию
     * @param planType план подписки, дублирует getSubscriptionPlan для более быстрого доступа к плану подписки
     */
    public void initUser(User user, String planType) {
        this.currentUser = user;
        this.currentPlan = planType;
        setupEditorFeatures();
        updateUI();
        setupTooltips();
        initFonts();
        initTempFile();
        startAutoSave();

        Runnable restore = () -> {
            if (pendingContent != null && !pendingContent.isEmpty()) {
                webEngine.executeScript("document.getElementById('content').innerHTML = '" + escapeJS(pendingContent) + "'");
                System.out.println("Восстановлено из памяти");
                pendingContent = null;
            } else if (tempFile != null && tempFile.exists()) {
                try {
                    String saved = new String(Files.readAllBytes(tempFile.toPath()), StandardCharsets.UTF_8);
                    if (!saved.isEmpty()) {
                        webEngine.executeScript("document.getElementById('content').innerHTML = '" + escapeJS(saved) + "'");
                        System.out.println("Восстановлено из файла");
                    }
                } catch (Exception e) {}
            }
        };

        if (webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
            restore.run();
        } else {
            webEngine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
                if (state == Worker.State.SUCCEEDED) {
                    restore.run();
                }
            });
        }
    }

    private void startAutoSave() {
        if (autoSaveTimer != null) autoSaveTimer.cancel();
        autoSaveTimer = new Timer();
        autoSaveTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> autoSave());
            }
        }, 30000, 30000);
    }

    private void autoSave() {
        if (tempFile == null) return;
        try {
            Object exists = webEngine.executeScript("document.getElementById('content') != null");
            if (exists == null || !(Boolean) exists) return;
            String content = (String) webEngine.executeScript("document.getElementById('content').innerHTML");
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
                writer.write(content);
                System.out.println("Автосохранение выполнено");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleInsertTagFromCombo() {
        if (handlingTagInsert) {
            return;
        }

        TagItem selected = tagCombo.getValue();
        if (selected == null) {
            return;
        }

        if (!"VIP".equals(currentPlan)) {
            showAlert("Только для VIP", "Вставка HTML-тегов доступна по VIP-подписке");
            Platform.runLater(() -> tagCombo.setValue(null));
            return;
        }

        handlingTagInsert = true;
        try {
            String tag = selected.getTag();
            String html = null;

            switch (tag) {
                case "img":
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Выберите изображение");
                    fileChooser.getExtensionFilters().add(
                            new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg", "*.gif")
                    );
                    File imageFile = fileChooser.showOpenDialog(tagCombo.getScene().getWindow());
                    if (imageFile != null) {
                        insertImageIntoEditor(imageFile);
                    }
                    break;

                case "a":
                    TextInputDialog urlDialog = new TextInputDialog("https://");
                    urlDialog.setTitle("Вставить ссылку");
                    urlDialog.setHeaderText("Введите URL:");
                    Optional<String> urlResult = urlDialog.showAndWait();
                    if (urlResult.isPresent() && !urlResult.get().isEmpty()) {
                        html = "<a href='" + urlResult.get() + "'>Ссылка</a>";
                    }

                    break;

                case "table":
                    html = "<table border='1'><tr><td>Ячейка</td></tr></table>";
                    break;

                default:
                    html = "<" + tag + ">Ваш текст</" + tag + ">";
                    break;
            }

            if (html != null) {
                webEngine.executeScript("document.execCommand('insertHTML', false, '" + escapeJS(html) + "')");
            }
        } catch (Exception e) {
            showError("Ошибка вставки", "Не удалось вставить тег: " + e.getMessage());
        } finally {
            handlingTagInsert = false;
            Platform.runLater(() -> tagCombo.setValue(null));
        }
    }

    @FXML
    private void handleStatistics() {
        try {
            String text = (String) webEngine.executeScript("document.getElementById('content').innerText");
            if (text == null) text = "";

            int chars = text.length();
            String[] words = text.trim().split("\\s+");
            int wordCount = text.trim().isEmpty() ? 0 : words.length;

            String html = (String) webEngine.executeScript("document.getElementById('content').innerHTML");
            int paragraphs = (html.split("<p>").length - 1);
            if (paragraphs == 0 && !text.isEmpty()) paragraphs = 1;

            String message = String.format("Символов: %d\nСлов: %d\nАбзацев: %d", chars, wordCount, paragraphs);
            showInfo("Статистика документа", message);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось получить статистику.");
        }
    }

    private String escapeRegex(String s) {
        return s.replaceAll("([\\\\\\[\\]{}()*+?.^$|])", "\\\\$1");//экранирование спец символов
    }

    private String escapeJS(String s) {
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    @FXML
    private void handleFind() {
        String searchText = findField.getText();
        if (searchText.isEmpty()) return;
        String text = (String) webEngine.executeScript(
                "document.getElementById('content').innerText"
        );
        int count = text.split(escapeRegex(searchText), -1).length - 1;
        if (count == 0) {
            showInfo("Поиск", "Текст не найден.");
        } else {
            showInfo("Поиск", "Найдено вхождений: " + count);
        }
    }

    @FXML
    private void handleReplaceAll() {
        String searchText = findField.getText();
        String replaceText = replaceField.getText();
        if (searchText.isEmpty()) return;

        String html = (String) webEngine.executeScript(
                "document.getElementById('content').innerHTML"
        );
        String newHtml = html.replaceAll(escapeRegex(searchText), Matcher.quoteReplacement(replaceText));
        webEngine.executeScript(
                "document.getElementById('content').innerHTML = '" + escapeJS(newHtml) + "'"
        );
        showInfo("Замена", "Замена выполнена.");
    }

    @FXML
    private void handleOpen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Открыть документ");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("HTML files", "*.html"),
                new FileChooser.ExtensionFilter("DOCX files", "*.docx"),
                new FileChooser.ExtensionFilter("All files", "*.*")
        );

        File file = fileChooser.showOpenDialog(webView.getScene().getWindow());
        if (file == null) return;

        String fileName = file.getName().toLowerCase();
        try {
            if (fileName.endsWith(".html")) {
                if (currentPlan == null || currentPlan.equals("FREE") || currentPlan.equals("BASIC")) {
                    showError("Ошибка","Открытие HTML доступно только для VIP-подписки");
                    return;
                }
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                String wrappedHtml = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>" +
                        "<div id='content' contenteditable='true'>" + content + "</div></body></html>";
                webEngine.loadContent(wrappedHtml);

                webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
                    @Override
                    public void changed(ObservableValue<? extends Worker.State> obs, Worker.State old, Worker.State newState) {
                        if (newState == Worker.State.SUCCEEDED) {
                            autoSave();
                            webEngine.getLoadWorker().stateProperty().removeListener(this);
                        }
                    }
                });
                showInfo("Успех", "Файл открыт");
            } else if (fileName.endsWith(".docx")) {
                try (FileInputStream fis = new FileInputStream(file);
                     XWPFDocument doc = new XWPFDocument(fis)) {
                    StringBuilder text = new StringBuilder();
                    for (XWPFParagraph paragraph : doc.getParagraphs()) {
                        text.append(paragraph.getText()).append("\n");
                    }
                    String htmlContent = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>" +
                            "<div id='content' contenteditable='true'>" +
                            escapeHtml(text.toString()) + "</div></body></html>";
                    webEngine.loadContent(htmlContent);
                    webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
                        @Override
                        public void changed(ObservableValue<? extends Worker.State> obs, Worker.State old, Worker.State newState) {
                            if (newState == Worker.State.SUCCEEDED) {
                                autoSave();
                                webEngine.getLoadWorker().stateProperty().removeListener(this);
                            }
                        }
                    });
                    showInfo("Успех", "DOCX открыт (только текст)");
                }
            } else {
                showError("Неподдерживаемый формат", "Неподдерживаемый формат файла");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Ошибка открытия", e.getMessage());
        }
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\n", "<br>");
    }

    private void initFonts() {
        allFonts = Font.getFamilies();
        User.SubscriptionPlan plan = currentUser.getSubscriptionPlan();
        switch (plan) {
            case VIP:
                availableFonts = allFonts.subList(0, Math.min(30, allFonts.size()));
                break;
            case BASIC:
                availableFonts = allFonts.subList(0, Math.min(10, allFonts.size()));
                break;
            default:
                availableFonts = List.of("System", "Serif", "SansSerif");
                fontComboBox.setDisable(false);
                break;
        }
        fontComboBox.getItems().setAll(availableFonts);
        fontComboBox.setValue(availableFonts.get(0));
        fontComboBox.setOnAction(e -> applyFont(fontComboBox.getValue()));
    }

    private void applyFont(String fontFamily) {
        webEngine.executeScript(
                "document.getElementById('content').style.fontFamily = '" + fontFamily + "';"
        );
    }

    @FXML
    private void handleCancelSubscription() {
        if (currentUser.getSubscriptionPlan() == User.SubscriptionPlan.FREE) {
            showAlert("Информация", "У вас уже бесплатный план");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Отмена подписки");
        confirmAlert.setHeaderText("Вы уверены, что хотите отменить подписку?");
        confirmAlert.setContentText("Вы потеряете доступ ко всем платным функциям.\n\n" +
                "Остаток средств за неиспользованный период не возвращается.");

        ButtonType buttonYes = new ButtonType("Да, отменить");
        ButtonType buttonNo = new ButtonType("Нет, оставить", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmAlert.getButtonTypes().setAll(buttonYes, buttonNo);

        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == buttonYes) {

            try {
                showAlert("Информация", "Обработка запроса...");

                DatabaseService dbService = new DatabaseService();

                boolean updated = dbService.updateUserPlan(
                        currentUser.getId(),
                        "FREE",
                        null
                );


                if (updated) {
                    User refreshedUser = dbService.getUserById(currentUser.getId());

                    if (refreshedUser != null) {
                        currentUser = refreshedUser;
                        showAlert("Успех", "Подписка успешно отменена!\n\n" +
                                "Теперь у вас бесплатный план.");
                        updateUI();

                    } else {
                        showAlert("Ошибка", "Не удалось загрузить обновлённые данные");
                    }
                } else {
                    showAlert("Ошибка", "Не удалось отменить подписку.\nПопробуйте позже.");
                }

            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Ошибка", "Ошибка базы данных: " + e.getMessage());
            }
        }
    }

    /**
     * Устанавливает всплывающие подсказки ко всем кнопкам в редакторе
     * <p>Подсказки содержат описание комбинаций горячих клавиш для форматирования</p>
     */
    private void setupTooltips() {
        if (formatButton != null) {
            String tooltipText;
            if ("VIP".equals(currentPlan)) {
                tooltipText = "Сочетания клавиш:\n" +
                        "• Ctrl+B - Жирный текст\n" +
                        "• Ctrl+I - Курсив\n" +
                        "• Ctrl+U - Подчеркнутый\n" +
                        "• Ctrl+Z - Отменить\n" +
                        "• Ctrl+Y - Повторить\n\n" +
                        "Для создания веб-страниц вы можете использовать HTML-теги прямо в редакторе.\n" +
                        "Нажмите на эту кнопку для просмотра полного списка тегов.";
            } else {
                tooltipText = "Сочетания клавиш:\n" +
                        "• Ctrl+B - Жирный текст\n" +
                        "• Ctrl+I - Курсив\n" +
                        "• Ctrl+U - Подчеркнутый\n" +
                        "• Ctrl+Z - Отменить\n" +
                        "• Ctrl+Y - Повторить";
            }
            Tooltip formatTooltip = new Tooltip(tooltipText);
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

    }

    /**
     * Обновляет кнопки на экране текстового редактора в соответствии с планом подписки
     */
    private void setupEditorFeatures() {
        System.out.println("setupEditorFeatures called with plan: " + currentPlan);
        switch (currentPlan) {
            case "FREE":
                formatButton.setDisable(true);
                insertImageButton.setDisable(true);
                if (boldButton != null) boldButton.setDisable(true);
                if (italicButton != null) italicButton.setDisable(true);
                if (underlineButton != null) underlineButton.setDisable(true);
                if (strikeButton != null) strikeButton.setDisable(true);
                if (alignLeftButton != null) alignLeftButton.setDisable(true);
                if (alignCenterButton != null) alignCenterButton.setDisable(true);
                if (alignRightButton != null) alignRightButton.setDisable(true);
                if (orderedListButton != null) orderedListButton.setDisable(true);
                if (unorderedListButton != null) unorderedListButton.setDisable(true);
                if (textColorPicker != null) textColorPicker.setDisable(true);
                if (fontSizeCombo != null) fontSizeCombo.setDisable(true);
                if (tagCombo != null) tagCombo.setDisable(true);
                if (sourceToggleButton != null) sourceToggleButton.setDisable(true);
                break;

            case "BASIC":
                formatButton.setDisable(false);
                insertImageButton.setDisable(true);
                if (boldButton != null) boldButton.setDisable(false);
                if (italicButton != null) italicButton.setDisable(false);
                if (underlineButton != null) underlineButton.setDisable(false);
                if (strikeButton != null) strikeButton.setDisable(false);
                if (alignLeftButton != null) alignLeftButton.setDisable(false);
                if (alignCenterButton != null) alignCenterButton.setDisable(false);
                if (alignRightButton != null) alignRightButton.setDisable(false);
                if (orderedListButton != null) orderedListButton.setDisable(false);
                if (unorderedListButton != null) unorderedListButton.setDisable(false);
                if (textColorPicker != null) textColorPicker.setDisable(true);
                if (fontSizeCombo != null) fontSizeCombo.setDisable(true);
                if (tagCombo != null) tagCombo.setDisable(true);
                if (sourceToggleButton != null) sourceToggleButton.setDisable(true);
                break;

            case "VIP":
                formatButton.setDisable(false);
                insertImageButton.setDisable(false);
                if (boldButton != null) boldButton.setDisable(false);
                if (italicButton != null) italicButton.setDisable(false);
                if (underlineButton != null) underlineButton.setDisable(false);
                if (strikeButton != null) strikeButton.setDisable(false);
                if (alignLeftButton != null) alignLeftButton.setDisable(false);
                if (alignCenterButton != null) alignCenterButton.setDisable(false);
                if (alignRightButton != null) alignRightButton.setDisable(false);
                if (orderedListButton != null) orderedListButton.setDisable(false);
                if (unorderedListButton != null) unorderedListButton.setDisable(false);
                if (textColorPicker != null) textColorPicker.setDisable(false);
                if (fontSizeCombo != null) fontSizeCombo.setDisable(false);
                if (tagCombo != null) tagCombo.setDisable(false);
                if (sourceToggleButton != null) sourceToggleButton.setDisable(false);
                break;
        }
        System.out.println("Доступные функции для плана: " + currentPlan);
    }

    @FXML private void handleBold() {
        webEngine.executeScript("document.execCommand('bold', false, null)");
    }
    @FXML private void handleItalic() {
        webEngine.executeScript("document.execCommand('italic', false, null)");
    }
    @FXML private void handleUnderline() {
        webEngine.executeScript("document.execCommand('underline', false, null)");
    }
    @FXML private void handleStrikethrough() {
        webEngine.executeScript("document.execCommand('strikeThrough', false, null)");
    }
    @FXML private void handleAlignLeft() {
        webEngine.executeScript("document.execCommand('justifyLeft', false, null)");
    }
    @FXML private void handleAlignCenter() {
        webEngine.executeScript("document.execCommand('justifyCenter', false, null)");
    }
    @FXML private void handleAlignRight() {
        webEngine.executeScript("document.execCommand('justifyRight', false, null)");//диалог и дополнительные параметры
    }
    @FXML private void handleOrderedList() {
        webEngine.executeScript("document.execCommand('insertOrderedList', false, null)");
    }
    @FXML private void handleUnorderedList() {
        webEngine.executeScript("document.execCommand('insertUnorderedList', false, null)");
    }
    @FXML private void handleTextColor() {
        Color color = textColorPicker.getValue();
        if (color != null) {
            String hex = String.format("#%02X%02X%02X",
                    (int)(color.getRed() * 255),
                    (int)(color.getGreen() * 255),
                    (int)(color.getBlue() * 255));
            webEngine.executeScript("document.execCommand('foreColor', false, '" + hex + "')");
        }
    }
    @FXML private void handleFontSize() {
        Integer size = fontSizeCombo.getValue();
        if (size != null) {
            webEngine.executeScript("document.execCommand('fontSize', false, '" + size + "')");
        }
    }

    @FXML
    private void handleToggleSource() {
        if (!"VIP".equals(currentPlan)) {
            showAlert("Только для VIP", "Редактирование HTML-кода доступно по VIP-подписке");
            return;
        }
        Object mode = webEngine.executeScript(
                "document.getElementById('sourceMode') ? 'source' : 'visual'"
        );
        if ("visual".equals(mode)) {
            webEngine.executeScript(
                    "var html = document.getElementById('content').innerHTML;" +
                            "var textarea = document.createElement('textarea');" +
                            "textarea.id = 'sourceMode';" +
                            "textarea.style.width='100%'; textarea.style.height='100%';" +
                            "textarea.value = html;" +
                            "document.body.innerHTML = '';" +
                            "document.body.appendChild(textarea);"
            );
        } else {
            webEngine.executeScript(
                    "var textarea = document.getElementById('sourceMode');" +
                            "var html = textarea.value;" +
                            "document.body.innerHTML = '<div id=\"content\" contenteditable=\"true\">' + html + '</div>';"
            );
        }
    }

    /**
     * Добавляет информационное сообщение с названием текущего плана подписки
     */
    private void updateUI() {
        subscriptionLabel.setText("План: " + currentPlan);
        if ("VIP".equals(currentPlan)) {
            saveButton.setTooltip(new Tooltip("Сохранить как DOCX или HTML (с поддержкой HTML-тегов)"));
            if (formatButton != null && formatButton.getTooltip() != null) {
                formatButton.getTooltip().setText(
                        "Сочетания клавиш:\n" +
                                "• Ctrl+B - Жирный текст\n" +
                                "• Ctrl+I - Курсив\n" +
                                "• Ctrl+U - Подчеркнутый\n" +
                                "• Ctrl+Z - Отменить\n" +
                                "• Ctrl+Y - Повторить\n\n" +
                                "Для создания веб-страниц вы можете использовать HTML-теги прямо в редакторе.\n" +
                                "Нажмите на эту кнопку для просмотра полного списка тегов."
                );
            }
        } else {
            saveButton.setTooltip(new Tooltip("Сохранить как DOCX"));
            if (formatButton != null && formatButton.getTooltip() != null) {
                formatButton.getTooltip().setText(
                        "Сочетания клавиш:\n" +
                                "• Ctrl+B - Жирный текст\n" +
                                "• Ctrl+I - Курсив\n" +
                                "• Ctrl+U - Подчеркнутый\n" +
                                "• Ctrl+Z - Отменить\n" +
                                "• Ctrl+Y - Повторить"
                );
            }
        }
    }
    /**
     * Позволяет осуществить вставку изображения с определённым форматом в текст
     */
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

    /**
     * Вставляет изображение в текстовый редактор с предустановленными параметрами.
     * <p>
     * Изображение добавляется в конец содержимого с максимальной шириной 300px
     * и отступом 10px со всех сторон.
     * </p>
     *
     * @param imageFile файл изображения для вставки
     */
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

    /**
     * Сохраняет текст в файле формата .html
     */
    @FXML
    private void handleSave() {
        if ("FREE".equals(currentPlan) || "BASIC".equals(currentPlan)) {
            saveAsDocx();
        } else if ("VIP".equals(currentPlan)) {
            ChoiceDialog<String> dialog = new ChoiceDialog<>("DOCX", "DOCX", "HTML");
            dialog.setTitle("Выбор формата");
            dialog.setHeaderText("Выберите формат сохранения");
            dialog.setContentText("Формат:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(format -> {
                if ("DOCX".equals(format)) {
                    saveAsDocx();
                } else if ("HTML".equals(format)) {
                    showHtmlHelp();
                    saveAsHtml();
                }
            });
        }
    }
    private void showHtmlHelp() {
        Alert helpAlert = new Alert(Alert.AlertType.INFORMATION);
        helpAlert.setTitle("Справка по HTML");
        helpAlert.setHeaderText("Вы можете создавать веб-страницы, используя HTML-теги прямо в редакторе!");
        helpAlert.setContentText(
                "Основные HTML-теги:\n\n" +
                        "• <h1>...<h6> – заголовки\n" +
                        "• <p> – абзац\n" +
                        "• <b> или <strong> – жирный текст\n" +
                        "• <i> или <em> – курсив\n" +
                        "• <u> – подчёркнутый\n" +
                        "• <a href='url'>ссылка</a> – гиперссылка\n" +
                        "• <img src='image.jpg'> – изображение\n" +
                        "• <ul><li>...</li></ul> – маркированный список\n" +
                        "• <ol><li>...</li></ol> – нумерованный список\n" +
                        "• <div> – блок, <span> – встроенный элемент\n" +
                        "• <br> – перенос строки\n\n" +
                        "Просто вводите эти теги в редакторе, и они сохранятся в HTML-файле."
        );
        helpAlert.showAndWait();
    }

    private void saveAsDocx() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("DOCX Files", "*.docx")
        );
        File file = fileChooser.showSaveDialog(webView.getScene().getWindow());

        if (file != null) {
            try (XWPFDocument document = new XWPFDocument();
                 FileOutputStream out = new FileOutputStream(file)) {

                String htmlContent = (String) webEngine.executeScript(
                        "document.getElementById('content').innerHTML"
                );
                String plainText = htmlContent.replaceAll("<[^>]*>", "").trim();

                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(plainText);
                run.setFontSize(12);

                document.write(out);
                showInfo("Успех", "Файл сохранён как DOCX");
            } catch (Exception e) {
                e.printStackTrace();
                showError("Ошибка сохранения", "Не удалось сохранить DOCX: " + e.getMessage());
            }
        }
    }

    private void saveAsHtml() {
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
                showInfo("Успех", "Файл сохранён как HTML");
            } catch (IOException e) {
                showError("Ошибка сохранения", e.getMessage());
            }
        }
    }

    /**
     * Открывает экран выбора подписки при клике в текстовом редакторе по кнопке "Сменить план"
     */
    @FXML
    private void handleUpgrade() {
        try {
            if (webEngine.getLoadWorker().getState() != Worker.State.SUCCEEDED) {
                CountDownLatch latch = new CountDownLatch(1);
                webEngine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
                    if (state == Worker.State.SUCCEEDED) {
                        latch.countDown();
                    }
                });
                latch.await(1, TimeUnit.SECONDS);
            }

            Object exists = webEngine.executeScript("document.getElementById('content') != null");
            if (exists != null && (Boolean) exists) {
                pendingContent = (String) webEngine.executeScript("document.getElementById('content').innerHTML");
                System.out.println("Текст сохранён в память");

                if (pendingContent != null && tempFile != null) {
                    try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
                        writer.write(pendingContent);
                        System.out.println("Сохранён в файл: " + tempFile.getAbsolutePath());
                    }
                }
            } else {
                System.out.println("Элемент content не найден после ожидания");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/subscription_plans.fxml"));
            Parent root = loader.load();
            SubscriptionPlansController controller = loader.getController();
            controller.initUser(currentUser);

            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Ошибка","Ошибка загрузки планов подписки");
            e.printStackTrace();
        }
    }

    /**
     * Открывает экран выбора планов подписки по нажатию в текстовом редакторе кнопки "Вернуться"
     */
    @FXML
    private void handleBack() {
        try {
            if (webEngine.getLoadWorker().getState() != Worker.State.SUCCEEDED) {
                CountDownLatch latch = new CountDownLatch(1);
                webEngine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
                    if (state == Worker.State.SUCCEEDED) {
                        latch.countDown();
                    }
                });
                latch.await(1, TimeUnit.SECONDS);
            }

            Object exists = webEngine.executeScript("document.getElementById('content') != null");
            if (exists != null && (Boolean) exists) {
                pendingContent = (String) webEngine.executeScript("document.getElementById('content').innerHTML");
                System.out.println("Текст сохранён в память");

                if (pendingContent != null && tempFile != null) {
                    try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
                        writer.write(pendingContent);
                        System.out.println("Сохранён в файл: " + tempFile.getAbsolutePath());
                    }
                }
            } else {
                System.out.println("Элемент content не найден после ожидания");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/subscription_plans.fxml"));
            Parent root = loader.load();
            SubscriptionPlansController controller = loader.getController();
            controller.initUser(currentUser);

            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            showError("Ошибка","Ошибка загрузки планов подписки");
            e.printStackTrace();
        }
    }

    /**
     * Показывает информационное сообщение по клику на кнопку "Форматирование" для пользователей с планом Базовый или Премиум. Если план пользователя Бесплатный,
     * то кнопка неактивна
     */
    @FXML
    private void handleFormat() {
        if (!"FREE".equals(currentPlan)) {
            if ("VIP".equals(currentPlan)) {
                showHtmlHelp();
            } else {
                showInfo("Форматирование", "Используйте сочетания клавиш для форматирования");
            }
        }
    }

    @FXML
    public void shutdown() {
        if (autoSaveTimer != null) autoSaveTimer.cancel();
    }

    /**
     * Показывает диалоговое окно с предупреждением
     * @param title заголовок
     * @param message текст предупреждения
     */
    private void showAlert(String title, String message) {
        new Alert(Alert.AlertType.WARNING, message, ButtonType.OK).show();
    }

    /**
     * Показывает диалоговое окно с сообщением об ошибке
     * @param title заголовок
     * @param message текст пойманной ошибки
     */
    private void showError(String title, String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).show();
    }

    /**
     * Показывает диалоговое окно с оповещением
     * @param title заголовок
     * @param message текст информационного оповещения
     */
    private void showInfo(String title, String message) {
        new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK).show();
    }
    public class TagItem {
        private final String tag;
        private final String description;

        public TagItem(String tag, String description) {
            this.tag = tag;
            this.description = description;
        }

        public String getTag() { return tag; }
        public String getDescription() { return description; }

        @Override
        public String toString() {
            return tag;
        }
    }
}