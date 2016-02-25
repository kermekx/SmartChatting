package com.kermekx.smartchatting.conversation;

import android.graphics.drawable.Drawable;

/**
 * Created by kermekx on 12/02/2016.
 */
public class Conversation {

    private boolean sended;
    private Drawable icon;
    private String message;

    public Conversation(boolean sended, Drawable icon, String message) {
        this.sended = sended;
        this.icon = icon;
        this.message = message;
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
}
