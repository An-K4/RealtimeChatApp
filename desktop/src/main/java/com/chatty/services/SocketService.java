package com.chatty.services;

import com.chatty.models.Message;
import com.google.gson.Gson;
import io.socket.client.IO;
import io.socket.client.Socket;
import javafx.application.Platform;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SocketService {
    private Socket socket;
    private final Gson gson;
    private List<String> onlineUsers;
    private Consumer<Message> onNewMessage;
    private Consumer<List<String>> onOnlineUsersUpdate;

    public SocketService() {
        this.gson = new Gson();
        this.onlineUsers = new ArrayList<>();
    }

    public void connect(String userId) {
        try {
            IO.Options opts = new IO.Options();
            opts.query = "userId=" + userId;
            socket = IO.socket("http://localhost:3000", opts);
            
            socket.on(Socket.EVENT_CONNECT, args -> {
                System.out.println("Socket connected");
            });

            socket.on("getOnlineUsers", args -> {
                List<String> users = new ArrayList<>();
                if (args[0] instanceof List) {
                    List<?> list = (List<?>) args[0];
                    for (Object item : list) {
                        users.add(item.toString());
                    }
                }
                this.onlineUsers = users;
                if (onOnlineUsersUpdate != null) {
                    Platform.runLater(() -> onOnlineUsersUpdate.accept(users));
                }
            });

            socket.on("newMessage", args -> {
                if (args.length > 0 && onNewMessage != null) {
                    Message message = gson.fromJson(args[0].toString(), Message.class);
                    Platform.runLater(() -> onNewMessage.accept(message));
                }
            });

            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (socket != null && socket.connected()) {
            socket.disconnect();
        }
    }

    public void setOnNewMessage(Consumer<Message> callback) {
        this.onNewMessage = callback;
    }

    public void setOnOnlineUsersUpdate(Consumer<List<String>> callback) {
        this.onOnlineUsersUpdate = callback;
    }

    public List<String> getOnlineUsers() {
        return onlineUsers;
    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }
}

