package com.chatty.services;

import com.chatty.models.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;

public class UserService {
    private final ApiService apiService;
    private final Gson gson;

    public UserService() {
        this.apiService = new ApiService();
        this.gson = new Gson();
    }

    // Class nội bộ để parse response khi upload ảnh (lấy URL)
    private static class UploadResponse {
        String url;
    }

    /**
     * Quy trình cập nhật Avatar toàn diện:
     * 1. Upload file ảnh lên server file (Multipart)
     * 2. Lấy URL trả về
     * 3. Gọi API update user info để lưu URL mới
     */
    public String updateUserAvatar(File file) throws IOException {
        UploadResponse uploadRes = apiService.postMultipart("/messages/upload", file, UploadResponse.class);

        if (uploadRes == null || uploadRes.url == null) {
            throw new IOException("Không thể upload ảnh lên server lưu trữ.");
        }

        String newAvatarUrl = uploadRes.url;
        JsonObject payload = new JsonObject();
        payload.addProperty("avatar", newAvatarUrl);

        apiService.patch("/users/upload-avatar", payload, JsonObject.class);

        return newAvatarUrl;
    }

    public void changePassword(String oldPassword, String newPassword) throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("oldPassword", oldPassword);
        payload.addProperty("newPassword", newPassword);
        apiService.post("/users/change-password", payload, JsonObject.class);
    }

    public User getCurrentUserInfo() throws IOException {
        try {
            JsonObject response = apiService.get("/auth/me", JsonObject.class, null);

            if (response != null && response.has("user")) {
                return gson.fromJson(response.get("user"), User.class);
            }

            return null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Không thể lấy thông tin người dùng: " + e.getMessage());
        }
    }
}