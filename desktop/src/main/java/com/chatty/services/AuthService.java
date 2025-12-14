package com.chatty.services;

import com.chatty.models.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import okhttp3.Cookie;
import okhttp3.HttpUrl;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AuthService {
    private final ApiService apiService;
    private String sessionCookie;
    public static User currentUser;

    public AuthService() {
        this.apiService = new ApiService();
        this.sessionCookie = loadSessionCookie();
    }

    public boolean checkAuth() {
        try {
            User user = apiService.get("/auth/check", User.class, null);
            if (user != null && user.get_id() != null) {
                this.currentUser = user;
                return true;
            }
        } catch (IOException e) {
            // Not authenticated
        }
        return false;
    }

    public User login(String username, String password) throws IOException {
        JsonObject loginData = new JsonObject();
        loginData.addProperty("username", username);
        loginData.addProperty("password", password);

        JsonObject loginResponse = apiService.post("/auth/login", loginData, JsonObject.class);

        if(loginResponse == null || !loginResponse.has("token")){
            throw new IOException("Đăng nhập thất bại: không nhận được token");
        }

        String token = loginResponse.get("token").getAsString();
        ApiService.authToken = token;

        JsonObject meResponse = apiService.get("/auth/me", JsonObject.class, null);

        if(meResponse == null || !meResponse.has("user")){
            throw new IOException("Đăng nhập thất bại: không nhận được thông tin user");
        }

        Gson gson = new Gson();
        User user = gson.fromJson(meResponse.get("user"), User.class);

        user.setToken(token);
        this.currentUser = user;
        saveSessionCookie();

        return user;
    }

    public User signup(String fullName, String email, String password) throws IOException {
        JsonObject signupData = new JsonObject();
        signupData.addProperty("fullName", fullName);
        signupData.addProperty("email", email);
        signupData.addProperty("password", password);

        User user = apiService.post("/auth/signup", signupData, User.class);
        if (user != null && user.get_id() != null) {
            this.currentUser = user;
            saveSessionCookie();
        }
        return user;
    }

    public void logout() {
        try {
            JsonObject empty = new JsonObject();
            apiService.post("/auth/logout", empty, JsonObject.class);
        } catch (IOException e) {
            // Ignore
        }
        this.currentUser = null;
        this.sessionCookie = null;
        clearSessionCookie();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    private void saveSessionCookie() {
        // In a real app, save cookie to persistent storage
    }

    private String loadSessionCookie() {
        // In a real app, load cookie from persistent storage
        return null;
    }

    private void clearSessionCookie() {
        // In a real app, clear cookie from persistent storage
    }
}

