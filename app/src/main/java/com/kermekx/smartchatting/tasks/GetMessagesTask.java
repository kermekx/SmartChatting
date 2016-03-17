package com.kermekx.smartchatting.tasks;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

import com.kermekx.smartchatting.datas.MessagesData;
import com.kermekx.smartchatting.listeners.TaskListener;

/**
 * Created by kermekx on 23/02/2016.
 * <p/>
 * This task is to get messages from the in the internal database ordered by messageID (sent time too)
 * You should execute UpdateMessagesTask if you want to sync the internal database with the server database
 */
public class GetMessagesTask extends AsyncTask<Void, Void, Boolean> {

    private final Context mContext;
    private final TaskListener mListener;

    public GetMessagesTask(Context context, TaskListener listener) {
        mContext = context;
        mListener = listener;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        Cursor cursor = MessagesData.getMessages(mContext);

        if (cursor.moveToFirst()) {
            do {
                //0 : Message ID
                //1 : Contact name
                //2 : is sent
                //3 : message
                mListener.onData(new String[]{
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3)
                });
            } while (cursor.moveToNext());

            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }
    }

    @Override
    protected void onPostExecute(final Boolean success) {
        if (mListener != null)
            mListener.onPostExecute(success);
    }

    @Override
    protected void onCancelled() {
        if (mListener != null)
            mListener.onCancelled();
    }
}
