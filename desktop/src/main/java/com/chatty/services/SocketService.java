package com.chatty.services;

import com.chatty.models.GroupMessage;
import com.chatty.models.Message;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import javafx.application.Platform;
import org.json.JSONArray;
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
    private Consumer<List<String>> onOnlineListReceived;
    private Consumer<String> onUserOnline;
    private Consumer<String> onUserOffline;

    // group callbacks
    private Consumer<GroupMessage> onNewGroupMessage;
    private Consumer<String> onGroupTypingStart;
    private Consumer<String> onGroupTypingStop;
    private Consumer<JsonObject> onGroupMessageSeen;
    private Consumer<JsonObject> onGroupCreated;
    private Consumer<JsonObject> onGroupDeleted;
    private Consumer<Void> onReloadGroups;

    // typing callbacks
    private Consumer<String> onTypingStart;
    private Consumer<String> onTypingStop;
    private Consumer<JsonObject> onMessageSeen;

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

            socket.on(Socket.EVENT_CONNECT, args -> System.out.println("Socket connected"));

            // ===== online users =====
            socket.on("noti-onlineList-toMe", args -> {
                List<String> users = new ArrayList<>();
                if (args.length > 0) {
                    if (args[0] instanceof JSONArray) {
                        JSONArray jsonArray = (JSONArray) args[0];

                        try {
                            for (int i = 0; i < jsonArray.length(); i++) {
                                users.add(jsonArray.getString(i));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                // debug
                System.out.println("DEBUG SOCKET: Đã parse được danh sách: " + users);

                onlineUsers = users;
                if (onOnlineListReceived != null) {
                    Platform.runLater(() -> onOnlineListReceived.accept(users));
                }
            });

            // new user online
            socket.on("noti-online", args -> {
                if (args.length > 0 && onUserOnline != null) {
                    try {
                        // backend send object
                        JSONObject data = (JSONObject) args[0];
                        String id = data.optString("id");
                        if (!id.isEmpty()) {
                            Platform.runLater(() -> onUserOnline.accept(id));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // new user offline
            socket.on("noti-offline", args -> {
                if (args.length > 0 && onUserOffline != null) {
                    try {
                        // backend send object
                        JSONObject data = (JSONObject) args[0];
                        String id = data.optString("id");
                        if (!id.isEmpty()) {
                            Platform.runLater(() -> onUserOffline.accept(id));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // typing start
            socket.on("typing-start", args -> {
                if (args.length > 0 && onTypingStart != null) {
                    JSONObject data = (JSONObject) args[0];
                    String senderId = data.optString("senderId");
                    Platform.runLater(() -> onTypingStart.accept(senderId));
                }
            });

            // typing stop
            socket.on("typing-stop", args -> {
                if (args.length > 0 && onTypingStop != null) {
                    JSONObject data = (JSONObject) args[0];
                    String senderId = data.optString("senderId");
                    Platform.runLater(() -> onTypingStop.accept(senderId));
                }
            });

            // seen event
            socket.on("send-message", args -> {
                if (args.length > 0 && onMessageSeen != null) {
                    try {
                        JsonObject data = gson.fromJson(args[0].toString(), JsonObject.class);
                        Platform.runLater(() -> onMessageSeen.accept(data));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // ===== receive message =====
            socket.on("receive-message", args -> {
                if (args.length > 0 && onNewMessage != null) {
                    try {
                        Message message = gson.fromJson(args[0].toString(), Message.class);

                        Platform.runLater(() -> onNewMessage.accept(message));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            socket.on("seen-message", args -> {
                if (args.length > 0 && onMessageSeen != null) {
                    try {
                        JsonObject data = gson.fromJson(args[0].toString(), JsonObject.class);
                        Platform.runLater(() -> onMessageSeen.accept(data));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            socket.on("receive-group-message", args -> {
                if (args.length > 0 && onNewGroupMessage != null) {
                    try {
                        GroupMessage message = gson.fromJson(args[0].toString(), GroupMessage.class);
                        Platform.runLater(() -> onNewGroupMessage.accept(message));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            socket.on("group-typing-start", args -> {
                if (args.length > 0 && onGroupTypingStart != null) {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        String senderName = data.optString("senderName", "Someone");
                        Platform.runLater(() -> onGroupTypingStart.accept(senderName));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            socket.on("group-typing-stop", args -> {
                if (args.length > 0 && onGroupTypingStop != null) {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        String senderId = data.optString("senderId");
                        Platform.runLater(() -> onGroupTypingStop.accept(senderId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            socket.on("user-seen-message", args -> {
                if (args.length > 0 && onGroupMessageSeen != null) {
                    try {
                        JsonObject data = gson.fromJson(args[0].toString(), JsonObject.class);
                        Platform.runLater(() -> onGroupMessageSeen.accept(data));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            socket.on("group-created", args -> {
                if (args.length > 0 && onGroupCreated != null) {
                    try {
                        JsonObject data = gson.fromJson(args[0].toString(), JsonObject.class);
                        Platform.runLater(() -> onGroupCreated.accept(data));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            socket.on("group-deleted", args -> {
                if (args.length > 0 && onGroupDeleted != null) {
                    try {
                        JsonObject data = gson.fromJson(args[0].toString(), JsonObject.class);
                        Platform.runLater(() -> onGroupDeleted.accept(data));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            socket.on("reload-groups", args -> {
                if (onReloadGroups != null) {
                    Platform.runLater(() -> onReloadGroups.accept(null));
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
            System.out.println("Socket disconnected");
        }
    }

    // typing status, seen status
    public void emitStartTyping(String receiverId) {
        if (socket != null) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("receiverId", receiverId);
            } catch (Exception e) {
                e.printStackTrace();
            }
            socket.emit("typing-start", obj);
        }
    }

    public void emitStopTyping(String receiverId) {
        if (socket != null) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("receiverId", receiverId);
            } catch (Exception e) {
                e.printStackTrace();
            }
            socket.emit("typing-stop", obj);
        }
    }

    public void emitSeenMessage(String senderId) {
        if (socket != null) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("senderId", senderId);
            } catch (Exception e) {
                e.printStackTrace();
            }
            socket.emit("seen-message", obj);
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

    // ==================== GROUP EMIT METHODS ====================
    public void joinGroup(String groupId) {
        if (socket == null || !socket.connected())
            return;

        JSONObject obj = new JSONObject();
        try {
            obj.put("groupId", groupId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        socket.emit("join-group", obj);
    }

    public void leaveGroup(String groupId) {
        if (socket == null || !socket.connected())
            return;

        JSONObject obj = new JSONObject();
        try {
            obj.put("groupId", groupId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        socket.emit("leave-group", obj);
    }

    public void sendGroupMessage(String groupId, String content) {
        if (socket == null || !socket.connected()) {
            System.out.println("Chưa kết nối socket");
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("groupId", groupId);
            payload.put("content", content);
            payload.put("replyTo", JSONObject.NULL);
            payload.put("fileUrl", JSONObject.NULL);
        } catch (Exception e) {
            e.printStackTrace();
        }

        socket.emit("send-group-message", payload, (Ack) args -> {
            if (args.length > 0) {
                System.out.println("Server phản hồi group message: " + args[0].toString());
            }
        });
    }

    public void emitGroupTypingStart(String groupId) {
        if (socket == null || !socket.connected())
            return;

        JSONObject obj = new JSONObject();
        try {
            obj.put("groupId", groupId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        socket.emit("group-typing-start", obj);
    }

    public void emitGroupTypingStop(String groupId) {
        if (socket == null || !socket.connected())
            return;

        JSONObject obj = new JSONObject();
        try {
            obj.put("groupId", groupId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        socket.emit("group-typing-stop", obj);
    }

    public void emitSeenGroupMessage(String messageId, String groupId) {
        if (socket == null || !socket.connected())
            return;

        JSONObject obj = new JSONObject();
        try {
            obj.put("messageId", messageId);
            obj.put("groupId", groupId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        socket.emit("seen-group-message", obj);
    }

    // ================= SETTERS =================

    public void setOnNewMessage(Consumer<Message> callback) {
        this.onNewMessage = callback;
    }

    public void setOnOnlineListReceived(Consumer<List<String>> callback) {
        this.onOnlineListReceived = callback;
    }

    public void setOnUserOnline(Consumer<String> onUserOnline) {
        this.onUserOnline = onUserOnline;
    }

    public void setOnUserOffline(Consumer<String> onUserOffline) {
        this.onUserOffline = onUserOffline;
    }

    public void setOnTypingStart(Consumer<String> onTypingStart) {
        this.onTypingStart = onTypingStart;
    }

    public void setOnTypingStop(Consumer<String> onTypingStop) {
        this.onTypingStop = onTypingStop;
    }

    public void setOnMessageSeen(Consumer<JsonObject> onMessageSeen) {
        this.onMessageSeen = onMessageSeen;
    }

    public void setOnNewGroupMessage(Consumer<GroupMessage> callback) {
        this.onNewGroupMessage = callback;
    }

    public void setOnGroupTypingStart(Consumer<String> callback) {
        this.onGroupTypingStart = callback;
    }

    public void setOnGroupTypingStop(Consumer<String> callback) {
        this.onGroupTypingStop = callback;
    }

    public void setOnGroupMessageSeen(Consumer<JsonObject> callback) {
        this.onGroupMessageSeen = callback;
    }

    public void setOnGroupCreated(Consumer<JsonObject> callback) {
        this.onGroupCreated = callback;
    }

    public void setOnGroupDeleted(Consumer<JsonObject> callback) {
        this.onGroupDeleted = callback;
    }

    public void setOnReloadGroups(Consumer<Void> callback) {
        this.onReloadGroups = callback;
    }

    // ================= GETTERS =================

    public List<String> getOnlineUsers() {
        return onlineUsers;
    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }
}
