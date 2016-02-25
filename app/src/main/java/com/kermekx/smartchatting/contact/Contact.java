package com.kermekx.smartchatting.contact;

import android.graphics.drawable.Drawable;

/**
 * Created by kermekx on 03/02/2016.
 */
public class Contact {

    private String username;
    private Drawable icon;

    public Contact(String pseudo, Drawable icon) {
        this.username = pseudo;
        this.icon = icon;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }
}
