package com.example.javafxapp;

import com.example.App;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import javafx.stage.Stage;

import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

@ExtendWith(ApplicationExtension.class)
public class AuthControllerTest {

    @Start
    private void start(Stage stage) throws Exception {
        FxToolkit.registerPrimaryStage();
        new App().start(stage);
    }

    @BeforeEach
    public void setUp() throws Exception {
        FxToolkit.setupApplication(App.class);
    }

    @AfterEach
    public void tearDown() throws Exception {
        FxToolkit.cleanupStages();
    }

    @Test
    public void should_show_error_when_fields_empty(FxRobot robot) {
        robot.clickOn("#loginButton");
        verifyThat("#errorLabel", hasText("Please enter username and password"));
    }

    @Test
    public void should_show_error_when_invalid_credentials(FxRobot robot) {
        robot.clickOn("#usernameField").write("invalid");
        robot.clickOn("#passwordField").write("invalid");
        robot.clickOn("#loginButton");
        verifyThat("#errorLabel", hasText("Invalid credentials"));
    }

    @Test
    public void should_navigate_to_editor_after_successful_login(FxRobot robot) {
        robot.clickOn("#usernameField").write("testuser");
        robot.clickOn("#passwordField").write("testpass");
        robot.clickOn("#loginButton");
        verifyThat("#titleLabel", hasText("Text Editor - testuser"));
    }

    @Test
    public void should_register_new_user(FxRobot robot) {
        String randomUsername = "user" + System.currentTimeMillis();
        robot.clickOn("#usernameField").write(randomUsername);
        robot.clickOn("#passwordField").write("password");
        robot.clickOn("#registerButton");
        verifyThat("#errorLabel", hasText("Registration successful! Please login"));
    }
}
