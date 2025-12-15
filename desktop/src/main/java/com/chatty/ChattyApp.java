package com.chatty;

import com.chatty.models.User;
import javafx.application.Application;
import javafx.stage.Stage;
import com.chatty.controllers.LoginController;
import com.chatty.services.AuthService;

public class ChattyApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        AuthService authService = new AuthService();

        User user = authService.checkAuth();

        // Check if user is already authenticated
        if (user != null) {
            // Show home page
            new com.chatty.controllers.HomeController().show(primaryStage, user);
        } else {
            // Show login page
            new LoginController().show(primaryStage);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

