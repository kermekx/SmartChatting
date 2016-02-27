package com.kermekx.smartchatting.icon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.kermekx.smartchatting.MainActivity;
import com.kermekx.smartchatting.R;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by kermekx on 08/02/2016.
 *
 * Icon manager used to load and cache users icon
 */
public class IconManager {

    private static final IconManager INSTANCE = new IconManager();

    private Map<String, Bitmap> icons = new HashMap<String, Bitmap>();

    public static Bitmap getIcon(Context context, String user) {
        if (INSTANCE.icons.get(user) != null)
            return INSTANCE.icons.get(user);

        Bitmap img = INSTANCE.getIconFromURL(context.getString(R.string.url_icons) + "/" + user.replace(' ', '-') + "/icon.png");

        if (img != null) {
            INSTANCE.icons.put(user, img);
            return img;
        }

        return null;
    }

    private Bitmap getIconFromURL(String url) {
        HttpURLConnection connection = null;
        try {
            Bitmap tmp;

            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(2000);
            connection.connect();
            InputStream input = connection.getInputStream();

            tmp = BitmapFactory.decodeStream(input);

            connection.disconnect();
            input.close();

            return tmp;
        } catch (MalformedURLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            if (!(ex instanceof FileNotFoundException))
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
