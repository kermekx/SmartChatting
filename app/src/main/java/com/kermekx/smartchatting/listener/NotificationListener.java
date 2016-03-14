package com.kermekx.smartchatting.listener;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.kermekx.smartchatting.R;
import com.kermekx.smartchatting.commandes.NotificationTask;
import com.kermekx.smartchatting.datas.MessagesData;
import com.kermekx.smartchatting.services.ServerService;

import java.util.List;

/**
 * Created by kermekx on 11/03/2016.
 */
public class NotificationListener extends DataListener {

    private static final String MESSAGE_DATA = "MESSAGE";

    private final Context mContext;
    private final String secretKeyRingBlock;

    public NotificationListener(Context context) {
        mContext = context;

        SharedPreferences settings = mContext.getSharedPreferences(mContext.getString(R.string.preference_file_session), 0);
        secretKeyRingBlock = settings.getString("privateKey", null);
    }

    @Override
    public void onData(Object... object) {
        List<String> data = (List<String>) object[0];

        if (data.get(0).equals(MESSAGE_DATA)) {
            String user = data.get(1);
            String message = data.get(2);

            if (ServerService.getPassword() != null) {
                new NotificationTask(mContext, user, message, secretKeyRingBlock, ServerService.getPassword()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

            MessagesData.insertMessage(mContext, user, "false", message);
        }
    }

}
