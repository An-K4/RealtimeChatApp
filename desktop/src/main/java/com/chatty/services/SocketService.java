package com.chatty.services;

import com.chatty.models.Message;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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

    // chat callbacks
    private Consumer<Message> onNewMessage;
    private Consumer<List<String>> onOnlineUsersUpdate;

    // video call callbacks (Gson JsonObject)
    private Consumer<JsonObject> onCallOffer;
    private Consumer<JsonObject> onCallAnswer;
    private Consumer<JsonObject> onIceCandidate;

    public SocketService() {
        this.gson = new Gson();
        this.onlineUsers = new ArrayList<>();
    }

    // ================= CONNECT =================

    public void connect(String userId) {
        try {
            IO.Options opts = new IO.Options();

            if (ApiService.authToken != null) {
                opts.auth = Collections.singletonMap("token", ApiService.authToken);
            } else {
                System.err.println("Token chưa có");
            }

            socket = IO.socket("http://localhost:3000", opts);

            socket.on(Socket.EVENT_CONNECT, args ->
                    System.out.println("Socket connected")
            );

            // ===== online users =====
            socket.on("getOnlineUsers", args -> {
                List<String> users = new ArrayList<>();
                if (args.length > 0 && args[0] instanceof List) {
                    List<?> list = (List<?>) args[0];
                    for (Object item : list) {
                        users.add(item.toString());
                    }
                }
                onlineUsers = users;
                if (onOnlineUsersUpdate != null) {
                    Platform.runLater(() -> onOnlineUsersUpdate.accept(users));
                }
            });

            // ===== receive message =====
            socket.on("receive-message", args -> {
                if (args.length > 0 && onNewMessage != null) {
                    try {
                        Message message =
                                gson.fromJson(args[0].toString(), Message.class);

                        Platform.runLater(() -> onNewMessage.accept(message));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // ===== VIDEO CALL SIGNALING =====

            socket.on("call:offer", args -> {
                if (onCallOffer != null && args.length > 0) {
                    JsonObject data =
                            gson.fromJson(args[0].toString(), JsonObject.class);
                    Platform.runLater(() -> onCallOffer.accept(data));
                }
            });

            socket.on("call:answer", args -> {
                if (onCallAnswer != null && args.length > 0) {
                    JsonObject data =
                            gson.fromJson(args[0].toString(), JsonObject.class);
                    Platform.runLater(() -> onCallAnswer.accept(data));
                }
            });

            socket.on("call:ice", args -> {
                if (onIceCandidate != null && args.length > 0) {
                    JsonObject data =
                            gson.fromJson(args[0].toString(), JsonObject.class);
                    Platform.runLater(() -> onIceCandidate.accept(data));
                }
            });

            socket.connect();

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (socket != null) {
            socket.off();
            socket.disconnect();
            socket = null;
        }
    }

    // ================= CHAT =================

    public void sendMessage(String receiverId, String content) {
        if (socket == null || !socket.connected()) {
            System.out.println("Chưa kết nối socket");
            return;
        }

        // Dùng JSONObject của org.json
        JSONObject payload = new JSONObject();
        try {
            payload.put("receiverId", receiverId);
            payload.put("content", content);
        } catch (Exception e) {
            e.printStackTrace();
        }

        socket.emit("send-message", payload, (Ack) args -> {
            // Xử lý callback như cũ
            if (args.length > 0) {
                // Chỗ này vẫn có thể dùng Gson để parse phản hồi từ server nếu muốn
                System.out.println("Server phản hồi: " + args[0].toString());
            }
        });
    }

    // ================= VIDEO CALL EMIT =================

    public void sendCallOffer(String to, String offer) {
        if (socket == null || !socket.connected()) return;

        JsonObject obj = new JsonObject();
        obj.addProperty("to", to);
        obj.addProperty("offer", offer);

        socket.emit("call:offer", obj);
    }

    public void sendCallAnswer(String to, String answer) {
        if (socket == null || !socket.connected()) return;

        JsonObject obj = new JsonObject();
        obj.addProperty("to", to);
        obj.addProperty("answer", answer);

        socket.emit("call:answer", obj);
    }

    public void sendIceCandidate(String to, String candidate) {
        if (socket == null || !socket.connected()) return;

        JsonObject obj = new JsonObject();
        obj.addProperty("to", to);
        obj.addProperty("candidate", candidate);

        socket.emit("call:ice", obj);
    }

    // ================= SETTERS =================

    public void setOnNewMessage(Consumer<Message> callback) {
        this.onNewMessage = callback;
    }

    public void setOnOnlineUsersUpdate(Consumer<List<String>> callback) {
        this.onOnlineUsersUpdate = callback;
    }

    public void setOnCallOffer(Consumer<JsonObject> callback) {
        this.onCallOffer = callback;
    }

    public void setOnCallAnswer(Consumer<JsonObject> callback) {
        this.onCallAnswer = callback;
    }

    public void setOnIceCandidate(Consumer<JsonObject> callback) {
        this.onIceCandidate = callback;
    }

    // ================= GETTERS =================

    public List<String> getOnlineUsers() {
        return onlineUsers;
    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }
}
