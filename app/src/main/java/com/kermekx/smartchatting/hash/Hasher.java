package com.kermekx.smartchatting.hash;

import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by kermekx on 30/01/2016.
 *
 * Hashing utilities
 */
public class Hasher {

    private final static Hasher INSTANCE = new Hasher();

    public static String sha256(String s) {
        return INSTANCE.hash(s, "SHA-256");
    }

    public static String md5(String s) {
        return INSTANCE.hash(s, "MD5");
    }

    public static byte[] sha256Byte(String s) {
        return INSTANCE.hashByte(s, "SHA-256");
    }

    public static byte[] md5Byte(String s) {
        return INSTANCE.hashByte(s, "MD5");
    }

    public static String aesEncrypt(String s, String secret) {
        return INSTANCE.encrypt(s, secret, "AES");
    }

    public static String aesDecrypt(String s, String secret) {
        return INSTANCE.decrypt(s, secret, "AES");
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

    public byte[] hashByte(String s, String hash) {

        MessageDigest digester = null;

        try {
            digester  = MessageDigest.getInstance(hash);
        } catch (NoSuchAlgorithmException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
            return null;
        }
        digester.reset();
        try {
            return digester.digest(s.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
            return null;
        }
    }

    public  String encrypt(String s, String secret, String crypt) {
        while (s.length() % 16 != 0) {
            s += " ";
        }
        try {
            Key key = new SecretKeySpec(secret.getBytes(), crypt);
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(s.getBytes());
            return Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        } catch (NoSuchPaddingException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        } catch (InvalidKeyException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        } catch (IllegalBlockSizeException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        } catch (BadPaddingException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        }
        return null;
    }

    public  String decrypt(String s, String secret, String crypt) {
        try {
            Key key = new SecretKeySpec(secret.getBytes(), crypt);
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(Base64.decode(s.replace(" ", "+"), Base64.NO_WRAP));
            return new String(decrypted);
        } catch (NoSuchAlgorithmException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        } catch (NoSuchPaddingException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        } catch (InvalidKeyException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        } catch (IllegalBlockSizeException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        } catch (BadPaddingException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        }
        return null;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
