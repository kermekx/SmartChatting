package com.kermekx.smartchatting.json;

import android.os.Build;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Kermekx on 29/01/2016.
 */
public class JsonManager {

    /**
     * Se connecte au serveur pour effectuer une requete (POST avec des donnée, GET sinon)
     * @param sUrl URL (HTTPS uniquement)
     * @param values Liste des donnée ( ?key1=value1&key2=value2)
     * @return JSON ou erreur
     */
    public static String getJSON(String sUrl, Map<String, String> values) {
        if (values == null)
            return getJSON(sUrl, 2000);
        return getJSON(sUrl, 2000, values);
    }

    /**
     * Se connecte au serveur pour effectuer une requete GET sans donnees
     * @param sUrl URL (HTTPS uniquement)
     * @param timeout temps max de la requete en millisecondes
     * @return JSON ou erreur
     */
    private static String getJSON(String sUrl, int timeout) {
        HttpsURLConnection connection = null;

        try {
            URL url = new URL(sUrl);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.connect();
            int status = connection.getResponseCode();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        result.append(line+"\n");
                    }
                    br.close();
                    connection.disconnect();
                    return result.toString();
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(JsonManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(JsonManager.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception ex) {
                    Logger.getLogger(JsonManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return null;
    }

    /**
     *Se connecte au serveur pour effectuer une requete POST avec des donnée
     * @param sUrl URL (HTTPS uniquement)
     * @param timeout temps max de la requete en millisecondes
     * @param values Liste des donnée ( ?key1=value1&key2=value2)
     * @return JSON ou erreur
     */
    private static String getJSON(String sUrl, int timeout, Map<String, String> values) {
        HttpsURLConnection connection = null;

        String charset = "UTF-8";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            charset = StandardCharsets.UTF_8.name();
        }

        String query = "";

        Iterator<String> it = values.keySet().iterator();

        while (it.hasNext()) {
            String key = it.next();
            query += key + "=" + values.get(key) + (it.hasNext() ? "&" : "");
        }

        try {
            URL url = new URL(sUrl);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Accept-Charset", charset);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);

            try {
                OutputStream outuput = connection.getOutputStream();
                outuput.write(query.getBytes(charset));
            } catch (IOException ex) {
                return null;
            }

            InputStream response = connection.getInputStream();

            int status = connection.getResponseCode();

            switch (status) {
                case 200:
                case 201:

                    BufferedReader br = new BufferedReader(new InputStreamReader(response));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        result.append(line+"\n");
                    }
                    br.close();
                    return result.toString();
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(JsonManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnknownHostException ex) {
            //Cannot connect to server
        } catch (IOException ex) {
            Logger.getLogger(JsonManager.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception ex) {
                    Logger.getLogger(JsonManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return null;
    }
}

