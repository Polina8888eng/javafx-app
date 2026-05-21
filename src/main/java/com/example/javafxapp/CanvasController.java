package com.example.javafxapp;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.*;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import static com.example.service.DatabaseService.logger;

/**
 * Простой графический редактор с поддержкой рисования карандашом,
 * ластиком, создания геометрических фигур, их перемещения,
 * заливки цветом и сохранения рисунка.
 */
public class CanvasController extends Application {

  /** Слой для рисования произвольных линий (карандаш, ластик). */
  private Canvas canvasLayer;
  /** Слой для векторных фигур. */
  private Pane shapeLayer;
  /** Слой для анимированных пузырьков (декоративных эффектов). */
  private Pane bubbleLayer;
  /** Контекст рисования на Canvas. */
  private GraphicsContext gc;
  /** Главное окно приложения. */

  /**
   * Название активного инструмента (PEN, ERASER, SELECT, RECT, CIRCLE, ELLIPSE,
   * LINE, TRIANGLE).
   */
  private String currentTool = "PEN";

  /** Начальные координаты нажатия мыши. */
  private double startX, startY;
  /** Временная фигура, которая строится при перетаскивании. */
  private Shape tempShape;

  /** Выделенная фигура для перемещения или заливки. */
  private Shape selectedShape;
  /** Точка, относительно которой начато перемещение выделенной фигуры. */
  private Point2D dragAnchor;
  /** Список якорей для изменения размеров фигуры. */
  private final List<Anchor> anchors = new ArrayList<>();

  /** Выбор цвета для рисования и заливки. */
  private ColorPicker colorPicker;
  /** Генератор случайных чисел для эффектов. */
  private final Random random = new Random();

  /** Время последнего создания пузырька (для ограничения частоты). */
  private long lastBubbleTime = 0;
  /** Минимальная задержка между созданием пузырьков в миллисекундах. */
  private static final long BUBBLE_DELAY_MS = 15;

  /** Доступная ширина области рисования. */
  private double drawAreaWidth;
  /** Доступная высота области рисования. */
  private double drawAreaHeight;

  /**
   * Точка входа в приложение. Создаёт главное окно, компонует слои и панели
   * инструментов.
   *
   * @param primaryStage главное окно, предоставляемое JavaFX.
   */
  @Override
  public void start(Stage primaryStage) {
    Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
    double toolbarWidth = 150;
    double topBarHeight = 80;
    drawAreaWidth = screenBounds.getWidth() - toolbarWidth;
    drawAreaHeight = screenBounds.getHeight() - topBarHeight;

    createLayers();

    BorderPane root = new BorderPane();
    VBox toolBar = createToolBar();
    root.setLeft(toolBar);
    HBox topBar = createTopBar();
    root.setTop(topBar);
    Pane centerPane = new Pane();
    centerPane.getChildren().addAll(canvasLayer, shapeLayer, bubbleLayer);
    root.setCenter(centerPane);

    setupMouseHandlers();

    Scene scene = new Scene(root, drawAreaWidth + toolbarWidth, drawAreaHeight + topBarHeight);
    primaryStage.setMaximized(true);
    primaryStage.setTitle("Графический редактор");
    primaryStage.setScene(scene);
    primaryStage.show();
  }

  /**
   * Инициализирует три слоя: холст для рисования, слой фигур и слой пузырьков.
   */
  private void createLayers() {
    canvasLayer = new Canvas(drawAreaWidth, drawAreaHeight);
    gc = canvasLayer.getGraphicsContext2D();
    gc.setFill(Color.WHITE);
    gc.fillRect(0, 0, canvasLayer.getWidth(), canvasLayer.getHeight());

    shapeLayer = new Pane();
    shapeLayer.setPrefSize(drawAreaWidth, drawAreaHeight);
    shapeLayer.setStyle("-fx-background-color: transparent;");

    bubbleLayer = new Pane();
    bubbleLayer.setPrefSize(drawAreaWidth, drawAreaHeight);
    bubbleLayer.setStyle("-fx-background-color: transparent;");
    bubbleLayer.setMouseTransparent(true);
  }

  /**
   * Создаёт верхнюю панель с выбором цвета, кнопками заливки, очистки и
   * сохранения.
   *
   * @return горизонтальный контейнер с элементами управления.
   */
  private HBox createTopBar() {
    HBox topBar = new HBox(10);
    topBar.setStyle("-fx-padding: 10; -fx-background-color: #f0f0f0;");

    Label colorLabel = new Label("Цвет:");
    colorPicker = new ColorPicker(Color.BLACK);

    Label fillLabel = new Label("Заливка:");
    Button fillButton = new Button("Залить выделенную фигуру");
    fillButton.setOnAction(e -> fillSelectedShape());

    Button clearCanvasBtn = new Button("Очистить рисунок");
    clearCanvasBtn.setOnAction(e -> clearCanvas());

    Button clearShapesBtn = new Button("Удалить все фигуры");
    clearShapesBtn.setOnAction(e -> shapeLayer.getChildren().clear());

    Button clearBubblesBtn = new Button("Очистить пузырьки");
    clearBubblesBtn.setOnAction(e -> bubbleLayer.getChildren().clear());

    Button saveImageBtn = new Button("💾 Сохранить рисунок");
    saveImageBtn.setOnAction(e -> saveDrawing());

    topBar.getChildren().addAll(colorLabel, colorPicker, fillLabel, fillButton, clearCanvasBtn,
        clearShapesBtn, clearBubblesBtn, saveImageBtn);

    return topBar;
  }

  /**
   * Создаёт боковую панель с переключаемыми инструментами рисования и выделения.
   *
   * @return вертикальный контейнер с радиокнопками инструментов.
   */
  private VBox createToolBar() {
    VBox toolBar = new VBox(10);
    toolBar.setStyle("-fx-padding: 10; -fx-background-color: #e0e0e0;");

    ToggleGroup tools = new ToggleGroup();

    RadioButton penTool = new RadioButton("✏️ Карандаш");
    penTool.setToggleGroup(tools);
    penTool.setSelected(true);
    penTool.setOnAction(e -> currentTool = "PEN");

    RadioButton eraserTool = new RadioButton("🧽 Ластик");
    eraserTool.setToggleGroup(tools);
    eraserTool.setOnAction(e -> currentTool = "ERASER");

    toolBar.getChildren().add(new Separator());

    Label shapeLabel = new Label("Фигуры (тяните мышкой)");
    shapeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

    RadioButton rectTool = new RadioButton("⬜ Прямоугольник");
    rectTool.setToggleGroup(tools);
    rectTool.setOnAction(e -> currentTool = "RECT");

    RadioButton circleTool = new RadioButton("⚪ Круг");
    circleTool.setToggleGroup(tools);
    circleTool.setOnAction(e -> currentTool = "CIRCLE");

    RadioButton ellipseTool = new RadioButton("🔘 Эллипс");
    ellipseTool.setToggleGroup(tools);
    ellipseTool.setOnAction(e -> currentTool = "ELLIPSE");

    RadioButton lineTool = new RadioButton("➖ Линия");
    lineTool.setToggleGroup(tools);
    lineTool.setOnAction(e -> currentTool = "LINE");

    RadioButton triangleTool = new RadioButton("🔺 Треугольник");
    triangleTool.setToggleGroup(tools);
    triangleTool.setOnAction(e -> currentTool = "TRIANGLE");

    toolBar.getChildren().add(new Separator());

    RadioButton selectTool = new RadioButton("🖱️ Выделить/Переместить");
    selectTool.setToggleGroup(tools);
    selectTool.setOnAction(e -> currentTool = "SELECT");

    toolBar.getChildren().addAll(
        new Label("Инструменты рисования:"),
        penTool, eraserTool,
        shapeLabel,
        rectTool, circleTool, ellipseTool, lineTool, triangleTool,
        selectTool);

    return toolBar;
  }

  /**
   * Сохраняет текущее содержимое области рисования (холст + фигуры) в файл PNG.
   * Перед снимком временно объединяет слои, затем возвращает их на место.
   */
  private void saveDrawing() {
    Stage stage = null;
    if (canvasLayer.getScene() != null) {
      stage = (Stage) canvasLayer.getScene().getWindow();
    }

    if (stage == null) {
      Alert alert = new Alert(Alert.AlertType.ERROR);
      alert.setTitle("Ошибка");
      alert.setContentText("Не удалось получить доступ к окну приложения");
      alert.showAndWait();
      return;
    }

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Сохранить рисунок");
    fileChooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("PNG Image", "*.png"));
    File file = fileChooser.showSaveDialog(stage);
    if (file == null)
      return;

    try {
      Pane oldParentCanvas = (Pane) canvasLayer.getParent();
      Pane oldParentShape = (Pane) shapeLayer.getParent();
      int canvasIndex = oldParentCanvas != null ? oldParentCanvas.getChildren().indexOf(canvasLayer)
          : -1;
      int shapeIndex = oldParentShape != null ? oldParentShape.getChildren().indexOf(shapeLayer)
          : -1;

      Group snapshotGroup = new Group();
      snapshotGroup.getChildren().addAll(canvasLayer, shapeLayer);

      WritableImage snapshot = snapshotGroup.snapshot(new SnapshotParameters(), null);

      if (oldParentCanvas != null) {
        if (canvasIndex >= 0 && canvasIndex <= oldParentCanvas.getChildren().size()) {
          oldParentCanvas.getChildren().add(canvasIndex, canvasLayer);
        } else {
          oldParentCanvas.getChildren().add(canvasLayer);
        }
      }
      if (oldParentShape != null) {
        if (shapeIndex >= 0 && shapeIndex <= oldParentShape.getChildren().size()) {
          oldParentShape.getChildren().add(shapeIndex, shapeLayer);
        } else {
          oldParentShape.getChildren().add(shapeLayer);
        }
      }

      BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);
      ImageIO.write(bufferedImage, "png", file);

      Alert info = new Alert(Alert.AlertType.INFORMATION);
      info.setTitle("Успех");
      info.setContentText("Рисунок сохранён в " + file.getName());
      info.showAndWait();
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Ошибка при выполнении запроса", e);
      Alert alert = new Alert(Alert.AlertType.ERROR);
      alert.setTitle("Ошибка сохранения");
      alert.setContentText(e.getMessage());
      alert.showAndWait();
    }
  }

  /**
   * Назначает обработчики мыши на слой фигур.
   */
  private void setupMouseHandlers() {
    shapeLayer.setOnMousePressed(this::handleMousePressed);
    shapeLayer.setOnMouseDragged(this::handleMouseDragged);
    shapeLayer.setOnMouseReleased(this::handleMouseReleased);
  }

  /**
   * Обрабатывает нажатие кнопки мыши: запускает создание фигуры, начало рисования
   * или выделение в зависимости от активного инструмента.
   *
   * @param e событие мыши.
   */
  private void handleMousePressed(MouseEvent e) {
    if (e.getX() < 0 || e.getX() > drawAreaWidth || e.getY() < 0 || e.getY() > drawAreaHeight) {
      return;
    }

    startX = e.getX();
    startY = e.getY();

    switch (currentTool) {
      case "PEN" :
        gc.setStroke(colorPicker.getValue());
        gc.setLineWidth(2);
        gc.beginPath();
        gc.moveTo(startX, startY);
        createBubbleAt(startX, startY, colorPicker.getValue());
        break;

      case "ERASER" :
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(20);
        gc.beginPath();
        gc.moveTo(startX, startY);
        createBubbleAt(startX, startY, Color.rgb(200, 200, 200, 0.8));
        break;

      case "SELECT" :
        for (javafx.scene.Node node : shapeLayer.getChildren()) {
          if (node instanceof Shape shape && node != tempShape && !(node instanceof Anchor)) {
            shape.setStroke(Color.BLACK);
            shape.setStrokeWidth(2);
          }
        }
        hideAnchors();

        selectedShape = findShapeAt(e.getX(), e.getY());
        if (selectedShape != null) {
          dragAnchor = new Point2D(e.getX(), e.getY());
          selectedShape.setStroke(Color.BLUE);
          selectedShape.setStrokeWidth(3);
          showAnchors(selectedShape);
        }
        break;

      case "RECT" :
        tempShape = new Rectangle(startX, startY, 0, 0);
        configureTempShape(tempShape);
        shapeLayer.getChildren().add(tempShape);
        break;

      case "CIRCLE" :
        tempShape = new Circle(startX, startY, 0);
        configureTempShape(tempShape);
        shapeLayer.getChildren().add(tempShape);
        break;

      case "ELLIPSE" :
        tempShape = new Ellipse(startX, startY, 0, 0);
        configureTempShape(tempShape);
        shapeLayer.getChildren().add(tempShape);
        break;

      case "LINE" :
        tempShape = new Line(startX, startY, startX, startY);
        configureTempShape(tempShape);
        shapeLayer.getChildren().add(tempShape);
        break;

      case "TRIANGLE" :
        Polygon triangle = new Polygon();
        triangle.getPoints().addAll(startX, startY, startX, startY, startX, startY);
        tempShape = triangle;
        configureTempShape(tempShape);
        shapeLayer.getChildren().add(tempShape);
        break;

      case "default" :
        break;
    }
  }

  /**
   * Настраивает временную фигуру: прозрачная заливка, чёрная пунктирная обводка.
   *
   * @param shape фигура для настройки.
   */
  private void configureTempShape(Shape shape) {
    shape.setFill(Color.TRANSPARENT);
    shape.setStroke(Color.BLACK);
    shape.setStrokeWidth(2);
    shape.getStrokeDashArray().addAll(5.0, 5.0);
  }

  /**
   * Обрабатывает перетаскивание мыши: рисует линии, изменяет временную фигуру или
   * перемещает выделенную.
   *
   * @param e событие мыши.
   */
  private void handleMouseDragged(MouseEvent e) {
    double x = Math.min(Math.max(e.getX(), 0), drawAreaWidth);
    double y = Math.min(Math.max(e.getY(), 0), drawAreaHeight);

    switch (currentTool) {
      case "PEN" :
        gc.lineTo(x, y);
        gc.stroke();
        createBubbleAt(x, y, colorPicker.getValue());
        break;

      case "ERASER" :
        gc.lineTo(x, y);
        gc.stroke();
        createBubbleAt(x, y, Color.rgb(200, 200, 200, 0.7));
        break;

      case "SELECT" :
        if (selectedShape != null && dragAnchor != null) {
          double dx = x - dragAnchor.getX();
          double dy = y - dragAnchor.getY();
          moveShape(selectedShape, dx, dy);
          dragAnchor = new Point2D(x, y);
          updateAnchors(selectedShape);
          createBubbleAt(x, y, Color.rgb(100, 200, 255, 0.6));
        }
        break;

      case "RECT" :
        if (tempShape instanceof Rectangle) {
          updateRectangle((Rectangle) tempShape, startX, startY, x, y);
          createBubbleAt(x, y, colorPicker.getValue());
        }
        break;

      case "CIRCLE" :
        if (tempShape instanceof Circle) {
          updateCircle((Circle) tempShape, startX, startY, x, y);
          createBubbleAt(x, y, colorPicker.getValue());
        }
        break;

      case "ELLIPSE" :
        if (tempShape instanceof Ellipse) {
          updateEllipse((Ellipse) tempShape, startX, startY, x, y);
          createBubbleAt(x, y, colorPicker.getValue());
        }
        break;

      case "LINE" :
        if (tempShape instanceof Line line) {
          line.setEndX(x);
          line.setEndY(y);
          createBubbleAt(x, y, colorPicker.getValue());
        }
        break;

      case "TRIANGLE" :
        if (tempShape instanceof Polygon tri) {
          tri.getPoints().clear();
          tri.getPoints().addAll(startX, startY, x, startY, startX, y);
          createBubbleAt(x, y, colorPicker.getValue());
        }
        break;
      case "default" :
        break;
    }
  }

  /**
   * Обрабатывает отпускание кнопки мыши: завершает создание временной фигуры,
   * проверяя её минимальные размеры, и удаляет слишком маленькие.
   *
   * @param e событие мыши.
   */
  private void handleMouseReleased(MouseEvent e) {
    double mouseX = Math.min(Math.max(e.getX(), 0), drawAreaWidth);
    double mouseY = Math.min(Math.max(e.getY(), 0), drawAreaHeight);

    switch (currentTool) {
      case "PEN" :
      case "ERASER" :
        break;

      case "RECT" :
        finalizeShape(tempShape, 5, 5, mouseX, mouseY);
        tempShape = null;
        break;

      case "CIRCLE" :
        finalizeShape(tempShape, 5, 5, mouseX, mouseY);
        tempShape = null;
        break;

      case "ELLIPSE" :
        finalizeShape(tempShape, 5, 5, mouseX, mouseY);
        tempShape = null;
        break;

      case "LINE" :
        finalizeShape(tempShape, 2, 2, mouseX, mouseY);
        tempShape = null;
        break;

      case "TRIANGLE" :
        finalizeShape(tempShape, 5, 5, mouseX, mouseY);
        tempShape = null;
        break;

      case "default" :
        break;
    }
  }

  /**
   * Проверяет, удовлетворяет ли фигура минимальным размерам, и либо оставляет её
   * с постоянной обводкой, либо удаляет. При успехе создаёт всплеск пузырьков.
   *
   * @param shape временная фигура.
   * @param minWidth минимальная ширина/радиус.
   * @param minHeight минимальная высота/радиус (для эллипса и т.п.).
   * @param mouseX координата X мыши в момент отпускания.
   * @param mouseY координата Y мыши в момент отпускания.
   */
  private void finalizeShape(Shape shape, double minWidth, double minHeight, double mouseX,
      double mouseY) {
    if (shape == null)
      return;
    boolean valid = false;
    if (shape instanceof Rectangle rect) {
      if (rect.getWidth() > minWidth && rect.getHeight() > minHeight)
        valid = true;
    } else if (shape instanceof Circle circle) {
      if (circle.getRadius() > minWidth)
        valid = true;
    } else if (shape instanceof Ellipse ellipse) {
      if (ellipse.getRadiusX() > minWidth && ellipse.getRadiusY() > minHeight)
        valid = true;
    } else if (shape instanceof Line line) {
      double len = Math.hypot(line.getEndX() - line.getStartX(), line.getEndY() - line.getStartY());
      if (len > minWidth)
        valid = true;
    } else if (shape instanceof Polygon poly) {
      if (poly.getPoints().size() >= 6) {
        double x1 = poly.getPoints().get(0), y1 = poly.getPoints().get(1);
        double x2 = poly.getPoints().get(2), y2 = poly.getPoints().get(3);
        double x3 = poly.getPoints().get(4), y3 = poly.getPoints().get(5);
        double area = Math.abs((x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2)) / 2.0);
        if (area > 5)
          valid = true;
      }
    }

    if (valid) {
      shape.setFill(Color.TRANSPARENT);
      shape.setStroke(Color.BLACK);
      shape.setStrokeWidth(2);
      shape.getStrokeDashArray().clear();
      for (int i = 0; i < 20; i++) {
        createBubbleAt(mouseX + random.nextInt(60) - 30,
            mouseY + random.nextInt(60) - 30,
            colorPicker.getValue());
      }
    } else {
      shapeLayer.getChildren().remove(shape);
    }
  }

  /**
   * Обновляет положение и размеры прямоугольника по координатам начала и текущей
   * точки.
   *
   * @param rect прямоугольник.
   * @param sx начальная X.
   * @param sy начальная Y.
   * @param ex текущая X.
   * @param ey текущая Y.
   */
  private void updateRectangle(Rectangle rect, double sx, double sy, double ex, double ey) {
    double x = Math.min(sx, ex);
    double y = Math.min(sy, ey);
    double w = Math.abs(ex - sx);
    double h = Math.abs(ey - sy);
    rect.setX(Math.max(0, x));
    rect.setY(Math.max(0, y));
    rect.setWidth(Math.min(w, drawAreaWidth - rect.getX()));
    rect.setHeight(Math.min(h, drawAreaHeight - rect.getY()));
  }

  /**
   * Обновляет радиус круга, ограничивая его границами области рисования.
   *
   * @param circle круг.
   * @param sx начальная X (центр остаётся прежним).
   * @param sy начальная Y (центр остаётся прежним).
   * @param ex текущая X.
   * @param ey текущая Y.
   */
  private void updateCircle(Circle circle, double sx, double sy, double ex, double ey) {
    double dx = ex - sx;
    double dy = ey - sy;
    double radius = Math.sqrt(dx * dx + dy * dy);
    double maxRadiusX = Math.min(sx, drawAreaWidth - sx);
    double maxRadiusY = Math.min(sy, drawAreaHeight - sy);
    radius = Math.min(radius, Math.min(maxRadiusX, maxRadiusY));
    circle.setRadius(radius);
    circle.setCenterX(sx);
    circle.setCenterY(sy);
  }

  /**
   * Обновляет полуоси эллипса, сохраняя центр между начальной и конечной точкой,
   * с учётом границ области.
   *
   * @param ellipse эллипс.
   * @param sx начальная X.
   * @param sy начальная Y.
   * @param ex текущая X.
   * @param ey текущая Y.
   */
  private void updateEllipse(Ellipse ellipse, double sx, double sy, double ex, double ey) {
    double rx = Math.abs(ex - sx) / 2;
    double ry = Math.abs(ey - sy) / 2;
    double cx = (sx + ex) / 2;
    double cy = (sy + ey) / 2;

    if (cx - rx < 0)
      rx = cx;
    if (cx + rx > drawAreaWidth)
      rx = drawAreaWidth - cx;
    if (cy - ry < 0)
      ry = cy;
    if (cy + ry > drawAreaHeight)
      ry = drawAreaHeight - cy;
    ellipse.setRadiusX(Math.max(1, rx));
    ellipse.setRadiusY(Math.max(1, ry));
    ellipse.setCenterX(cx);
    ellipse.setCenterY(cy);
  }

  /**
   * Перемещает фигуру на заданное смещение, не выходя за пределы области
   * рисования.
   *
   * @param shape фигура.
   * @param dx смещение по X.
   * @param dy смещение по Y.
   */
  private void moveShape(Shape shape, double dx, double dy) {
    if (shape instanceof Rectangle rect) {
      double nx = rect.getX() + dx;
      double ny = rect.getY() + dy;
      if (nx >= 0 && ny >= 0 && nx + rect.getWidth() <= drawAreaWidth
          && ny + rect.getHeight() <= drawAreaHeight) {
        rect.setX(nx);
        rect.setY(ny);
      }
    } else if (shape instanceof Circle circle) {
      double nx = circle.getCenterX() + dx;
      double ny = circle.getCenterY() + dy;
      double r = circle.getRadius();
      if (nx - r >= 0 && ny - r >= 0 && nx + r <= drawAreaWidth && ny + r <= drawAreaHeight) {
        circle.setCenterX(nx);
        circle.setCenterY(ny);
      }
    } else if (shape instanceof Ellipse ellipse) {
      double nx = ellipse.getCenterX() + dx;
      double ny = ellipse.getCenterY() + dy;
      double rx = ellipse.getRadiusX();
      double ry = ellipse.getRadiusY();
      if (nx - rx >= 0 && ny - ry >= 0 && nx + rx <= drawAreaWidth && ny + ry <= drawAreaHeight) {
        ellipse.setCenterX(nx);
        ellipse.setCenterY(ny);
      }
    } else if (shape instanceof Line line) {
      double sx = line.getStartX() + dx;
      double sy = line.getStartY() + dy;
      double ex = line.getEndX() + dx;
      double ey = line.getEndY() + dy;

      if (sx >= 0 && sy >= 0 && ex >= 0 && ey >= 0 &&
          sx <= drawAreaWidth && sy <= drawAreaHeight &&
          ex <= drawAreaWidth && ey <= drawAreaHeight) {
        line.setStartX(sx);
        line.setStartY(sy);
        line.setEndX(ex);
        line.setEndY(ey);
      }
    } else if (shape instanceof Polygon poly) {
      List<Double> points = poly.getPoints();
      boolean canMove = true;

      for (int i = 0; i < points.size(); i += 2) {
        double newX = points.get(i) + dx;
        double newY = points.get(i + 1) + dy;
        if (newX < 0 || newX > drawAreaWidth || newY < 0 || newY > drawAreaHeight) {
          canMove = false;
          break;
        }
      }
      if (canMove) {
        for (int i = 0; i < points.size(); i += 2) {
          points.set(i, points.get(i) + dx);
          points.set(i + 1, points.get(i + 1) + dy);
        }
      }
    }
  }

  /**
   * Заливает выделенную фигуру выбранным цветом. Для линий заливка невозможна.
   * Сопровождается анимированными пузырьками.
   */
  private void fillSelectedShape() {
    if (selectedShape != null) {
      if (!(selectedShape instanceof Line)) {
        selectedShape.setFill(colorPicker.getValue());
        selectedShape.setStroke(Color.BLUE);
        selectedShape.setStrokeWidth(3);

        for (int i = 0; i < 30; i++) {
          double x = 0, y = 0;
          if (selectedShape instanceof Rectangle rect) {
            x = rect.getX() + rect.getWidth() * (0.2 + random.nextDouble() * 0.6);
            y = rect.getY() + rect.getHeight() * (0.2 + random.nextDouble() * 0.6);
          } else if (selectedShape instanceof Circle circle) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double r = circle.getRadius() * (0.2 + random.nextDouble() * 0.6);
            x = circle.getCenterX() + r * Math.cos(angle);
            y = circle.getCenterY() + r * Math.sin(angle);
          } else if (selectedShape instanceof Ellipse ellipse) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double rx = ellipse.getRadiusX() * (0.2 + random.nextDouble() * 0.6);
            double ry = ellipse.getRadiusY() * (0.2 + random.nextDouble() * 0.6);
            x = ellipse.getCenterX() + rx * Math.cos(angle);
            y = ellipse.getCenterY() + ry * Math.sin(angle);
          } else if (selectedShape instanceof Polygon poly) {
            List<Double> pts = poly.getPoints();
            if (pts.size() >= 6) {
              double x1 = pts.get(0), y1 = pts.get(1);
              double x2 = pts.get(2), y2 = pts.get(3);
              double x3 = pts.get(4), y3 = pts.get(5);
              double alpha = random.nextDouble();
              double beta = random.nextDouble() * (1 - alpha);
              double gamma = 1 - alpha - beta;
              x = alpha * x1 + beta * x2 + gamma * x3;
              y = alpha * y1 + beta * y2 + gamma * y3;
            } else {
              continue;
            }
          }
          createBubbleAt(x, y, colorPicker.getValue());
        }
      } else {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Невозможно залить");
        alert.setHeaderText(null);
        alert.setContentText("Линию нельзя залить.");
        alert.showAndWait();
      }
    } else {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle("Нет выделения");
      alert.setHeaderText(null);
      alert.setContentText("Сначала выделите фигуру инструментом 'Выделить/Переместить'");
      alert.showAndWait();
    }
  }

  /**
   * Ищет фигуру под указанными координатами (последнюю нарисованную, самую
   * верхнюю).
   *
   * @param x координата X.
   * @param y координата Y.
   * @return найденная фигура или null.
   */
  private Shape findShapeAt(double x, double y) {
    for (int i = shapeLayer.getChildren().size() - 1; i >= 0; i--) {
      javafx.scene.Node node = shapeLayer.getChildren().get(i);
      if (node == tempShape || node instanceof Anchor)
        continue;

      if (node instanceof Rectangle rect) {
        if (x >= rect.getX() && x <= rect.getX() + rect.getWidth() &&
            y >= rect.getY() && y <= rect.getY() + rect.getHeight()) {
          return rect;
        }
      } else if (node instanceof Circle circle) {
        double dx = x - circle.getCenterX();
        double dy = y - circle.getCenterY();
        if (dx * dx + dy * dy <= circle.getRadius() * circle.getRadius()) {
          return circle;
        }
      } else if (node instanceof Ellipse ellipse) {
        ;
        double dx = (x - ellipse.getCenterX()) / ellipse.getRadiusX();
        double dy = (y - ellipse.getCenterY()) / ellipse.getRadiusY();
        if (dx * dx + dy * dy <= 1) {
          return ellipse;
        }
      } else if (node instanceof Line line) {
        double dist = lineDistance(line.getStartX(), line.getStartY(), line.getEndX(),
            line.getEndY(), x, y);
        if (dist <= 5) {
          return line;
        }
      } else if (node instanceof Polygon poly) {
        if (poly.contains(x, y)) {
          return poly;
        }
      }
    }
    return null;
  }

  /**
   * Вычисляет расстояние от точки до отрезка.
   *
   * @param x1 X начала отрезка.
   * @param y1 Y начала отрезка.
   * @param x2 X конца отрезка.
   * @param y2 Y конца отрезка.
   * @param px X точки.
   * @param py Y точки.
   * @return расстояние.
   */
  private double lineDistance(double x1, double y1, double x2, double y2, double px, double py) {
    double A = px - x1;
    double B = py - y1;
    double C = x2 - x1;
    double D = y2 - y1;
    double dot = A * C + B * D;
    double len2 = C * C + D * D;
    double param = -1;
    if (len2 != 0)
      param = dot / len2;
    double xx, yy;
    if (param < 0) {
      xx = x1;
      yy = y1;
    } else if (param > 1) {
      xx = x2;
      yy = y2;
    } else {
      xx = x1 + param * C;
      yy = y1 + param * D;
    }
    double dx = px - xx;
    double dy = py - yy;
    return Math.sqrt(dx * dx + dy * dy);
  }

  /**
   * Показывает управляющие якоря по углам описывающего прямоугольника для круга и
   * эллипса.
   *
   * @param shape фигура.
   */
  private void showAnchors(Shape shape) {
    hideAnchors();
    if (shape instanceof Circle circle) {
      double cx = circle.getCenterX();
      double cy = circle.getCenterY();
      double r = circle.getRadius();
      Anchor[] anchorsArray = {
          new Anchor(cx - r, cy - r, circle),
          new Anchor(cx + r, cy - r, circle),
          new Anchor(cx - r, cy + r, circle),
          new Anchor(cx + r, cy + r, circle)
      };
      for (Anchor a : anchorsArray) {
        anchors.add(a);
        shapeLayer.getChildren().add(a);
      }
    } else if (shape instanceof Ellipse ellipse) {
      double cx = ellipse.getCenterX();
      double cy = ellipse.getCenterY();
      double rx = ellipse.getRadiusX();
      double ry = ellipse.getRadiusY();
      Anchor[] anchorsArray = {
          new Anchor(cx - rx, cy - ry, ellipse),
          new Anchor(cx + rx, cy - ry, ellipse),
          new Anchor(cx - rx, cy + ry, ellipse),
          new Anchor(cx + rx, cy + ry, ellipse)
      };
      for (Anchor a : anchorsArray) {
        anchors.add(a);
        shapeLayer.getChildren().add(a);
      }
    }
  }

  /**
   * Скрывает все якоря и очищает их список.
   */
  private void hideAnchors() {
    shapeLayer.getChildren().removeAll(anchors);
    anchors.clear();
  }

  /**
   * Обновляет позиции якорей в соответствии с текущими размерами фигуры.
   *
   * @param shape фигура, чьи якоря обновляются.
   */
  private void updateAnchors(Shape shape) {
    if (shape instanceof Circle circle) {
      if (anchors.size() == 4) {
        double cx = circle.getCenterX();
        double cy = circle.getCenterY();
        double r = circle.getRadius();
        anchors.get(0).setCenterX(cx - r);
        anchors.get(0).setCenterY(cy - r);
        anchors.get(1).setCenterX(cx + r);
        anchors.get(1).setCenterY(cy - r);
        anchors.get(2).setCenterX(cx - r);
        anchors.get(2).setCenterY(cy + r);
        anchors.get(3).setCenterX(cx + r);
        anchors.get(3).setCenterY(cy + r);
      }
    } else if (shape instanceof Ellipse ellipse) {
      if (anchors.size() == 4) {
        double cx = ellipse.getCenterX();
        double cy = ellipse.getCenterY();
        double rx = ellipse.getRadiusX();
        double ry = ellipse.getRadiusY();
        anchors.get(0).setCenterX(cx - rx);
        anchors.get(0).setCenterY(cy - ry);
        anchors.get(1).setCenterX(cx + rx);
        anchors.get(1).setCenterY(cy - ry);
        anchors.get(2).setCenterX(cx - rx);
        anchors.get(2).setCenterY(cy + ry);
        anchors.get(3).setCenterX(cx + rx);
        anchors.get(3).setCenterY(cy + ry);
      }
    }
  }

  /**
   * Создаёт один декоративный пузырёк в указанной точке с заданным цветом.
   * Пузырёк анимирован: поднимается, меняет размер и исчезает.
   * Частота создания ограничена {@link #BUBBLE_DELAY_MS}.
   *
   * @param x координата X.
   * @param y координата Y.
   * @param color базовый цвет пузырька.
   */
  private void createBubbleAt(double x, double y, Color color) {
    long now = System.currentTimeMillis();
    if (now - lastBubbleTime < BUBBLE_DELAY_MS)
      return;
    lastBubbleTime = now;

    if (x < 0 || x > drawAreaWidth || y < 0 || y > drawAreaHeight)
      return;

    double radius = 3 + random.nextInt(8);
    Circle bubble = new Circle(radius);

    int red = (int) (color.getRed() * 255);
    int green = (int) (color.getGreen() * 255);
    int blue = (int) (color.getBlue() * 255);

    RadialGradient gradient = new RadialGradient(
        0, 0, radius * 0.3, radius * 0.3, radius,
        false, CycleMethod.NO_CYCLE,
        new Stop(0, Color.rgb(255, 255, 255, 0.95)),
        new Stop(0.5, Color.rgb(red, green, blue, 0.8)),
        new Stop(1, Color.rgb(red, green, blue, 0.3)));
    bubble.setFill(gradient);
    bubble.setStroke(Color.rgb(255, 255, 255, 0.6));
    bubble.setStrokeWidth(1);

    bubble.setTranslateX(x - radius);
    bubble.setTranslateY(y - radius);

    bubbleLayer.getChildren().add(bubble);

    double speedX = (random.nextDouble() - 0.5) * 25;
    double speedY = -12 - random.nextDouble() * 20;
    int duration = 600 + random.nextInt(400);

    TranslateTransition move = new TranslateTransition(Duration.millis(duration), bubble);
    move.setByX(speedX);
    move.setByY(speedY);

    FadeTransition fade = new FadeTransition(Duration.millis(duration), bubble);
    fade.setFromValue(1.0);
    fade.setToValue(0.0);

    ScaleTransition scale = new ScaleTransition(Duration.millis(duration), bubble);
    scale.setFromX(1.0);
    scale.setFromY(1.0);
    scale.setToX(1.2 + random.nextDouble() * 0.5);
    scale.setToY(1.2 + random.nextDouble() * 0.5);

    ParallelTransition parallel = new ParallelTransition(move, fade, scale);
    parallel.setOnFinished(ev -> bubbleLayer.getChildren().remove(bubble));
    parallel.play();
  }

  /**
   * Полностью очищает холст (заливает белым цветом).
   */
  private void clearCanvas() {
    gc.setFill(Color.WHITE);
    gc.fillRect(0, 0, canvasLayer.getWidth(), canvasLayer.getHeight());
  }

  /**
   * Управляющий якорь (маленький кружок), позволяющий изменять размеры связанной
   * фигуры.
   */
  private class Anchor extends Circle {

    /** Фигура, размерами которой управляет данный якорь. */
    private final Shape target;

    /**
     * Создаёт якорь в заданных координатах.
     *
     * @param x начальная X.
     * @param y начальная Y.
     * @param target фигура, для которой предназначен якорь.
     */
    public Anchor(double x, double y, Shape target) {
      super(x, y, 8);
      this.target = target;
      setFill(Color.GOLD);
      setStroke(Color.BLACK);
      setStrokeWidth(2);
      setupMouseHandlers();
    }

    /**
     * Назначает обработчики мыши для изменения размеров фигуры.
     */
    private void setupMouseHandlers() {
      setOnMousePressed(e -> {
        setFill(Color.RED);
        e.consume();
      });

      setOnMouseDragged(e -> {
        if (target instanceof Circle circle) {
          double newX = e.getX();
          double newY = e.getY();
          double cx = circle.getCenterX();
          double cy = circle.getCenterY();
          double dx = newX - cx;
          double dy = newY - cy;
          double newRadius = Math.sqrt(dx * dx + dy * dy);
          double maxRadiusX = Math.min(cx, drawAreaWidth - cx);
          double maxRadiusY = Math.min(cy, drawAreaHeight - cy);
          newRadius = Math.min(newRadius, Math.min(maxRadiusX, maxRadiusY));
          circle.setRadius(Math.max(10, newRadius));
          updateAnchors(circle);
          createBubbleAt(e.getX(), e.getY(), Color.rgb(255, 200, 100, 0.7));
        } else if (target instanceof Ellipse ellipse) {
          double newX = e.getX();
          double newY = e.getY();
          double cx = ellipse.getCenterX();
          double cy = ellipse.getCenterY();
          double dx = newX - cx;
          double dy = newY - cy;
          double rx = Math.abs(dx);
          double ry = Math.abs(dy);

          if (cx - rx < 0)
            rx = cx;
          if (cx + rx > drawAreaWidth)
            rx = drawAreaWidth - cx;
          if (cy - ry < 0)
            ry = cy;
          if (cy + ry > drawAreaHeight)
            ry = drawAreaHeight - cy;
          ellipse.setRadiusX(Math.max(10, rx));
          ellipse.setRadiusY(Math.max(10, ry));
          updateAnchors(ellipse);
          createBubbleAt(e.getX(), e.getY(), Color.rgb(255, 200, 100, 0.7));
        }
        e.consume();
      });

      setOnMouseReleased(e -> {
        setFill(Color.GOLD);
        e.consume();
      });
    }
  }

  /**
   * Главный метод для запуска приложения.
   *
   * @param args аргументы командной строки (не используются).
   */
  public static void main(String[] args) {
    launch(args);
  }
}
