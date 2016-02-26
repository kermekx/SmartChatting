package com.kermekx.smartchatting.commandes;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

import com.kermekx.smartchatting.R;
import com.kermekx.smartchatting.datas.MessagesData;
import com.kermekx.smartchatting.json.JsonManager;
import com.kermekx.smartchatting.rsa.RSA;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by kermekx on 23/02/2016.
 *
 * Used to sync messages database from the server
 * Messages are encrypted on the database
 */
public class UpdateMessagesTask extends AsyncTask<Void, Void, Boolean> {

    private final Context mContext;
    private final TaskListener mListener;
    private final String mEmail;
    private final String mPassword;

    public UpdateMessagesTask(Context context, TaskListener listener, String email, String password) {
        mContext = context;
        mListener = listener;
        mEmail = email;
        mPassword = password;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        Map<String, String> values = new HashMap<>();

        values.put("email", mEmail);
        values.put("password", mPassword);

        int max = 0;

        Cursor cursor = MessagesData.getMessages(mContext);
        if (cursor.moveToFirst()) {
            do {
                int id = Integer.parseInt(cursor.getString(0));
                if (id > max)
                    max = id;
            } while (cursor.moveToNext());

            cursor.close();
        }
        values.put("lastMessage", "" + max);

        String json = JsonManager.getJSON(mContext.getString(R.string.url_get_new_messages), values);

        if (json == null) {
            return false;
        }

        try {
            JSONObject result = new JSONObject(json);
            if (result.getBoolean("signed") && result.getBoolean("newMessage")) {
                JSONArray messages = result.getJSONArray("messages");

                for (int i = 0; i < messages.length() / 4; i ++) {
                    MessagesData.insertMessage(mContext, messages.getString(i * 4), messages.getString(i * 4 + 1), messages.getString(i * 4 + 2), messages.getString(i * 4 + 3));
                    if (mListener != null)
                        mListener.onData(new String[]{
                                messages.getString(i * 4),
                                messages.getString(i * 4 + 1),
                                messages.getString(i * 4 + 2),
                                messages.getString(i * 4 + 3)
                        });
                }

                return true;
            }

            return false;
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (mListener != null)
            mListener.onPostExecute(success);
    }

    @Override
    protected void onCancelled() {
        if (mListener != null)
            mListener.onCancelled();
    }
}
