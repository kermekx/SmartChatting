package com.kermekx.smartchatting.rsa;

import android.util.Base64;

import com.kermekx.smartchatting.hash.Hasher;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by kermekx on 11/02/2016.
 */
public class RSA {

    /**
     * Genere une paire de clefs RSA de 2048 bits
     * et prepare les données pour les envoyer au serveur
     * tout en encryptant la clef privée
     * @param secure le mot de passe crypter en md5
     * @return les parametre dans une map pour les envoyer au serveur
     */
    public static Map<String, String> generateKeys(String secure) {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);

            KeyPair keyPair = keyPairGenerator.genKeyPair();
            Key publicKey = keyPair.getPublic();
            Key privateKey = keyPair.getPrivate();

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKeySpec rsaPublicKeySpec = keyFactory.getKeySpec(publicKey, RSAPublicKeySpec.class);

            BigInteger modulus = rsaPublicKeySpec.getModulus();
            BigInteger publicExponent = rsaPublicKeySpec.getPublicExponent();

            RSAPrivateKeySpec rsaPrivateKeySpec = keyFactory.getKeySpec(privateKey, RSAPrivateKeySpec.class);

            BigInteger privateExponent = rsaPrivateKeySpec.getPrivateExponent();

            Map<String, String> values = new HashMap<String, String>();
            values.put("modulus", modulus.toString());
            values.put("publicExponent", publicExponent.toString());
            values.put("privateExponent", Hasher.aesEncrypt(privateExponent.toString(), secure));

            return values;

        } catch (NoSuchAlgorithmException e) {

        } catch (InvalidKeySpecException e) {

        }

        return null;
    }

    /**
     * recreer une clef public a partir du modulo et de l'exposant public
     * @param exponent exposant public
     * @param modulus modulo
     * @return clef public
     */
    public static Key recreatePublicKey(BigInteger exponent, BigInteger modulus) {

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(modulus, exponent);
            return keyFactory.generatePublic(rsaPublicKeySpec);
        } catch (NoSuchAlgorithmException e) {

        } catch (InvalidKeySpecException e) {

        }

        return null;
    }

    /**
     * recreer une clef privee a partir du modulo et de l'exposant privee
     * @param exponent exposant privee
     * @param modulus modulo
     * @return clef privee
     */
    public static Key recreatePrivateKey(BigInteger exponent, BigInteger modulus) {

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPrivateKeySpec rsaPrivateKeySpec = new RSAPrivateKeySpec(modulus, exponent);
            return keyFactory.generatePrivate(rsaPrivateKeySpec);
        } catch (NoSuchAlgorithmException e) {

        } catch (InvalidKeySpecException e) {

        }

        return null;
    }

    /**
     * chiffre un message avec une clef public et l'encode en base 64
     * @param message message a chiffrer
     * @param publicKey clef public
     * @return message chiffre en base 64
     */
    public static String encrypt(String message, Key publicKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encrypted = cipher.doFinal(message.getBytes());
            return Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (NoSuchPaddingException e) {
            Logger.getLogger(RSA.class.getName()).log(Level.SEVERE, null, e);
        } catch (NoSuchAlgorithmException e) {
            Logger.getLogger(RSA.class.getName()).log(Level.SEVERE, null, e);
        } catch (InvalidKeyException e) {
            Logger.getLogger(RSA.class.getName()).log(Level.SEVERE, null, e);
        } catch (IllegalBlockSizeException e) {
            Logger.getLogger(RSA.class.getName()).log(Level.SEVERE, null, e);
        } catch (BadPaddingException e) {
            Logger.getLogger(RSA.class.getName()).log(Level.SEVERE, null, e);
        }

        return null;
    }

    /**
     * dechiffre un message chiffre avec la clef privee
     * @param message message chiffre  en base 64
     * @param privateKey clef privee
     * @return message dechiffre
     */
    public static String decrypt(String message, Key privateKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decrypted = cipher.doFinal(Base64.decode(message.replace(" ", "+"), Base64.NO_WRAP));
            return new String(decrypted);
        } catch (NoSuchPaddingException e) {
            Logger.getLogger(RSA.class.getName()).log(Level.SEVERE, null, e);
        } catch (NoSuchAlgorithmException e) {
            Logger.getLogger(RSA.class.getName()).log(Level.SEVERE, null, e);
        } catch (InvalidKeyException e) {
            Logger.getLogger(RSA.class.getName()).log(Level.SEVERE, null, e);
        } catch (IllegalBlockSizeException e) {
            Logger.getLogger(RSA.class.getName()).log(Level.SEVERE, null, e);
        } catch (BadPaddingException e) {
            Logger.getLogger(RSA.class.getName()).log(Level.SEVERE, null, e);
        }

        return null;
    }
}
