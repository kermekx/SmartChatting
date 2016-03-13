package com.kermekx.smartchatting.contact;

import android.graphics.drawable.Drawable;

/**
 * Created by kermekx on 03/02/2016.
 *
 * Contact data to display item list in the main activity
 */
public class Contact {

    private String username;
    private String email;
    private String publicKey;
    private Drawable icon;

    public Contact(String pseudo, String email, String publicKey, Drawable icon) {
        this.username = pseudo;
        this.email = email;
        this.publicKey = publicKey;
        this.icon = icon;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
