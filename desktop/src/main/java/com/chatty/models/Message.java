package com.chatty.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Message {
    private String _id;

    // --- AFTER ---
    @SerializedName("senderId") // Báo cho GSON biết trường này tương ứng với "senderId" trong JSON
    private User sender; // Kiểu dữ liệu bây giờ là User object

    private String receiverId;
    private String content;
    private String image;
    private String createdAt;
    private List<String> seenBy = new ArrayList<>();

    public Message() {
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getSenderId() {
        return (sender != null) ? sender.get_id() : null;
    }

    // Thêm hàm getter cho object sender
    public User getSender() {
        return sender;
    }

    // Sửa hàm setter (nếu cần)
    public void setSender(User sender) {
        this.sender = sender;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getSeenBy() {
        return seenBy;
    }

    public void setSeenBy(List<String> seenBy) {
        this.seenBy = seenBy;
    }

    public boolean isSeenBy(String userId){
        return seenBy != null && seenBy.contains(userId);
    }
}

