package com.chatty.services;

import com.chatty.models.Message;
import com.google.gson.Gson;
import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import javafx.application.Platform;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
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

            if(ApiService.authToken != null){
                opts.auth = Collections.singletonMap("token", ApiService.authToken);
            } else {
                System.err.println("Token chưa có");
            }

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

            socket.on("receive-message", args -> {
                if (args.length > 0 && onNewMessage != null) {
                    try {
                        JSONObject jsonObject = (JSONObject) args[0];

                        Message message = new Message();
                        message.setSenderId(jsonObject.optString("senderId"));
                        message.setContent(jsonObject.optString("content"));
                        message.setSentAt(jsonObject.optString("sentAt"));

                        Platform.runLater(() -> onNewMessage.accept(message));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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

    public void sendMessage(String receiverId, String content){
        if(socket == null || !socket.connected()){
            System.out.println("Chưa kết nối socket");
            return;
        }

        try {
            JSONObject payload = new JSONObject();
            payload.put("receiverId", receiverId);
            payload.put("content", content);

            socket.emit("send-message", payload, (Ack) args ->{
                JSONObject response = (JSONObject) args[0];
                boolean success = response.optBoolean("success");

                if(success){
                    System.out.println("Đã gửi tin nhắn");
                } else {
                    System.out.println("Gửi tin thất bại");
                }
            });
        } catch ( Exception e){
            e.printStackTrace();
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

