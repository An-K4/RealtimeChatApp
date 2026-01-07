package com.chatty.controllers;

import com.chatty.models.Message;
import com.chatty.models.User;
import com.chatty.services.AuthService;
import com.chatty.services.ChatService;
import com.chatty.services.SocketService;
import com.chatty.services.ThemeService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class HomeController {
    private final AuthService authService;
    private final ChatService chatService;
    private final SocketService socketService;
    private User selectedUser;
    private List<User> allUsers;
    private List<User> currentDisplayedUsers;
    private List<User> latestSearchResults;
    private List<Message> messages;
    private Set<String> onlineUserIds = new HashSet<>();
    private Timer typingTimer; // typing stop timer
    private Timer searchDebounceTimer;

    // components
    private VBox messageContainer;
    private ScrollPane messageScrollPane;
    private TextField messageInput;
    private ListView<User> userListView;
    private Stage primaryStage;
    private BorderPane mainContainer;
    private HBox centerContent;
    private Scene scene;
    private Label onlineCountLabel;
    private CheckBox onlineOnlyCheck;
    private TextField searchField;
    Label searchStatusLabel;

    public HomeController() {
        this.authService = new AuthService();
        this.socketService = new SocketService();
        this.chatService = new ChatService(socketService);
        this.messages = new ArrayList<>();
        this.allUsers = new ArrayList<>();
        this.currentDisplayedUsers = new ArrayList<>();
        this.latestSearchResults = new ArrayList<>();
    }

    public void show(Stage primaryStage, User user) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Kma Chatty");
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
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
            setupSocketListeners();
            socketService.connect(currentUser.get_id());
        }

        scene = new Scene(mainContainer, 1200, 700);
        // Load theme preference
        String themeStylesheet = ThemeService.getThemeStylesheet();
        scene.getStylesheets().add(getClass().getResource(themeStylesheet).toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.sizeToScene();
        primaryStage.centerOnScreen();
        primaryStage.show();

        Platform.runLater(mainContainer::requestFocus);
    }

    private void setupSocketListeners() {
        // get full list first
        socketService.setOnOnlineListReceived(onlineIds -> {
            // debug
            System.out.println("CLIENT: Đã nhận danh sách online từ Server: " + onlineIds);
            System.out.println("CLIENT: Số lượng user hiện tại trong list: " + (userListView.getItems() != null ? userListView.getItems().size() : "null"));

            Platform.runLater(() -> {
                onlineUserIds.clear();
                onlineUserIds.addAll(onlineIds);

                if (allUsers != null) {
                    for (User u : allUsers) {
                        u.setOnline(onlineUserIds.contains(u.get_id()));
                    }
                    updateListViewBasedOnFilterAndSearch();
                    updateOnlineCountLabel();
                }
            });

        });

        // new user online
        socketService.setOnUserOnline(userId -> {
            Platform.runLater(() -> {
                // update temp, avoid race condition bug
                onlineUserIds.add(userId);

                if (allUsers != null) {
                    allUsers.stream()
                            .filter(u -> u.get_id().equals(userId))
                            .findFirst()
                            .ifPresent(u -> {
                                u.setOnline(true);
                            });
                    updateListViewBasedOnFilterAndSearch();
                    updateOnlineCountLabel();
                }
            });
        });

        // new user offline
        socketService.setOnUserOffline(userId -> {
            Platform.runLater(() -> {
                onlineUserIds.remove(userId);

                if (userListView.getItems() != null) {
                    userListView.getItems().stream()
                            .filter(u -> u.get_id().equals(userId))
                            .findFirst()
                            .ifPresent(u -> u.setOnline(false));
                    updateListViewBasedOnFilterAndSearch();
                    updateOnlineCountLabel();
                }
            });
        });

        // typing
        socketService.setOnTypingStart(senderId -> {
            updateUserTypingStatus(senderId, true);
        });

        socketService.setOnTypingStop(senderId -> {
            updateUserTypingStatus(senderId, false);
        });

        // new message
        socketService.setOnNewMessage(message -> {
            if (selectedUser != null && message.getSenderId().equals(selectedUser.get_id())) {
                messages.add(message);
                Platform.runLater(() -> {
                    renderMessages();
                    socketService.emitSeenMessage(selectedUser.get_id());
                });
            } else {
                // update unread count
                Platform.runLater(() -> {
                    String senderId = message.getSenderId();

                    // find user to increase unread count
                    this.allUsers.stream()
                            .filter(u -> u.get_id().equals(senderId))
                            .findFirst()
                            .ifPresent(u -> {
                                u.setUnreadCount(u.getUnreadCount() + 1);
                                userListView.refresh();
                            });
                });
            }

            updateSidebarLastMessage(message);
        });

        // message seen
        socketService.setOnMessageSeen(data -> {
            Platform.runLater(() -> {
                // check if are chatting with
                String viewerId = data.get("viewerId").getAsString();

                if (selectedUser != null && selectedUser.get_id().equals(viewerId)) {
                    // find lastMessage label to change seen status
                    Label statusLabel = (Label) scene.lookup("#lastMessageStatus");
                    if (statusLabel != null) {
                        statusLabel.setText("Đã xem");
                    }
                }
            });
        });
    }

    private void updateSidebarLastMessage(Message message) {
        Platform.runLater(() -> {
            String otherUserId = message.getSenderId().equals(authService.getCurrentUser().get_id())
                    ? message.getReceiverId()
                    : message.getSenderId();

            this.allUsers.stream()
                    .filter(u -> u.get_id().equals(otherUserId))
                    .findFirst()
                    .ifPresent(user -> {
                        // create new LastMessage obj
                        User.LastMessage lastMsg = new User.LastMessage();
                        lastMsg.setContent(message.getContent());
                        lastMsg.setCreatedAt(message.getCreatedAt());

                        boolean isMine = message.getSenderId().equals(authService.getCurrentUser().get_id());
                        lastMsg.setIsMine(isMine);

                        user.setLastMessage(lastMsg);

                        // pop this user to the top
                        this.allUsers.remove(user);
                        this.allUsers.add(0, user);
                        updateListViewBasedOnFilterAndSearch();
                        if (selectedUser != null && userListView.getSelectionModel().getSelectedItems() != null) {
                            userListView.getSelectionModel().select(selectedUser);
                        }
                    });
        });
    }

    private void updateUserTypingStatus(String senderId, boolean isTyping) {
        if (allUsers == null) return;
        allUsers.stream()
                .filter(u -> u.get_id().equals(senderId))
                .findFirst()
                .ifPresent(u -> u.setTyping(isTyping)); // model auto update text
    }

    private void updateOnlineCountLabel() {
        Platform.runLater(() -> {
            if (allUsers == null || onlineCountLabel == null) return;

            long count = userListView.getItems().stream().filter(User::isOnline).count();
            onlineCountLabel.setText("(" + count + " online)");
        });
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

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

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
        navbar.getChildren().addAll(logoContainer, spacer, rightButtons);

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

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.getStyleClass().add("input-search");
        FontIcon searchIcon = new FontIcon("mdi2m-magnify");
        searchIcon.setIconSize(24);
        searchField = new TextField();
        searchField.setPromptText("Click to search user");
        searchField.getStyleClass().add("sidebar-title");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchBox.getChildren().addAll(searchIcon, searchField);
        searchStatusLabel = new Label("Đang tìm kiếm...");
        searchStatusLabel.getStyleClass().add("search-status-label");
        searchStatusLabel.setAlignment(Pos.CENTER);
        searchStatusLabel.setMaxWidth(Double.MAX_VALUE);
        searchStatusLabel.setVisible(false);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (searchDebounceTimer != null) {
                searchDebounceTimer.cancel();
            }

            searchDebounceTimer = new Timer();
            searchDebounceTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> {
                        String searchTerm = searchField.getText().trim();
                        if (searchTerm.isEmpty()) {
                            latestSearchResults.clear();
                            updateListViewBasedOnFilterAndSearch();
                            searchStatusLabel.setVisible(false);
                        } else {
                            performSearch(searchTerm);
                        }
                    });
                }
            }, 500);
        });

        // Filter checkbox
        onlineOnlyCheck = new CheckBox("Show online only");
        onlineOnlyCheck.getStyleClass().add("filter-checkbox");
        onlineOnlyCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            updateListViewBasedOnFilterAndSearch();
        });

        onlineCountLabel = new Label("(0 online)");
        onlineCountLabel.getStyleClass().add("online-count");

        HBox filterContainer = new HBox(10);
        filterContainer.getChildren().addAll(onlineOnlyCheck, onlineCountLabel);

        sidebarHeader.getChildren().addAll(searchBox, filterContainer);

        // User list
        userListView = new ListView<>();
        userListView.setCellFactory(list -> new UserListCell());
        userListView.getStyleClass().add("user-list");

        userListView.setOnMouseClicked(e -> {
            User selected = userListView.getSelectionModel().getSelectedItem();
            if (selected != null && !searchStatusLabel.isVisible()) {
                selectUser(selected);
            }
        });

        StackPane userListStack = new StackPane();
        VBox.setVgrow(userListStack, Priority.ALWAYS);
        StackPane.setAlignment(searchStatusLabel, Pos.CENTER);

        userListStack.getChildren().addAll(userListView, searchStatusLabel);

        sidebar.getChildren().addAll(sidebarHeader, userListStack);

        return sidebar;
    }

    private void performSearch(String searchTearm) {
        userListView.getItems().clear();
        userListView.refresh();
        searchStatusLabel.setText("Đang tìm kiếm...");
        searchStatusLabel.setVisible(true);

        new Thread(() -> {
            try {
                List<User> searchResults = chatService.searchUser(searchTearm);
                Platform.runLater(() -> {
                    latestSearchResults.clear();
                    latestSearchResults.addAll(searchResults);
                    updateListViewBasedOnFilterAndSearch();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    userListView.getItems().clear();
                    latestSearchResults.clear();
                    searchStatusLabel.setText("Lỗi khi tìm kiếm!");
                    searchStatusLabel.setVisible(true);
                });
            }
        }).start();
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

        messageContainer = new VBox(15);
        messageContainer.setPadding(new Insets(20));
        messageScrollPane.setContent(messageContainer);
        messageContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            messageScrollPane.setVvalue(1.0);
        });

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

        messageInput.textProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedUser == null) return;

            // is typing (no type before)
            if (!newVal.isEmpty()) {
                socketService.emitStartTyping(selectedUser.get_id());

                // reset old timer
                if (typingTimer != null) typingTimer.cancel();

                // new timer after 2s no type
                typingTimer = new Timer();
                typingTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        socketService.emitStopTyping(selectedUser.get_id());
                    }
                }, 2000);
            }
        });
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
                allUsers = chatService.getUsers();
                Platform.runLater(() -> {
                    for (User u : allUsers) {
                        if (onlineUserIds.contains(u.get_id())) {
                            u.setOnline(true);
                        }
                        u.updateStatusPreview();
                    }

                    userListView.getItems().clear();
                    userListView.getItems().addAll(allUsers);

                    updateListViewBasedOnFilterAndSearch();
                    updateOnlineCountLabel();
                    searchStatusLabel.setVisible(false);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert("Error", "Failed to load users: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    private void updateListViewBasedOnFilterAndSearch() {
        Platform.runLater(() -> {
            String currentSearchTerm = searchField != null ? searchField.getText().trim() : "";
            List<User> usersToFilter;

            if (!currentSearchTerm.isEmpty() && !latestSearchResults.isEmpty()) {
                usersToFilter = new ArrayList<>(latestSearchResults);
                System.out.println("latest" + latestSearchResults);
            } else if (currentSearchTerm.isEmpty()) {
                usersToFilter = new ArrayList<>(this.allUsers);
            } else {
                userListView.getItems().clear();
                searchStatusLabel.setText("Không tìm thấy người dùng.");
                searchStatusLabel.setVisible(true);

                currentDisplayedUsers.clear();
                return;
            }

            for (User u : usersToFilter) {
                u.setOnline(onlineUserIds.contains(u.get_id()));
                u.updateStatusPreview();
            }

            List<User> finalUsersToDisplay = new ArrayList<>(usersToFilter);
            System.out.println(finalUsersToDisplay + "before check");
            System.out.println(usersToFilter + "usertofilter");
            System.out.println(latestSearchResults + "latest");
            if (onlineOnlyCheck != null && onlineOnlyCheck.isSelected()) {
                finalUsersToDisplay = finalUsersToDisplay.stream()
                        .filter(User::isOnline)
                        .toList();
                System.out.println("final only check" + finalUsersToDisplay);
            }

            currentDisplayedUsers.clear();
            currentDisplayedUsers.addAll(finalUsersToDisplay);
            userListView.getItems().setAll(currentDisplayedUsers);
            System.out.println("final" + finalUsersToDisplay);
            System.out.println("current" + currentDisplayedUsers);
            System.out.println();

            if (finalUsersToDisplay.isEmpty() && !currentSearchTerm.isEmpty()) {
                searchStatusLabel.setText("Không tìm thấy người dùng phù hợp");
                searchStatusLabel.setVisible(true);
            } else if (finalUsersToDisplay.isEmpty() && currentSearchTerm.isEmpty() && onlineOnlyCheck.isSelected()) {
                searchStatusLabel.setText("Không người dùng online nào khớp");
                searchStatusLabel.setVisible(true);
            } else {
                searchStatusLabel.setVisible(false);
            }
        });
    }

    private void selectUser(User user) {
        this.selectedUser = user;

        // reset unread message
        user.setUnreadCount(0);
        userListView.refresh();
        socketService.emitSeenMessage(user.get_id());

        // Update UI
        Platform.runLater(() -> {
            // Show chat header
            HBox chatHeader = (HBox) ((VBox) messageScrollPane.getParent()).getChildren().get(0);
            chatHeader.setVisible(true);
            chatHeader.setManaged(true);

            // Update header content
            chatHeader.getChildren().clear();
            Node avatarNode = createAvatarNode(user.getProfilePic(), 40, 24);
            avatarNode.getStyleClass().add("chat-header-avatar");

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
            chatHeader.getChildren().addAll(avatarNode, userInfo, videoCallBtn, closeBtn);

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

                Platform.runLater(this::renderMessages);
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

        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            boolean isMyMessage = message.getSenderId().equals(currentUser.get_id());

            HBox messageBox = new HBox(10);
            messageBox.getStyleClass().add(isMyMessage ? "message-box-right" : "message-box-left");

            VBox messageContent = new VBox(5);
            messageContent.setMaxWidth(400); // Giới hạn độ rộng tin nhắn
            messageContent.setFillWidth(false);

            // Render ảnh trong tin nhắn (nếu có)
            if (message.getImage() != null && !message.getImage().isEmpty()) {
                ImageView imageView = new ImageView(new Image(message.getImage()));
                imageView.setFitWidth(200);
                imageView.setPreserveRatio(true);
                imageView.getStyleClass().add("message-image");
                messageContent.getChildren().add(imageView);
            }

            // Render nội dung văn bản
            if (message.getContent() != null && !message.getContent().isEmpty()) {
                Label messageText = new Label(message.getContent());
                messageText.getStyleClass().add("message-text");
                messageText.setWrapText(true);
                messageContent.getChildren().add(messageText);
            }

            Label timeLabel = new Label(formatTime(message.getCreatedAt()));
            timeLabel.getStyleClass().add("message-time");
            HBox statusContainer = new HBox(2);
            statusContainer.getChildren().add(timeLabel);
            messageContent.getChildren().add(statusContainer);

            if (isMyMessage) {
                VBox myMessageContainer = new VBox(2);
                myMessageContainer.getChildren().add(messageContent);
                statusContainer.setAlignment(Pos.BOTTOM_RIGHT);
                messageContent.setAlignment(Pos.BOTTOM_RIGHT);

                Node myAvatar = createAvatarNode(currentUser.getProfilePic(), 40, 24);

                if (messages.indexOf(message) == messages.size() - 1) {
                    Label statusLabel = new Label("Đã gửi");
                    statusLabel.getStyleClass().add("message-status");
                    statusLabel.setId("lastMessageStatus");

                    if (selectedUser != null && message.isSeenBy(selectedUser.get_id())) {
                        statusLabel.setText("Đã xem");
                    }

                    statusContainer.getChildren().add(statusLabel);
                }

                messageBox.getChildren().addAll(myMessageContainer, myAvatar);
            } else {
                Node receiverAvatar = createAvatarNode(selectedUser.getProfilePic(), 40, 24);
                messageBox.getChildren().addAll(receiverAvatar, messageContent);
            }

            messageContainer.getChildren().add(messageBox);
        }
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
                Platform.runLater(this::renderMessages);
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
        VBox parentContainer = new VBox(20);
        parentContainer.setPadding(new Insets(15));
        parentContainer.getStyleClass().add("profile-container");
        parentContainer.setAlignment(Pos.TOP_CENTER);

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

        // Profile and change password content
        HBox profileContainer = new HBox(50);
        profileContainer.setMaxWidth(1000);
        profileContainer.setMinWidth(600);

        // Profile content
        VBox profileContent = new VBox(20);
        profileContent.setAlignment(Pos.TOP_CENTER);
        profileContent.setMinWidth(400);

        // Avatar
        Node avatarNode = createAvatarNode(user.getProfilePic(), 150, 120);
        avatarNode.getStyleClass().add("chat-header-avatar");

        // User name
        Label usernameLabel = new Label(user.getUsername() != null ? user.getUsername() : "N/A");
        usernameLabel.getStyleClass().add("profile-name");

        // Full name
        Label fullnameLabel = new Label(user.getFullName() != null ? user.getFullName() : "N/A");
        fullnameLabel.getStyleClass().add("profile-name");

        // Email
        Label emailLabel = new Label(user.getEmail() != null ? user.getEmail() : "N/A");
        emailLabel.getStyleClass().add("profile-email");

        // Horizontal Divider
        Separator horizontalDivider = new Separator();
        horizontalDivider.setMaxWidth(200);

        // Info section
        VBox infoSection = new VBox(15);
        infoSection.setAlignment(Pos.CENTER);
        infoSection.setPadding(new Insets(20, 0, 0, 0));
        infoSection.setMaxWidth(Region.USE_PREF_SIZE);

        HBox usernameInfo = createInfoRow("Tên đăng nhập:", user.getUsername() != null ? user.getUsername() : "N/A");
        HBox fullnameInfo = createInfoRow("Họ tên:", user.getFullName() != null ? user.getFullName() : "N/A");
        HBox emailInfo = createInfoRow("Email:", user.getEmail() != null ? user.getEmail() : "N/A");

        Button changePhotoBtn = new Button("Đổi ảnh đại diện");
        changePhotoBtn.getStyleClass().add("primary-button");
        changePhotoBtn.setMaxWidth(Double.MAX_VALUE);
        changePhotoBtn.setOnAction(e -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Chọn ảnh đại diện");
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
            );
            java.io.File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                System.out.println("Đã chọn file: " + selectedFile.getAbsolutePath());
                // Logic upload ảnh sẽ viết ở đây
            }
        });

        infoSection.getChildren().addAll(usernameInfo, fullnameInfo, emailInfo, changePhotoBtn);

        profileContent.getChildren().addAll(avatarNode, fullnameLabel, emailLabel, horizontalDivider, infoSection);

        // Vertical Divider
        Separator verticalDivider = new Separator(Orientation.VERTICAL);
        verticalDivider.setMaxHeight(500);

        // Change password
        VBox changePasswordContent = new VBox(20);
        changePasswordContent.setAlignment(Pos.BOTTOM_LEFT);
        changePasswordContent.setMinWidth(400);
        changePasswordContent.setPadding(new Insets(0, 0, 0, 30));

        Label changePassTitle = new Label("Đổi mật khẩu");
        changePassTitle.getStyleClass().add("settings-section-title");

        VBox passForm = new VBox(15);

        // Hàm helper tạo input mật khẩu nhanh
        VBox oldPassGroup = createPasswordInputGroup("Mật khẩu cũ", "Nhập mật khẩu hiện tại");
        VBox newPassGroup = createPasswordInputGroup("Mật khẩu mới", "Nhập mật khẩu mới");
        VBox confirmPassGroup = createPasswordInputGroup("Xác nhận mật khẩu mới", "Nhập lại mật khẩu mới");

        Button updatePassBtn = new Button("Cập nhật mật khẩu");
        updatePassBtn.getStyleClass().add("primary-button");
        updatePassBtn.setPrefWidth(Double.MAX_VALUE);
        updatePassBtn.setOnAction(e -> {
            // Logic đổi mật khẩu sẽ viết ở đây
            System.out.println("Đang thực hiện đổi mật khẩu...");
        });

        passForm.getChildren().addAll(oldPassGroup, newPassGroup, confirmPassGroup, updatePassBtn);
        changePasswordContent.getChildren().addAll(changePassTitle, passForm);

        // Thêm 2 cột vào HBox chính
        profileContainer.getChildren().addAll(profileContent, verticalDivider, changePasswordContent);
        parentContainer.getChildren().addAll(headerBox, profileContainer);

        return parentContainer;
    }

    private VBox createPasswordInputGroup(String labelText, String prompt) {
        VBox group = new VBox(5);
        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");

        HBox inputWrapper = new HBox();
        inputWrapper.getStyleClass().add("input-container");
        inputWrapper.setAlignment(Pos.CENTER_LEFT);
        inputWrapper.setPrefHeight(45);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(prompt);
        passwordField.getStyleClass().add("text-input");

        inputWrapper.getChildren().add(passwordField);
        group.getChildren().addAll(label, inputWrapper);
        return group;
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
            // Xóa stylesheet cũ
            scene.getStylesheets().clear();

            // Nạp stylesheet mới
            String cssUrl = getClass().getResource(stylesheet).toExternalForm();
            scene.getStylesheets().add(cssUrl);

            // Ép toàn bộ giao diện tính toán lại CSS
            scene.getRoot().applyCss();
            scene.getRoot().layout();

            System.out.println("Đã áp dụng theme: " + stylesheet);
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

            // Nếu ô trống thì xóa nội dung và style
            if (empty || user == null) {
                setGraphic(null);
                setText(null);
                getStyleClass().remove("filled-cell");
            } else {
                // Thêm class để css nhận diện ô có dữ liệu
                if (!getStyleClass().contains("filled-cell")) {
                    getStyleClass().add("filled-cell");
                }

                HBox cell = new HBox(15);
                cell.setPadding(new Insets(10));
                cell.setAlignment(Pos.CENTER_LEFT);

                Node avatarNode = createAvatarNode(user.getProfilePic(), 40, 24);

                VBox userInfo = new VBox(2);
                userInfo.setAlignment(Pos.CENTER_LEFT);

                // Hiển thị tên và chấm online
                HBox nameRow = new HBox(6);
                nameRow.setAlignment(Pos.CENTER_LEFT);

                Label userName = new Label(user.getFullName());
                userName.getStyleClass().add("user-name");

                // Chấm xanh
                Circle onlineDot = new Circle(4);
                onlineDot.getStyleClass().add("online-dot");
                onlineDot.visibleProperty().bind(user.isOnlineProperty());

                nameRow.getChildren().addAll(userName, onlineDot);

                Label statusLabel = new Label();
                statusLabel.getStyleClass().add("last-message-preview");
                statusLabel.setMaxWidth(180);

                // update text when User.statusPreview change
                statusLabel.textProperty().bind(user.statusPreviewProperty());

                // data binding style
                user.isTypingProperty().addListener((obs, wasTyping, isTyping) -> {
                    if (isTyping) {
                        statusLabel.setStyle("-fx-text-fill: #31a24c; -fx-font-style: italic;");
                    } else {
                        statusLabel.setStyle("");
                    }
                });

                // unread messages
                if (user.getUnreadCount() > 0) {
                    Label badge = new Label(String.valueOf(user.getUnreadCount()));
                    badge.getStyleClass().add("unread-badge");

                    HBox.setHgrow(userInfo, Priority.ALWAYS);
                    cell.getChildren().addAll(avatarNode, userInfo, badge);
                } else {
                    cell.getChildren().addAll(avatarNode, userInfo);
                }

                userInfo.getChildren().addAll(nameRow, statusLabel);
                setGraphic(cell);
            }
        }
    }

    private Node createAvatarNode(String photoUrl, double avatarNodeSize, int iconSize) {
        if (photoUrl != null && !photoUrl.isEmpty()) {
            try {
                ImageView avatar = new ImageView(new Image(photoUrl, true));
                avatar.setFitWidth(avatarNodeSize);
                avatar.setFitHeight(avatarNodeSize);
                avatar.getStyleClass().add("message-avatar");
                return avatar;
            } catch (Exception e) {
                // Nếu load ảnh lỗi thì rơi xuống phần tạo icon mặc định bên dưới
            }
        }

        // --- TẠO FONT ICON MẶC ĐỊNH ---
        FontIcon defaultIcon = new FontIcon("mdi2a-account");
        defaultIcon.setIconSize(iconSize);
        defaultIcon.getStyleClass().add("avatar-icon"); // Để đổi màu trong CSS

        // Cho icon vào một cái vòng tròn (StackPane) để trông chuyên nghiệp hơn
        StackPane container = new StackPane(defaultIcon);
        container.setPrefSize(avatarNodeSize, avatarNodeSize);
        container.setMinSize(avatarNodeSize, avatarNodeSize);
        container.getStyleClass().add("default-avatar-container");

        return container;
    }
}