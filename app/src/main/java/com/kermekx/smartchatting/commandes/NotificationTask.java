package com.kermekx.smartchatting.commandes;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.kermekx.smartchatting.R;
import com.kermekx.smartchatting.pgp.KeyManager;

import java.io.ByteArrayOutputStream;

/**
 * Created by kermekx on 14/03/2016.
 */
public class NotificationTask extends AsyncTask<Void, Void, Boolean> {

    private final Context mContext;
    private final String mUsername;
    private final String mMessage;
    private final String mSecretKeyRingBlock;
    private final char[] mPassword;

    public NotificationTask(Context context, String username, String message, String secretKeyRingBlock, char[] password) {
        mContext = context;
        mUsername = username;
        mMessage = message;
        mSecretKeyRingBlock = secretKeyRingBlock;
        mPassword = password;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        KeyManager.decode(KeyManager.readSecreteKeyRing(mSecretKeyRingBlock), mPassword, mMessage, data);

        String message = new String(data.toByteArray());

        NotificationManager mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder;

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            builder = new NotificationCompat.Builder(mContext).setAutoCancel(true).setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE).setColor(mContext.getColor(R.color.primary)).setLights(mContext.getColor(R.color.primary), 300, 1000).setSmallIcon(R.drawable.ic_menu_message).setContentTitle(mUsername).setContentText(message).setGroup("SMART_CHATTING_MESSAGE_KEY").setCategory(Notification.CATEGORY_MESSAGE).setPriority(Notification.PRIORITY_HIGH);
        } else {
            builder = new NotificationCompat.Builder(mContext).setAutoCancel(true).setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE).setColor(mContext.getResources().getColor(R.color.primary)).setLights(mContext.getResources().getColor(R.color.primary), 300, 1000).setSmallIcon(R.drawable.ic_menu_message).setContentTitle(mUsername).setContentText(message).setGroup("SMART_CHATTING_MESSAGE_KEY").setCategory(Notification.CATEGORY_MESSAGE).setPriority(Notification.PRIORITY_HIGH);
        }

        mNotificationManager.notify("SMART_CHATTING_MESSAGE_KEY".hashCode(), builder.build());

        return true;
    }
}
