package com.chatty.models;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class User {
    private String _id;
    private String username;
    private String fullName;
    private String email;
    private String profilePic;
    private String token;
    private final BooleanProperty isOnline = new SimpleBooleanProperty(false);

    public User() {}

    public User(String _id, String username, String fullName, String email, String profilePic) {
        this._id = _id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.profilePic = profilePic;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

    public boolean isOnline() {
        return isOnline.get();
    }

    public BooleanProperty isOnlineProperty() {
        return isOnline;
    }

    public void setOnline(boolean online){
        this.isOnline.set(online);
    }
}

