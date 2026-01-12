package com.chatty.models;

import com.google.gson.annotations.SerializedName;
import javafx.beans.property.*;
import java.util.ArrayList;
import java.util.List;

public class Group {
    private String _id;
    private String name;
    private String description;
    private String avatar;
    private User owner;
    private List<GroupMember> members = new ArrayList<>();
    private boolean isActive;
    private int unreadCount; // Sửa từ IntegerProperty thành int
    private LastMessage lastMessage;

    // Các Property này là "transient" - GSON sẽ bỏ qua chúng
    // Chúng chỉ dùng để binding với giao diện JavaFX
    private final transient StringProperty statusPreview = new SimpleStringProperty("");
    private final transient BooleanProperty isTyping = new SimpleBooleanProperty(false);

    public static class GroupMember {
        @SerializedName("userId")
        private User user; // Có thể là object User hoặc chỉ String ID
        private String role; // "admin" hoặc "member"
        private String joinedAt;

        public GroupMember() {}

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getJoinedAt() {
            return joinedAt;
        }

        public void setJoinedAt(String joinedAt) {
            this.joinedAt = joinedAt;
        }

        public boolean isAdmin() {
            return "admin".equals(role);
        }
    }

    public static class LastMessage {
        private String content;
        private String createdAt;
        private String senderName;
        private boolean isMine;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getSenderName() {
            return senderName;
        }

        public void setSenderName(String senderName) {
            this.senderName = senderName;
        }

        public boolean isMine() {
            return isMine;
        }

        public void setIsMine(boolean isMine) {
            this.isMine = isMine;
        }
    }

    public Group() {}

    // Update status preview (giống User model)
    public void updateStatusPreview() {
        if (isTyping.get()) {
            statusPreview.set("Đang soạn tin...");
        } else {
            if (lastMessage != null && lastMessage.getContent() != null) {
                String prefix = lastMessage.isMine() ? "Bạn: " : lastMessage.getSenderName() + ": ";
                String content = lastMessage.getContent();
                if (content.length() > 25) {
                    content = content.substring(0, 25) + "...";
                }
                statusPreview.set(prefix + content);
            } else {
                statusPreview.set("Chạm để bắt đầu chat");
            }
        }
    }

    // Check if current user is admin
    public boolean isUserAdmin(String userId) {
        return members.stream()
                .anyMatch(m -> m.getUser() != null
                        && m.getUser().get_id().equals(userId)
                        && m.isAdmin());
    }

    // Check if current user is owner
    public boolean isUserOwner(String userId) {
        return owner != null && owner.equals(userId);
    }

    // Getters and Setters
    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public List<GroupMember> getMembers() {
        return members;
    }

    public void setMembers(List<GroupMember> members) {
        this.members = members;
    }

    public int getMemberCount() {
        return members != null ? members.size() : 0;
    }

    // JavaFX Properties
    public boolean isTyping() {
        return isTyping.get();
    }

    public BooleanProperty isTypingProperty() {
        return isTyping;
    }

    public void setTyping(boolean typing) {
        this.isTyping.set(typing);
        updateStatusPreview();
    }

    public StringProperty statusPreviewProperty() {
        return statusPreview;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public LastMessage getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(LastMessage lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getStatusPreview() {
        return statusPreview.get();
    }

    public boolean isIsTyping() {
        return isTyping.get();
    }
}
