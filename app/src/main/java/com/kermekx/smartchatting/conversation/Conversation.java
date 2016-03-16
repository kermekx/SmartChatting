package com.kermekx.smartchatting.conversation;

import android.graphics.drawable.Drawable;

import java.security.Key;

/**
 * Created by kermekx on 12/02/2016.
 *
 * Message data to display item list in the conversation activity
 */
public class Conversation implements Comparable<Conversation> {

    private int messageID;
    private boolean sended;
    private Drawable icon;
    private String message;
    private String cryptedMessage;

    public Conversation(int messageID, boolean sended, Drawable icon, String message, String cryptedMessage) {
        this.messageID = messageID;
        this.sended = sended;
        this.icon = icon;
        this.message = message;
        this.cryptedMessage = cryptedMessage;
    }

    public boolean isSent() {
        return sended;
    }

    public void setSent(boolean sended) {
        this.sended = sended;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCryptedMessage() {
        return cryptedMessage;
    }

    @Override
    public int compareTo(Conversation another) {
        return Integer.valueOf(messageID).compareTo(Integer.valueOf(another.messageID));
    }
}
