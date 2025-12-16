package com.chatty.controllers;

import com.chatty.models.User;
import com.chatty.services.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.application.Platform;
import org.kordamp.ikonli.javafx.FontIcon;

public class LoginController {
    private AuthService authService;

    public LoginController() {
        this.authService = new AuthService();
    }

    public void show(Stage primaryStage) {
        primaryStage.setTitle("Chatty - Login");
        primaryStage.setWidth(1000);
        primaryStage.setHeight(600);
        primaryStage.setResizable(false);

        // Main container
        HBox mainContainer = new HBox();
        mainContainer.getStyleClass().add("login-container");

        // Left side - Form
        VBox leftPane = new VBox(20);
        leftPane.setPadding(new Insets(40));
        leftPane.setAlignment(Pos.CENTER);
        leftPane.setPrefWidth(450);
        leftPane.getStyleClass().add("login-form-pane");

        // Logo
        VBox logoContainer = new VBox(10);
        logoContainer.setAlignment(Pos.CENTER);
        Region iconBg = new Region();
        iconBg.getStyleClass().add("logo-icon-bg");
        FontIcon messageIcon = new FontIcon("mdi2m-message-text");
        messageIcon.setIconSize(24);
        messageIcon.getStyleClass().add("logo-icon");
        
        Label title = new Label("Welcome Back");
        title.getStyleClass().add("login-title");
        Label subtitle = new Label("Sign in to your account");
        subtitle.getStyleClass().add("login-subtitle");
        
        logoContainer.getChildren().addAll(iconBg, title, subtitle);

        // Form fields
        VBox formContainer = new VBox(20);
        formContainer.setPrefWidth(350);

        // Username field
        Label usernameLabel = new Label("Username");
        usernameLabel.getStyleClass().add("form-label");
        HBox usernameContainer = new HBox(10);
        usernameContainer.setAlignment(Pos.CENTER_LEFT);
        usernameContainer.getStyleClass().add("input-container");
        FontIcon userIcon = new FontIcon("mdi2a-account");
        userIcon.setIconSize(20);
        userIcon.getStyleClass().add("input-icon");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.getStyleClass().add("text-input");
        usernameField.setPrefWidth(310);
        usernameContainer.getChildren().addAll(userIcon, usernameField);

        // Password field
        Label passwordLabel = new Label("Password");
        passwordLabel.getStyleClass().add("form-label");
        HBox passwordContainer = new HBox(10);
        passwordContainer.setAlignment(Pos.CENTER_LEFT);
        passwordContainer.getStyleClass().add("input-container");
        FontIcon lockIcon = new FontIcon("mdi2l-lock");
        lockIcon.setIconSize(20);
        lockIcon.getStyleClass().add("input-icon");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("••••••••");
        passwordField.getStyleClass().add("text-input");
        passwordField.setPrefWidth(270);
        
        ToggleButton showPasswordBtn = new ToggleButton();
        FontIcon eyeIcon = new FontIcon("mdi2e-eye");
        eyeIcon.setIconSize(20);
        showPasswordBtn.setGraphic(eyeIcon);
        showPasswordBtn.getStyleClass().add("icon-button");
        TextField visiblePasswordField = new TextField();
        visiblePasswordField.setPromptText("••••••••");
        visiblePasswordField.getStyleClass().add("text-input");
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setPrefWidth(270);
        
        showPasswordBtn.setOnAction(e -> {
            if (showPasswordBtn.isSelected()) {
                visiblePasswordField.setText(passwordField.getText());
                passwordField.setVisible(false);
                visiblePasswordField.setVisible(true);
                eyeIcon.setIconLiteral("mdi2e-eye-off");
            } else {
                passwordField.setText(visiblePasswordField.getText());
                visiblePasswordField.setVisible(false);
                passwordField.setVisible(true);
                eyeIcon.setIconLiteral("mdi2e-eye");
            }
        });
        
        passwordContainer.getChildren().addAll(lockIcon, passwordField, visiblePasswordField, showPasswordBtn);

        // Login button
        Button loginButton = new Button("Sign in");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setPrefWidth(350);
        
        // Sign up link
        HBox signupLinkContainer = new HBox();
        signupLinkContainer.setAlignment(Pos.CENTER);
        Label signupLabel = new Label("Don't have an account? ");
        Hyperlink signupLink = new Hyperlink("Create account");
        signupLink.getStyleClass().add("link");
        signupLinkContainer.getChildren().addAll(signupLabel, signupLink);

        formContainer.getChildren().addAll(usernameLabel, usernameContainer, passwordLabel, passwordContainer, loginButton, signupLinkContainer);

        leftPane.getChildren().addAll(logoContainer, formContainer);

        // Right side - Pattern/Image
        VBox rightPane = new VBox();
        rightPane.setPrefWidth(550);
        rightPane.getStyleClass().add("login-pattern-pane");
        Label patternTitle = new Label("Welcome back!");
        patternTitle.getStyleClass().add("pattern-title");
        Label patternSubtitle = new Label("Sign in to continue your conversations and catch up with your messages.");
        patternSubtitle.getStyleClass().add("pattern-subtitle");
        rightPane.setAlignment(Pos.CENTER);
        rightPane.setSpacing(20);
        rightPane.getChildren().addAll(patternTitle, patternSubtitle);

        mainContainer.getChildren().addAll(leftPane, rightPane);

        // Login handler
        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.isVisible() ? passwordField.getText() : visiblePasswordField.getText();
            
            if (username.isEmpty() || password.isEmpty()) {
                showAlert("Error", "Please fill in all fields", Alert.AlertType.ERROR);
                return;
            }

            loginButton.setDisable(true);
            loginButton.setText("Loading...");
            
            new Thread(() -> {
                try {
                    User loginUser = authService.login(username, password);
                    Platform.runLater(() -> {
                        new HomeController().show(primaryStage, loginUser);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        showAlert("Login Failed", ex.getMessage(), Alert.AlertType.ERROR);
                        loginButton.setDisable(false);
                        loginButton.setText("Sign in");
                    });
                }
            }).start();
        });

        // Sign up link handler
        signupLink.setOnAction(e -> {
            new SignUpController().show(primaryStage);
        });

        Scene scene = new Scene(mainContainer);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

