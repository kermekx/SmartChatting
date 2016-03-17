package com.kermekx.smartchatting.tasks;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.kermekx.smartchatting.ConversationActivity;
import com.kermekx.smartchatting.R;
import com.kermekx.smartchatting.pgp.PGPManager;

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

    public NotificationTask(Context context, String username, String message, char[] password) {
        mContext = context;
        mUsername = username;
        mMessage = message;
        SharedPreferences settings = mContext.getSharedPreferences(mContext.getString(R.string.preference_file_session), 0);
        mSecretKeyRingBlock = settings.getString("privateKey", null);
        mPassword = password;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        PGPManager.decode(PGPManager.readSecreteKeyRing(mSecretKeyRingBlock), mPassword, mMessage, data);

        String message = new String(data.toByteArray());

        Bundle extras = new Bundle();
        extras.putString("message", message);

        Intent service = new Intent(ConversationActivity.MESSAGE_RECEIVER + mUsername);
        service.putExtras(extras);

        mContext.sendBroadcast(service);

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
