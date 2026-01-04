package com.chatty.controllers;

import com.chatty.models.User;
import com.chatty.services.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
        primaryStage.setTitle("Kma Chatty - Login");
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();

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
        ImageView logo = new ImageView();
        logo.setFitWidth(150);
        logo.setFitHeight(150);
        logo.setImage(new Image(getClass().getResource("/logo.png").toExternalForm()));
        logoContainer.getChildren().add(logo);

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
        StackPane passwordField = new StackPane();

        PasswordField invisiblePasswordField = new PasswordField();
        invisiblePasswordField.setPromptText("••••••••");
        invisiblePasswordField.getStyleClass().add("text-input");
        invisiblePasswordField.setPrefWidth(270);

        TextField visiblePasswordField = new TextField();
        visiblePasswordField.setPromptText("••••••••");
        visiblePasswordField.getStyleClass().add("text-input");
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setPrefWidth(270);
        
        ToggleButton showPasswordBtn = new ToggleButton();
        FontIcon eyeIcon = new FontIcon("mdi2e-eye");
        eyeIcon.setIconSize(20);
        showPasswordBtn.setGraphic(eyeIcon);
        showPasswordBtn.getStyleClass().add("icon-button");
        
        showPasswordBtn.setOnAction(e -> {
            if (showPasswordBtn.isSelected()) {
                visiblePasswordField.setText(invisiblePasswordField.getText());
                invisiblePasswordField.setVisible(false);
                visiblePasswordField.setVisible(true);
                eyeIcon.setIconLiteral("mdi2e-eye-off");
            } else {
                invisiblePasswordField.setText(visiblePasswordField.getText());
                visiblePasswordField.setVisible(false);
                invisiblePasswordField.setVisible(true);
                eyeIcon.setIconLiteral("mdi2e-eye");
            }
        });

        passwordField.getChildren().addAll(invisiblePasswordField, visiblePasswordField);
        passwordContainer.getChildren().addAll(lockIcon, passwordField, showPasswordBtn);

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
            String password = invisiblePasswordField.isVisible() ? invisiblePasswordField.getText() : visiblePasswordField.getText();
            
            if (username.isEmpty() || password.isEmpty()) {
                showAlert("Error", "Please fill in all fields", Alert.AlertType.ERROR);
                return;
            }

            usernameField.setDisable(true);
            passwordField.setDisable(true);
            signupLink.setDisable(true);
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
                        usernameField.setDisable(false);
                        passwordField.setDisable(false);
                        signupLink.setDisable(false);
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

        Scene scene = new Scene(mainContainer, 1000, 600);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();

        Platform.runLater(mainContainer::requestFocus);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

