package com.kermekx.smartchatting.tasks;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

import com.kermekx.smartchatting.datas.ContactsData;
import com.kermekx.smartchatting.listeners.TaskListener;

/**
 * Created by kermekx on 23/02/2016.
 *
 * This task is to get contacts saved in the internal database ordered by username
 * You should execute UpdateContactsTask if you want to sync the internal database with the server database
 */
public class GetContactsTask extends AsyncTask<Void, Void, Boolean> {

    private final Context mContext;
    private final TaskListener mListener;

    public GetContactsTask(Context context, TaskListener listener) {
        mContext = context;
        mListener = listener;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        Cursor cursor = ContactsData.getContacts(mContext);

        if (cursor.moveToFirst()) {
            do {
                //0 : contact ID
                //1 : username
                //2 : email
                //3 : publicKey
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
            return true;
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
