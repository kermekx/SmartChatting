package com.kermekx.smartchatting.schedule;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.kermekx.smartchatting.ConversationActivity;
import com.kermekx.smartchatting.R;
import com.kermekx.smartchatting.commandes.BaseTaskListener;
import com.kermekx.smartchatting.commandes.GetPrivateKeyTask;
import com.kermekx.smartchatting.commandes.UpdateMessagesTask;

import java.security.Key;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kermekx on 14/02/2016.
 *
 * Scheduled task to notify from new message
 */
public class NewMessage extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences settings = context.getSharedPreferences(context.getString(R.string.preference_file_session), 0);
        new GetPrivateKeyTask(context, new GetPrivateKeyTaskListener(context), settings.getString("email", ""), settings.getString("password", "")).execute();
    }

    private class GetPrivateKeyTaskListener extends BaseTaskListener {

        private final Context mContext;

        public GetPrivateKeyTaskListener(Context context) {
            mContext = context;
        }

        @Override
        public void onError(int error) {

        }

        @Override
        public void onData(Object... object) {
            SharedPreferences settings = mContext.getSharedPreferences(mContext.getString(R.string.preference_file_session), 0);
            new UpdateMessagesTask(mContext, new UpdateMessagesTaskListener(mContext, (Key) object[0]), settings.getString("email", ""), settings.getString("password", "")).execute();
        }

        @Override
        public void onPostExecute(Boolean success) {

        }

        @Override
        public void onCancelled() {

        }
    }

    private class UpdateMessagesTaskListener extends BaseTaskListener {

        private final Context mContext;
        private final Key mKey;

        List<String> users = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        public UpdateMessagesTaskListener(Context context, Key key) {
            mContext = context;
            mKey = key;
        }

        @Override
        public void onError(int error) {

        }

        @Override
        public void onData(Object... object) {
            String[] data = (String[]) object;

            if (data[2].equals("false")) {
                NotificationManager mNotificationManager =
                        (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationCompat.Builder builder;

                String user = data[1];
                String message = data[3];//RSA.decrypt(data[3], mKey);

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                    builder = new NotificationCompat.Builder(mContext).setAutoCancel(true).setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE).setColor(mContext.getColor(R.color.primary)).setLights(mContext.getColor(R.color.primary), 300, 1000).setSmallIcon(R.drawable.ic_menu_message).setContentTitle(user).setContentText(message).setGroup("SMART_CHATTING_MESSAGE_KEY").setCategory(Notification.CATEGORY_MESSAGE).setPriority(Notification.PRIORITY_HIGH);
                } else {
                    builder = new NotificationCompat.Builder(mContext).setAutoCancel(true).setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE).setColor(mContext.getResources().getColor(R.color.primary)).setLights(mContext.getResources().getColor(R.color.primary), 300, 1000).setSmallIcon(R.drawable.ic_menu_message).setContentTitle(user).setContentText(message).setGroup("SMART_CHATTING_MESSAGE_KEY").setCategory(Notification.CATEGORY_MESSAGE).setPriority(Notification.PRIORITY_HIGH);
                }

                mNotificationManager.notify("SMART_CHATTING_MESSAGE_KEY".hashCode(), builder.build());

                if (!users.contains(user)) {
                    users.add(user);
                }
                messages.add(message);

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {

                }
            }
        }

        @Override
        public void onPostExecute(Boolean success) {
            if (success && users.size() > 0) {
                String title = "";
                String content = "";

                if (users.size() == 1) {
                    title = users.get(0);

                    for (String message : messages)
                        content += message + '\n';
                } else if (users.size() > 1) {
                    title = users.size() + " " + mContext.getString(R.string.new_messages);

                    for (int i = 0; i < users.size(); i++) {
                        content += users.get(i) + ((i + 1 == users.size()) ? "." : ", ");
                    }
                }

                NotificationManager mNotificationManager =
                        (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationCompat.Builder builder;

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                    builder = new NotificationCompat.Builder(mContext).setAutoCancel(true).setColor(mContext.getColor(R.color.primary)).setLights(mContext.getColor(R.color.primary), 300, 1000).setSmallIcon(R.drawable.ic_menu_message).setContentTitle(title).setContentText(content).setGroup("SMART_CHATTING_MESSAGE_KEY");
                } else {
                    builder = new NotificationCompat.Builder(mContext).setAutoCancel(true).setColor(mContext.getResources().getColor(R.color.primary)).setLights(mContext.getResources().getColor(R.color.primary), 300, 1000).setSmallIcon(R.drawable.ic_menu_message).setContentTitle(title).setContentText(content).setGroup("SMART_CHATTING_MESSAGE_KEY");
                }

                Intent conversationIntent = new Intent(mContext, ConversationActivity.class);
                Bundle extra = new Bundle();
                extra.putString("username", users.get(users.size() - 1));
                conversationIntent.putExtras(extra);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
                    stackBuilder.addParentStack(ConversationActivity.class);
                    stackBuilder.addNextIntent(conversationIntent);

                    PendingIntent conversationPendingIntent =
                            stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                    builder.setContentIntent(conversationPendingIntent);
                }

                mNotificationManager.notify("SMART_CHATTING_MESSAGE_KEY".hashCode(), builder.build());
            }
        }

        @Override
        public void onCancelled() {

        }
    }
}