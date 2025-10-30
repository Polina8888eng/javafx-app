module com.example.javafxapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires bcrypt;

    opens com.example.javafxapp to javafx.fxml, org.testfx;
    opens com.example to javafx.fxml;

    exports com.example.javafxapp;
    exports com.example;
}