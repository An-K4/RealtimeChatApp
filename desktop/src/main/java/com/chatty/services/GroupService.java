package com.chatty.services;

import com.chatty.models.Group;
import com.chatty.models.GroupMessage;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class GroupService {
    private final ApiService apiService;
    private final SocketService socketService;
    private final Gson gson;

    public GroupService(SocketService socketService) {
        this.apiService = new ApiService();
        this.socketService = socketService;
        this.gson = new Gson();
    }

    // ==================== GROUP CRUD ====================

    /**
     * Lấy danh sách tất cả nhóm của user
     */
    public List<Group> getGroups() throws IOException {
        try {
            JsonObject response = apiService.get("/groups/getGroups", JsonObject.class, null);

            if (response != null && response.has("groups")) {
                JsonArray groupArray = response.getAsJsonArray("groups");
                Type listType = new TypeToken<List<Group>>(){}.getType();
                return gson.fromJson(groupArray, listType);
            }

            return new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Tạo nhóm mới
     * @param name Tên nhóm
     * @param description Mô tả
     * @param memberIds Danh sách ID thành viên
     */
    public Group createGroup(String name, String description, List<String> memberIds) throws IOException {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("name", name);
            payload.addProperty("description", description);

            JsonArray membersArray = new JsonArray();
            for (String id : memberIds) {
                membersArray.add(id);
            }
            payload.add("members", membersArray);

            JsonObject response = apiService.post("/groups/create", payload, JsonObject.class);

            if (response != null && response.has("group")) {
                Group group = gson.fromJson(response.get("group"), Group.class);

                // Auto join group room sau khi tạo
                if (group != null) {
                    socketService.joinGroup(group.get_id());
                }

                return group;
            }

            return null;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Cập nhật thông tin nhóm
     */
    public Group updateGroup(String groupId, String name, String description, String avatar) throws IOException {
        try {
            JsonObject payload = new JsonObject();
            if (name != null) payload.addProperty("name", name);
            if (description != null) payload.addProperty("description", description);
            if (avatar != null) payload.addProperty("avatar", avatar);

            JsonObject response = apiService.put("/groups/update/" + groupId, payload, JsonObject.class);

            if (response != null && response.has("group")) {
                return gson.fromJson(response.get("group"), Group.class);
            }

            return null;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Lấy thông tin chi tiết nhóm
     */
    public Group getGroupInfo(String groupId) throws IOException {
        try {
            JsonObject response = apiService.get("/groups/" + groupId, JsonObject.class, null);

            if (response != null && response.has("group")) {
                return gson.fromJson(response.get("group"), Group.class);
            }

            return null;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Xóa nhóm (nếu là owner) hoặc rời nhóm (nếu là member)
     */
    public boolean deleteGroup(String groupId) throws IOException {
        try {
            apiService.delete("/groups/delete/" + groupId, JsonObject.class);

            // Leave group room
            socketService.leaveGroup(groupId);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    // ==================== GROUP MESSAGES ====================

    /**
     * Lấy danh sách tin nhắn trong nhóm
     */
    public List<GroupMessage> getGroupMessages(String groupId) throws IOException {
        try {
            JsonObject response = apiService.get("/groups/" + groupId + "/messages", JsonObject.class, null);

            if (response != null && response.has("messages")) {
                JsonArray messageArray = response.getAsJsonArray("messages");
                Type listType = new TypeToken<List<GroupMessage>>(){}.getType();
                return gson.fromJson(messageArray, listType);
            }

            return new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Gửi tin nhắn trong nhóm qua Socket
     * Trả về local message để hiển thị ngay trên UI
     */
    public GroupMessage sendGroupMessage(String groupId, String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        // Create local message for immediate UI feedback
        GroupMessage localMsg = new GroupMessage();
        localMsg.set_id(String.valueOf(System.currentTimeMillis()));
        localMsg.setGroupId(groupId);
        localMsg.setContent(content);
        localMsg.setCreatedAt(Instant.now().toString());

        // Send via socket
        socketService.sendGroupMessage(groupId, content);

        return localMsg;
    }

    // ==================== GROUP MEMBERS ====================

    /**
     * Lấy danh sách thành viên nhóm
     */
    public List<Group.GroupMember> getGroupMembers(String groupId) throws IOException {
        try {
            JsonObject response = apiService.get("/groups/" + groupId + "/getMembers", JsonObject.class, null);

            if (response != null && response.has("members")) {
                JsonArray memberArray = response.getAsJsonArray("members");
                Type listType = new TypeToken<List<Group.GroupMember>>(){}.getType();
                return gson.fromJson(memberArray, listType);
            }

            return new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Thêm thành viên vào nhóm
     */
    public boolean addMembers(String groupId, List<String> memberIds) throws IOException {
        try {
            JsonObject payload = new JsonObject();
            JsonArray membersArray = new JsonArray();
            for (String id : memberIds) {
                membersArray.add(id);
            }
            payload.add("memberIds", membersArray);

            apiService.post("/groups/" + groupId + "/addMembers", payload, JsonObject.class);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Xóa thành viên khỏi nhóm (chỉ admin)
     */
    public boolean removeMember(String groupId, String memberId) throws IOException {
        try {
            apiService.delete("/groups/" + groupId + "/deleteMembers/" + memberId, JsonObject.class);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Đổi quyền thành viên (chỉ owner)
     */
    public boolean changeRole(String groupId, String memberId, String newRole) throws IOException {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("newRole", newRole);

            apiService.put("/groups/" + groupId + "/changeRole/" + memberId, payload, JsonObject.class);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    // ==================== TYPING INDICATORS ====================

    /**
     * Emit typing start trong nhóm
     */
    public void startTyping(String groupId) {
        socketService.emitGroupTypingStart(groupId);
    }

    /**
     * Emit typing stop trong nhóm
     */
    public void stopTyping(String groupId) {
        socketService.emitGroupTypingStop(groupId);
    }

    /**
     * Emit seen message trong nhóm
     */
    public void markMessageAsSeen(String messageId, String groupId) {
        socketService.emitSeenGroupMessage(messageId, groupId);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Join group room để nhận real-time events
     */
    public void joinGroupRoom(String groupId) {
        socketService.joinGroup(groupId);
    }

    /**
     * Leave group room
     */
    public void leaveGroupRoom(String groupId) {
        socketService.leaveGroup(groupId);
    }
}