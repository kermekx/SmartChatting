package com.kermekx.smartchatting.message;

import android.graphics.drawable.Drawable;

/**
 * Created by kermekx on 03/02/2016.
 *
 * Message data to display item list in the main activity
 */
public class Message {

    private String username;
    private String lastMessage;
    private String email;
    private String publicKey;
    private Drawable icon;

    public Message(String pseudo, String lastMessage, String email, String publicKey) {
        this.username = pseudo;
        this.lastMessage = lastMessage;
        this.email = email;
        this.publicKey = publicKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }
}
