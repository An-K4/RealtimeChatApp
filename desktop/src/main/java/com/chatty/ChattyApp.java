package com.chatty;

import javafx.application.Application;
import javafx.stage.Stage;
import com.chatty.controllers.LoginController;
import com.chatty.services.AuthService;

public class ChattyApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        AuthService authService = new AuthService();
        
        // Check if user is already authenticated
        if (authService.checkAuth()) {
            // Show home page
            new com.chatty.controllers.HomeController().show(primaryStage);
        } else {
            // Show login page
            new LoginController().show(primaryStage);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

