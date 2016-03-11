package com.kermekx.smartchatting.listener;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.kermekx.smartchatting.R;
import com.kermekx.smartchatting.services.ServerService;

import java.util.List;

/**
 * Created by kermekx on 11/03/2016.
 */
public class NotificationListener extends DataListener {

    private static final String MESSAGE_DATA = "MESSAGE";

    private final Context mContext;

    public NotificationListener(Context context) {
        mContext = context;
    }

    @Override
    public void onData(Object... object) {
        List<String> data = (List<String>) object[0];

        if (data.get(0).equals(MESSAGE_DATA)) {
            NotificationManager mNotificationManager =
                    (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder;
            String user = data.get(1);
            String message = data.get(2);

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                builder = new NotificationCompat.Builder(mContext).setAutoCancel(true).setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE).setColor(mContext.getColor(R.color.primary)).setLights(mContext.getColor(R.color.primary), 300, 1000).setSmallIcon(R.drawable.ic_menu_message).setContentTitle(user).setContentText(message).setGroup("SMART_CHATTING_MESSAGE_KEY").setCategory(Notification.CATEGORY_MESSAGE).setPriority(Notification.PRIORITY_HIGH);
            } else {
                builder = new NotificationCompat.Builder(mContext).setAutoCancel(true).setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE).setColor(mContext.getResources().getColor(R.color.primary)).setLights(mContext.getResources().getColor(R.color.primary), 300, 1000).setSmallIcon(R.drawable.ic_menu_message).setContentTitle(user).setContentText(message).setGroup("SMART_CHATTING_MESSAGE_KEY").setCategory(Notification.CATEGORY_MESSAGE).setPriority(Notification.PRIORITY_HIGH);
            }

            mNotificationManager.notify("SMART_CHATTING_MESSAGE_KEY".hashCode(), builder.build());

        }
    }

}
