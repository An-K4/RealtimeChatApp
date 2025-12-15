package com.chatty.services;

import com.chatty.models.Message;
import com.chatty.models.User;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatService {
    private final ApiService apiService;
    private final SocketService socketService;

    public ChatService(SocketService socketService) {
        this.apiService = new ApiService();
        this.socketService = socketService;
    }

    public List<User> getUsers() throws IOException {
        try {
            JsonObject response = apiService.get("/messages/users", JsonObject.class, null);

            if(response != null && response.has("users")){
                JsonArray userArray = response.getAsJsonArray("users");

                Gson gson = new Gson();
                Type listType = new TypeToken<List<User>>(){}.getType();
                return gson.fromJson(userArray, listType);
            }

            return new ArrayList<>();
        } catch (Exception e){
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Message> getMessages(String friendId) throws IOException {
        try {
            JsonObject response = apiService.get("/messages/" + friendId, JsonObject.class, null);

            if(response != null && response.has("messages")){
                JsonArray messageArray = response.getAsJsonArray("messages");

                Gson gson = new Gson();
                Type listType = new TypeToken<List<Message>>(){}.getType();
                return gson.fromJson(messageArray, listType);
            }

            return new ArrayList<>();
        } catch (Exception e){
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public Message sendMessage(String senderId, String receiverId, String content){
        if(content == null || content.trim().isEmpty()){
            return null;
        }

        Message localMsg = new Message();
        localMsg.set_id(String.valueOf(System.currentTimeMillis()));
        localMsg.setSenderId(senderId);
        localMsg.setReceiverId(receiverId);
        localMsg.setContent(content);
        localMsg.setCreatedAt(Instant.now().toString());

        socketService.sendMessage(receiverId, content);

        return localMsg;
    }
}

