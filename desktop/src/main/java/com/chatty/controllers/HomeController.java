package com.chatty.controllers;

import com.chatty.models.Message;
import com.chatty.models.User;
import com.chatty.services.AuthService;
import com.chatty.services.ChatService;
import com.chatty.services.SocketService;
import com.chatty.services.ThemeService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HomeController {
    private final AuthService authService;
    private final ChatService chatService;
    private final SocketService socketService;
    private User selectedUser;
    private List<User> users;
    private List<Message> messages;
    private VBox messageContainer;
    private ScrollPane messageScrollPane;
    private TextField messageInput;
    private ListView<User> userListView;
    private Stage primaryStage;
    private BorderPane mainContainer;
    private HBox centerContent;
    private Scene scene;

    public HomeController() {
        this.authService = new AuthService();
        this.socketService = new SocketService();
        this.chatService = new ChatService(socketService);
        this.messages = new ArrayList<>();
    }

    public void show(Stage primaryStage, User user) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Kma Chatty");
        primaryStage.setWidth(1200);
        primaryStage.setHeight(700);
        primaryStage.setResizable(true);
        primaryStage.centerOnScreen();

        // Main container
        mainContainer = new BorderPane();
        mainContainer.getStyleClass().add("home-container");

        // Navbar
        HBox navbar = createNavbar(primaryStage);
        mainContainer.setTop(navbar);

        // Center content
        centerContent = new HBox();
        centerContent.getStyleClass().add("chat-container");

        // Sidebar
        VBox sidebar = createSidebar();
        centerContent.getChildren().add(sidebar);

        // Chat area
        VBox chatArea = createChatArea();
        centerContent.getChildren().add(chatArea);
        HBox.setHgrow(chatArea, Priority.ALWAYS);

        mainContainer.setCenter(centerContent);

        // Load users
        loadUsers();

        // Connect socket
        authService.setCurrentUser(user);
        User currentUser = authService.getCurrentUser();
        if (currentUser != null) {
            socketService.connect(currentUser.get_id());
            socketService.setOnNewMessage(message -> {
                if (selectedUser != null && message.getSenderId().equals(selectedUser.get_id())) {
                    messages.add(message);
                    Platform.runLater(() -> {
                        renderMessages();
                        messageScrollPane.setVvalue(1.0);
                    });
                }
            });
        }

        scene = new Scene(mainContainer);
        // Load theme preference
        String themeStylesheet = ThemeService.getThemeStylesheet();
        scene.getStylesheets().add(getClass().getResource(themeStylesheet).toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();

        Platform.runLater(mainContainer::requestFocus);
    }

    private HBox createNavbar(Stage stage) {
        HBox navbar = new HBox(20);
        navbar.setPadding(new Insets(15, 30, 15, 30));
        navbar.setAlignment(Pos.CENTER_LEFT);
        navbar.getStyleClass().add("navbar");

        // Logo
        HBox logoContainer = new HBox(10);
        logoContainer.setAlignment(Pos.CENTER_LEFT);
        Label appName = new Label("Kma Chatty");
        appName.getStyleClass().add("navbar-title");
        logoContainer.getChildren().add(appName);

        // Right side buttons
        HBox rightButtons = new HBox(10);
        rightButtons.setAlignment(Pos.CENTER_RIGHT);

        Button settingsBtn = new Button("Settings");
        settingsBtn.getStyleClass().add("nav-button");
        FontIcon settingsIcon = new FontIcon("mdi2c-cog");
        settingsIcon.setIconSize(18);
        settingsBtn.setGraphic(settingsIcon);
        settingsBtn.setOnAction(e -> showSettings());

        Button profileBtn = new Button("Profile");
        profileBtn.getStyleClass().add("nav-button");
        FontIcon profileIcon = new FontIcon("mdi2a-account");
        profileIcon.setIconSize(18);
        profileBtn.setGraphic(profileIcon);
        profileBtn.setOnAction(e -> showProfile());

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("nav-button");
        FontIcon logoutIcon = new FontIcon("mdi2l-logout");
        logoutIcon.setIconSize(18);
        logoutBtn.setGraphic(logoutIcon);

        logoutBtn.setOnAction(e -> {
            authService.logout();
            socketService.disconnect();
            new LoginController().show(stage);
        });

        rightButtons.getChildren().addAll(settingsBtn, profileBtn, logoutBtn);

        HBox.setHgrow(rightButtons, Priority.ALWAYS);
        navbar.getChildren().addAll(logoContainer, rightButtons);

        return navbar;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(280);
        sidebar.getStyleClass().add("sidebar");

        // Header
        VBox sidebarHeader = new VBox(15);
        sidebarHeader.setPadding(new Insets(20));
        sidebarHeader.getStyleClass().add("sidebar-header");

        HBox headerTitle = new HBox(10);
        headerTitle.setAlignment(Pos.CENTER_LEFT);
        FontIcon usersIcon = new FontIcon("mdi2a-account-group");
        usersIcon.setIconSize(24);
        Label contactsLabel = new Label("Contacts");
        contactsLabel.getStyleClass().add("sidebar-title");
        headerTitle.getChildren().addAll(usersIcon, contactsLabel);

        // Filter checkbox
        CheckBox onlineOnlyCheck = new CheckBox("Show online only");
        onlineOnlyCheck.getStyleClass().add("filter-checkbox");

        Label onlineCount = new Label("(0 online)");
        onlineCount.getStyleClass().add("online-count");

        HBox filterContainer = new HBox(10);
        filterContainer.getChildren().addAll(onlineOnlyCheck, onlineCount);

        sidebarHeader.getChildren().addAll(headerTitle, filterContainer);

        // User list
        userListView = new ListView<>();
        userListView.setCellFactory(list -> new UserListCell());
        userListView.getStyleClass().add("user-list");

        userListView.setOnMouseClicked(e -> {
            User selected = userListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selectUser(selected);
            }
        });

        VBox.setVgrow(userListView, Priority.ALWAYS);
        sidebar.getChildren().addAll(sidebarHeader, userListView);

        return sidebar;
    }

    private VBox createChatArea() {
        VBox chatArea = new VBox();
        chatArea.getStyleClass().add("chat-area");

        // Chat header (will be shown when user is selected)
        HBox chatHeader = new HBox(15);
        chatHeader.setPadding(new Insets(15, 20, 15, 20));
        chatHeader.getStyleClass().add("chat-header");
        chatHeader.setVisible(false);
        chatHeader.setManaged(false);
        chatArea.setId("chatHeader");

        // Messages container
        messageScrollPane = new ScrollPane();
        messageScrollPane.setFitToWidth(true);
        messageScrollPane.getStyleClass().add("message-scroll-pane");
        messageScrollPane.setVvalue(1.0);

        messageContainer = new VBox(15);
        messageContainer.setPadding(new Insets(20));
        messageScrollPane.setContent(messageContainer);

        // No chat selected view
        VBox noChatView = new VBox(20);
        noChatView.setAlignment(Pos.CENTER);
        noChatView.getStyleClass().add("no-chat-view");
        ImageView logo = new ImageView();
        logo.setFitWidth(200);
        logo.setFitHeight(200);
        logo.setImage(new Image(getClass().getResource("/logo.png").toExternalForm()));
        Label welcomeLabel = new Label("Welcome to Kma Chatty!");
        welcomeLabel.getStyleClass().add("no-chat-title");
        Label subtitleLabel = new Label("Select a conversation from the sidebar to start chatting");
        subtitleLabel.getStyleClass().add("no-chat-subtitle");
        noChatView.getChildren().addAll(logo, welcomeLabel, subtitleLabel);
        noChatView.setId("noChatView");

        // Message input
        HBox messageInputContainer = new HBox(10);
        messageInputContainer.setPadding(new Insets(15, 20, 15, 20));
        messageInputContainer.getStyleClass().add("message-input-container");

        messageInput = new TextField();
        messageInput.setPromptText("Type a message...");
        messageInput.getStyleClass().add("message-input");
        HBox.setHgrow(messageInput, Priority.ALWAYS);

        Button sendButton = new Button();
        FontIcon sendIcon = new FontIcon("mdi2s-send");
        sendIcon.setIconSize(20);
        sendButton.setGraphic(sendIcon);
        sendButton.getStyleClass().add("send-button");

        messageInput.setOnAction(e -> sendMessage());
        sendButton.setOnAction(e -> sendMessage());

        messageInputContainer.getChildren().addAll(messageInput, sendButton);
        messageInputContainer.setId("messageInputContainer");
        messageInputContainer.setVisible(false);
        messageInputContainer.setManaged(false);

        chatArea.getChildren().addAll(chatHeader, noChatView, messageScrollPane, messageInputContainer);
        VBox.setVgrow(messageScrollPane, Priority.ALWAYS);
        VBox.setVgrow(noChatView, Priority.ALWAYS);

        return chatArea;
    }

    private void loadUsers() {
        new Thread(() -> {
            try {
                users = chatService.getUsers();
                Platform.runLater(() -> {
                    userListView.getItems().clear();
                    userListView.getItems().addAll(users);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert("Error", "Failed to load users: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    private void selectUser(User user) {
        this.selectedUser = user;

        // Update UI
        Platform.runLater(() -> {
            // Show chat header
            HBox chatHeader = (HBox) ((VBox) messageScrollPane.getParent()).getChildren().get(0);
            chatHeader.setVisible(true);
            chatHeader.setManaged(true);

            // Update header content
            chatHeader.getChildren().clear();
            ImageView avatar = new ImageView();
            avatar.setFitWidth(40);
            avatar.setFitHeight(40);
            avatar.getStyleClass().add("chat-header-avatar");
            try {
                if (user.getProfilePic() != null && !user.getProfilePic().isEmpty()) {
                    avatar.setImage(new Image(user.getProfilePic()));
                }
            } catch (Exception e) {
                // Use default - no image
                avatar.setImage(null);
            }

            VBox userInfo = new VBox(5);
            Label userName = new Label(user.getFullName());
            userName.getStyleClass().add("chat-header-name");
            Label userStatus = new Label("Online");
            userStatus.getStyleClass().add("chat-header-status");
            userInfo.getChildren().addAll(userName, userStatus);

            // Video call button
            Button videoCallBtn = new Button();
            FontIcon videoIcon = new FontIcon("mdi2v-video");
            videoIcon.setIconSize(20);
            videoCallBtn.setGraphic(videoIcon);
            videoCallBtn.getStyleClass().add("video-call-button");
            videoCallBtn.setTooltip(new Tooltip("Gọi video"));
            videoCallBtn.setOnAction(e -> {
                if (selectedUser != null) {
                    startVideoCall(selectedUser.get_id());
                }
            });

            Button closeBtn = new Button();
            FontIcon closeIcon = new FontIcon("mdi2c-close");
            closeIcon.setIconSize(20);
            closeBtn.setGraphic(closeIcon);
            closeBtn.getStyleClass().add("close-button");
            closeBtn.setOnAction(e -> {
                selectedUser = null;
                chatHeader.setVisible(false);
                chatHeader.setManaged(false);
                VBox noChatView = (VBox) (messageScrollPane.getParent()).lookup("#noChatView");
                if (noChatView != null) {
                    noChatView.setVisible(true);
                    noChatView.setManaged(true);
                }
                HBox messageInputContainer = (HBox) (messageScrollPane.getParent()).lookup("#messageInputContainer");
                if (messageInputContainer != null) {
                    messageInputContainer.setVisible(false);
                    messageInputContainer.setManaged(false);
                }
                messageContainer.getChildren().clear();
            });

            HBox.setHgrow(userInfo, Priority.ALWAYS);
            chatHeader.getChildren().addAll(avatar, userInfo, videoCallBtn, closeBtn);

            // Hide no chat view
            VBox noChatView = (VBox) (messageScrollPane.getParent()).lookup("#noChatView");
            if (noChatView != null) {
                noChatView.setVisible(false);
                noChatView.setManaged(false);
            }

            // Show message input
            HBox messageInputContainer = (HBox) ((VBox) messageScrollPane.getParent()).lookup("#messageInputContainer");
            if (messageInputContainer != null) {
                messageInputContainer.setVisible(true);
                messageInputContainer.setManaged(true);
            }

            // Load messages
            loadMessages();
        });
    }

    private void loadMessages() {
        if (selectedUser == null) return;

        new Thread(() -> {
            try {
                messages = chatService.getMessages(selectedUser.get_id());

                Platform.runLater(() -> {
                    renderMessages();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert("Error", "Failed to load messages: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    private void renderMessages() {
        messageContainer.getChildren().clear();
        User currentUser = authService.getCurrentUser();

        for (Message message : messages) {
            boolean isMyMessage = message.getSenderId().equals(currentUser.get_id());

            HBox messageBox = new HBox(10);
            messageBox.getStyleClass().add(isMyMessage ? "message-box-right" : "message-box-left");

            if (!isMyMessage) {
                ImageView avatar = new ImageView();
                avatar.setFitWidth(40);
                avatar.setFitHeight(40);
                avatar.getStyleClass().add("message-avatar");

                if (currentUser.getProfilePic() != null && !currentUser.getProfilePic().isEmpty()) {
                    avatar.setImage(new Image(currentUser.getProfilePic()));
                } else {
                    // Use default - no image
                    avatar.setImage(new Image(getClass().getResource("/account.png").toExternalForm()));
                }

                messageBox.getChildren().add(avatar);
            }

            VBox messageContent = new VBox(5);

            if (message.getImage() != null && !message.getImage().isEmpty()) {
                ImageView imageView = new ImageView(new Image(message.getImage()));
                imageView.setFitWidth(200);
                imageView.setPreserveRatio(true);
                imageView.getStyleClass().add("message-image");
                messageContent.getChildren().add(imageView);
            }

            if (message.getContent() != null && !message.getContent().isEmpty()) {
                Label messageText = new Label(message.getContent());
                messageText.getStyleClass().add("message-text");
                messageText.setWrapText(true);
                messageContent.getChildren().add(messageText);
            }

            Label timeLabel = new Label(formatTime(message.getCreatedAt()));
            timeLabel.getStyleClass().add("message-time");
            messageContent.getChildren().add(timeLabel);

            messageBox.getChildren().add(messageContent);

            if (isMyMessage) {
                ImageView avatar = new ImageView();
                avatar.setFitWidth(40);
                avatar.setFitHeight(40);
                avatar.getStyleClass().add("message-avatar");

                if (currentUser.getProfilePic() != null && !currentUser.getProfilePic().isEmpty()) {
                    avatar.setImage(new Image(currentUser.getProfilePic()));
                } else {
                    // Use default - no image
                    avatar.setImage(new Image(getClass().getResource("/account.png").toExternalForm()));
                }

                messageBox.getChildren().add(avatar);
            }

            messageContainer.getChildren().add(messageBox);
        }

        // Scroll to bottom
        Platform.runLater(() -> {
            messageScrollPane.setVvalue(1.0);
        });
    }

    private void sendMessage() {
        if (selectedUser == null || messageInput.getText().trim().isEmpty()) {
            return;
        }

        String senderId = authService.getCurrentUser().get_id();
        String receiverId = selectedUser.get_id();
        String content = messageInput.getText().trim();
        messageInput.clear();

        new Thread(() -> {
            try {
                Message sentMessage = chatService.sendMessage(senderId, receiverId, content);
                messages.add(sentMessage);
                Platform.runLater(() -> {
                    renderMessages();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert("Error", "Failed to send message: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    private String formatTime(String timeStamp) {
        if (timeStamp == null || timeStamp.isEmpty()) return "";
        try {
            Instant instant = Instant.parse(timeStamp);
            ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            return zonedDateTime.format(dateTimeFormatter);
        } catch (Exception e) {
            try {
                // Fallback đơn giản: Nếu chuỗi đã có sẵn giờ phút dạng HH:mm thì trả về luôn
                return timeStamp.substring(11, 16);
            } catch (Exception ex) {
                return "";
            }
        }
    }

    private void showProfile() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            showAlert("Error", "User not found", Alert.AlertType.ERROR);
            return;
        }

        // Create profile view
        VBox profileView = createProfileView(currentUser);
        
        // Replace center content with profile view
        mainContainer.setCenter(profileView);
    }

    private VBox createProfileView(User user) {
        VBox profileContainer = new VBox(20);
        profileContainer.setPadding(new Insets(30));
        profileContainer.getStyleClass().add("profile-container");
        profileContainer.setAlignment(Pos.TOP_CENTER);

        // Back button
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Button backBtn = new Button("Quay lại");
        FontIcon backIcon = new FontIcon("mdi2a-arrow-left");
        backIcon.setIconSize(18);
        backBtn.setGraphic(backIcon);
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> {
            // Restore original center content
            mainContainer.setCenter(centerContent);
        });
        headerBox.getChildren().add(backBtn);

        // Profile content
        VBox profileContent = new VBox(20);
        profileContent.setAlignment(Pos.TOP_CENTER);
        profileContent.setMaxWidth(600);

        // Avatar
        ImageView avatar = new ImageView();
        avatar.setFitWidth(120);
        avatar.setFitHeight(120);
        avatar.getStyleClass().add("profile-avatar-large");
        try {
            if (user.getProfilePic() != null && !user.getProfilePic().isEmpty()) {
                avatar.setImage(new Image(user.getProfilePic()));
            } else {
                avatar.setImage(new Image(getClass().getResource("/account.png").toExternalForm()));
            }
        } catch (Exception e) {
            avatar.setImage(new Image(getClass().getResource("/account.png").toExternalForm()));
        }

        // User name
        Label nameLabel = new Label(user.getFullName() != null ? user.getFullName() : "N/A");
        nameLabel.getStyleClass().add("profile-name");

        // Email
        Label emailLabel = new Label(user.getEmail() != null ? user.getEmail() : "N/A");
        emailLabel.getStyleClass().add("profile-email");

        // User ID
        Label idLabel = new Label("ID: " + (user.get_id() != null ? user.get_id() : "N/A"));
        idLabel.getStyleClass().add("profile-id");

        // Divider
        Separator divider = new Separator();
        divider.setMaxWidth(400);

        // Info section
        VBox infoSection = new VBox(15);
        infoSection.setAlignment(Pos.CENTER_LEFT);
        infoSection.setPadding(new Insets(20, 0, 0, 0));

        HBox nameInfo = createInfoRow("Họ tên:", user.getFullName() != null ? user.getFullName() : "N/A");
        HBox emailInfo = createInfoRow("Email:", user.getEmail() != null ? user.getEmail() : "N/A");
        HBox idInfo = createInfoRow("User ID:", user.get_id() != null ? user.get_id() : "N/A");

        infoSection.getChildren().addAll(nameInfo, emailInfo, idInfo);

        profileContent.getChildren().addAll(avatar, nameLabel, emailLabel, divider, infoSection);
        profileContainer.getChildren().addAll(headerBox, profileContent);

        return profileContainer;
    }

    private HBox createInfoRow(String label, String value) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5, 0, 5, 0));

        Label labelField = new Label(label);
        labelField.getStyleClass().add("profile-info-label");
        labelField.setMinWidth(100);

        Label valueField = new Label(value);
        valueField.getStyleClass().add("profile-info-value");

        row.getChildren().addAll(labelField, valueField);
        return row;
    }

    private void showSettings() {
        // Create settings view
        VBox settingsView = createSettingsView();
        
        // Replace center content with settings view
        mainContainer.setCenter(settingsView);
    }

    private VBox createSettingsView() {
        VBox settingsContainer = new VBox(20);
        settingsContainer.setPadding(new Insets(30));
        settingsContainer.getStyleClass().add("settings-container");
        settingsContainer.setAlignment(Pos.TOP_CENTER);

        // Back button
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Button backBtn = new Button("Quay lại");
        FontIcon backIcon = new FontIcon("mdi2a-arrow-left");
        backIcon.setIconSize(18);
        backBtn.setGraphic(backIcon);
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> {
            // Restore original center content
            mainContainer.setCenter(centerContent);
        });
        headerBox.getChildren().add(backBtn);

        // Settings content
        VBox settingsContent = new VBox(30);
        settingsContent.setAlignment(Pos.TOP_CENTER);
        settingsContent.setMaxWidth(600);

        // Title
        Label titleLabel = new Label("Cài đặt");
        titleLabel.getStyleClass().add("settings-title");

        // Theme section
        VBox themeSection = new VBox(15);
        themeSection.setAlignment(Pos.CENTER_LEFT);
        themeSection.setPadding(new Insets(20));
        themeSection.getStyleClass().add("settings-section");

        Label themeLabel = new Label("Giao diện");
        themeLabel.getStyleClass().add("settings-section-title");

        ToggleGroup themeGroup = new ToggleGroup();
        
        RadioButton lightTheme = new RadioButton("Sáng (Light)");
        lightTheme.setToggleGroup(themeGroup);
        lightTheme.getStyleClass().add("theme-radio");
        
        RadioButton darkTheme = new RadioButton("Tối (Dark)");
        darkTheme.setToggleGroup(themeGroup);
        darkTheme.getStyleClass().add("theme-radio");

        // Set current theme
        ThemeService.Theme currentTheme = ThemeService.getTheme();
        if (currentTheme == ThemeService.Theme.DARK) {
            darkTheme.setSelected(true);
        } else {
            lightTheme.setSelected(true);
        }

        // Theme change handler
        themeGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == lightTheme) {
                ThemeService.setTheme(ThemeService.Theme.LIGHT);
                applyTheme("/styles.css");
            } else if (newValue == darkTheme) {
                ThemeService.setTheme(ThemeService.Theme.DARK);
                applyTheme("/styles-dark.css");
            }
        });

        VBox themeOptions = new VBox(10);
        themeOptions.getChildren().addAll(lightTheme, darkTheme);
        themeSection.getChildren().addAll(themeLabel, themeOptions);

        settingsContent.getChildren().addAll(titleLabel, themeSection);
        settingsContainer.getChildren().addAll(headerBox, settingsContent);

        return settingsContainer;
    }

    private void applyTheme(String stylesheet) {
        if (scene != null) {
            // Remove old stylesheets
            scene.getStylesheets().clear();
            // Add new stylesheet
            scene.getStylesheets().add(getClass().getResource(stylesheet).toExternalForm());
        }
    }

    private void startVideoCall(String friendId) {
        // TODO: Implement video call functionality
        // For now, just show an alert
        showAlert("Video Call", "Video call feature coming soon! Friend ID: " + friendId, Alert.AlertType.INFORMATION);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Custom ListCell for users
    private class UserListCell extends ListCell<User> {
        @Override
        protected void updateItem(User user, boolean empty) {
            super.updateItem(user, empty);

            if (empty || user == null) {
                setGraphic(null);
            } else {
                HBox cell = new HBox(15);
                cell.setPadding(new Insets(10));
                cell.setAlignment(Pos.CENTER_LEFT);

                ImageView avatar = new ImageView();
                avatar.setFitWidth(48);
                avatar.setFitHeight(48);
                avatar.getStyleClass().add("user-avatar");
                try {
                    if (user.getProfilePic() != null && !user.getProfilePic().isEmpty()) {
                        avatar.setImage(new Image(user.getProfilePic()));
                    }
                } catch (Exception e) {
                    // Use default - no image
                    avatar.setImage(null);
                }

                VBox userInfo = new VBox(5);
                Label userName = new Label(user.getFullName());
                userName.getStyleClass().add("user-name");
                Label userStatus = new Label("Offline");
                userStatus.getStyleClass().add("user-status");
                userInfo.getChildren().addAll(userName, userStatus);

                cell.getChildren().addAll(avatar, userInfo);
                setGraphic(cell);
            }
        }
    }
}

