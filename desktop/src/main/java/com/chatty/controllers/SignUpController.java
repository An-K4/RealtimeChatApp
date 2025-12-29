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
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.application.Platform;

public class SignUpController {
    private AuthService authService;

    public SignUpController() {
        this.authService = new AuthService();
    }

    public void show(Stage primaryStage) {
        primaryStage.setTitle("Kma Chatty - Sign Up");
        primaryStage.setWidth(1000);
        primaryStage.setHeight(750);
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
        Label title = new Label("Create Account");
        title.getStyleClass().add("login-title");
        Label subtitle = new Label("Get started with your free account");
        subtitle.getStyleClass().add("login-subtitle");

        logoContainer.getChildren().addAll(title, subtitle);

        // Form fields
        VBox formContainer = new VBox(20);
        formContainer.setPrefWidth(350);

        // Username field
        Label usernameLabel = new Label("Username");
        usernameLabel.getStyleClass().add("form-label");
        HBox usernameContainer = new HBox(10);
        usernameContainer.setAlignment(Pos.CENTER_LEFT);
        usernameContainer.getStyleClass().add("input-container");
        FontIcon usernameIcon = new FontIcon("mdi2a-account-circle");
        usernameIcon.setIconSize(20);
        usernameIcon.getStyleClass().add("input-icon");
        TextField usernameField = new TextField();
        usernameField.setPromptText("your_username");
        usernameField.getStyleClass().add("text-input");
        usernameField.setPrefWidth(310);
        usernameContainer.getChildren().addAll(usernameIcon, usernameField);

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

        // Full Name field
        Label fullNameLabel = new Label("Full Name");
        fullNameLabel.getStyleClass().add("form-label");
        HBox fullNameContainer = new HBox(10);
        fullNameContainer.setAlignment(Pos.CENTER_LEFT);
        fullNameContainer.getStyleClass().add("input-container");
        FontIcon userIcon = new FontIcon("mdi2a-account");
        userIcon.setIconSize(20);
        userIcon.getStyleClass().add("input-icon");
        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Your Full Name");
        fullNameField.getStyleClass().add("text-input");
        fullNameField.setPrefWidth(310);
        fullNameContainer.getChildren().addAll(userIcon, fullNameField);

        // Email field
        Label emailLabel = new Label("Email");
        emailLabel.getStyleClass().add("form-label");
        HBox emailContainer = new HBox(10);
        emailContainer.setAlignment(Pos.CENTER_LEFT);
        emailContainer.getStyleClass().add("input-container");
        FontIcon mailIcon = new FontIcon("mdi2e-email");
        mailIcon.setIconSize(20);
        mailIcon.getStyleClass().add("input-icon");
        TextField emailField = new TextField();
        emailField.setPromptText("you@example.com");
        emailField.getStyleClass().add("text-input");
        emailField.setPrefWidth(310);
        emailContainer.getChildren().addAll(mailIcon, emailField);

        // Sign up button
        Button signupButton = new Button("Create Account");
        signupButton.getStyleClass().add("primary-button");
        signupButton.setPrefWidth(350);

        // Login link
        HBox loginLinkContainer = new HBox();
        loginLinkContainer.setAlignment(Pos.CENTER);
        Label loginLabel = new Label("Already have an account? ");
        Hyperlink loginLink = new Hyperlink("Sign in");
        loginLink.getStyleClass().add("link");
        loginLinkContainer.getChildren().addAll(loginLabel, loginLink);

        formContainer.getChildren().addAll(usernameLabel, usernameContainer, passwordLabel, passwordContainer, fullNameLabel, fullNameContainer, emailLabel, emailContainer, signupButton, loginLinkContainer);

        leftPane.getChildren().addAll(logoContainer, formContainer);

        // Right side - Pattern/Image
        VBox rightPane = new VBox();
        rightPane.setPrefWidth(550);
        rightPane.getStyleClass().add("login-pattern-pane");
        ImageView logo = new ImageView();
        logo.setFitWidth(250);
        logo.setFitHeight(250);
        logo.setImage(new Image(getClass().getResource("/logo.png").toExternalForm()));

        // --- BO TRÒN ---
        // Tạo một hình tròn
        Circle clip = new Circle(125, 125, 125); // Tọa độ tâm X, Y, Bán kính (Bán kính = 1 nửa kích thước ảnh)
        logo.setClip(clip);

        Label patternTitle = new Label("Join our community");
        patternTitle.getStyleClass().add("pattern-title");
        Label patternSubtitle = new Label("Connect with friends, share moments, and stay in touch with your loved ones.");
        patternSubtitle.getStyleClass().add("pattern-subtitle");
        rightPane.setAlignment(Pos.CENTER);
        rightPane.setSpacing(20);
        rightPane.getChildren().addAll(logo, patternTitle, patternSubtitle);

        mainContainer.getChildren().addAll(leftPane, rightPane);

        // Sign up handler
        signupButton.setOnAction(e -> {
            String username = usernameField.getText();
            String fullName = fullNameField.getText();
            String email = emailField.getText();
            String password = passwordField.isVisible() ? passwordField.getText() : visiblePasswordField.getText();

            if (username.isEmpty() || fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                showAlert("Error", "Please fill in all fields", Alert.AlertType.ERROR);
                return;
            }

            if (password.length() < 6) {
                showAlert("Error", "Password must be at least 6 characters", Alert.AlertType.ERROR);
                return;
            }

            signupButton.setDisable(true);
            signupButton.setText("Loading...");

            new Thread(() -> {
                try {
                    authService.signup(username, fullName, email, password);

                    Platform.runLater(() -> {
                        showAlert("Đăng ký", "Đăng ký thành công!", Alert.AlertType.INFORMATION);
                        new LoginController().show(primaryStage);
                    });
                } catch (Exception ex) {
                    // Xử lý lỗi
                    String errorMessage = ex.getMessage();

                    // Mẹo nhỏ: Nếu lỗi có chứa JSON, thử làm sạch nó (tùy chọn)
                    if (errorMessage.contains("Request failed:")) {
                        // Cắt bớt chữ "Request failed:" cho đỡ dài
                        errorMessage = errorMessage.replace("Request failed:", "").trim();
                    }

                    final String finalMsg = errorMessage;
                    Platform.runLater(() -> {
                        showAlert("Đăng ký thất bại", finalMsg, Alert.AlertType.ERROR);
                        signupButton.setDisable(false);
                        signupButton.setText("Đăng ký");
                    });
                }
            }).start();
        });

        // Login link handler
        loginLink.setOnAction(e -> {
            new LoginController().show(primaryStage);
        });

        Scene scene = new Scene(mainContainer);
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

