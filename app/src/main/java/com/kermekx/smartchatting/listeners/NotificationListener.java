package com.kermekx.smartchatting.listeners;

import android.content.Context;
import android.os.AsyncTask;

import com.kermekx.smartchatting.tasks.NotificationTask;
import com.kermekx.smartchatting.datas.MessagesData;
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
            String user = data.get(1);
            String message = data.get(2);

            if (ServerService.getPassword() != null) {
                new NotificationTask(mContext, user, message, ServerService.getPassword()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

            MessagesData.insertMessage(mContext, user, "false", message);
        }
    }
}