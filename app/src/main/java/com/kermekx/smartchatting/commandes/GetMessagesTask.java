package com.kermekx.smartchatting.commandes;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

import com.kermekx.smartchatting.datas.ContactsData;
import com.kermekx.smartchatting.datas.MessagesData;
import com.kermekx.smartchatting.rsa.RSA;

import java.security.Key;

/**
 * Created by kermekx on 23/02/2016.
 *
 * This task is to get messages from the in the internal database ordered by messageID (sent time too)
 * You should execute UpdateMessagesTask if you want to sync the internal database with the server database
 */
public class GetMessagesTask extends AsyncTask<Void, Void, Boolean> {

    private final Context mContext;
    private final TaskListener mListener;
    private final Key mKey;

    public GetMessagesTask(Context context, TaskListener listener,  Key key) {
        mContext = context;
        mListener = listener;
        mKey = key;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        Cursor cursor = MessagesData.getMessages(mContext);

        if (cursor.moveToFirst()) {
            do {
                mListener.onData(new String[]{
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        RSA.decrypt(cursor.getString(3), mKey)
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
