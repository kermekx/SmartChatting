package com.kermekx.smartchatting.hash;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by kermekx on 30/01/2016.
 */
public class Hasher {

    private final static Hasher INSTANCE = new Hasher();

    public static String sha256(String s) {
        return INSTANCE.hash(s, "SHA-256");
    }

    public String hash(String s, String hash) {

        MessageDigest digester = null;

        try {
            digester  = MessageDigest.getInstance(hash);
        } catch (NoSuchAlgorithmException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
            return null;
        }
        digester.reset();
        try {
            byte[] bytes = digester.digest(s.getBytes("UTF-8"));

            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
            return null;
        }
    }
}
