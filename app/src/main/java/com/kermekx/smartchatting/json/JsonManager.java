package com.kermekx.smartchatting.json;

import com.kermekx.smartchatting.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Kermekx on 29/01/2016.
 */
public class JsonManager {

    private final static JsonManager INSTANCE = new JsonManager();

    public static String getJSON(String sUrl, Map<String, String> values) {
        if (values == null)
            return INSTANCE.getJSON(sUrl, 2000);
        return INSTANCE.getJSON(sUrl, 2000, values);
    }

    private String getJSON(String sUrl, int timeout) {
        HttpsURLConnection connection = null;

        try {
            URL url = new URL(sUrl);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-length", "0");
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
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
                    return result.toString();
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return null;
    }

    private String getJSON(String sUrl, int timeout, Map<String, String> values) {
        HttpsURLConnection connection = null;

        Iterator<String> it = values.keySet().iterator();

        if (it.hasNext())
            sUrl += "?";

        while (it.hasNext()) {
            String next = it.next();
            sUrl += next + "=" + values.get(next);
            if (it.hasNext())
                sUrl += "&";
        }

        try {
            URL url = new URL(sUrl);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-length", "0");
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
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
                    return result.toString();
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return null;
    }
}

