package com.chatty.services;

import com.chatty.models.Message;
import com.chatty.models.User;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ChatService {
    private final ApiService apiService;
    private final Gson gson;

    public ChatService() {
        this.apiService = new ApiService();
        this.gson = new Gson();
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

    public Message sendMessage(String receiverId, String text, String image) throws IOException {
        com.google.gson.JsonObject messageData = new com.google.gson.JsonObject();
        if (text != null && !text.isEmpty()) {
            messageData.addProperty("text", text);
        }
        if (image != null && !image.isEmpty()) {
            messageData.addProperty("image", image);
        }
        
        return apiService.post("/messages/send/" + receiverId, messageData, Message.class);
    }
}

