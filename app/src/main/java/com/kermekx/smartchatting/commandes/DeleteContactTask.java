package com.kermekx.smartchatting.commandes;

import android.content.Context;
import android.os.AsyncTask;

import com.kermekx.smartchatting.R;
import com.kermekx.smartchatting.datas.ContactsData;
import com.kermekx.smartchatting.json.JsonManager;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by kermekx on 24/02/2016.
 *
 * This task is used to delete a contact
 */
public class DeleteContactTask extends AsyncTask<Void, Void, Boolean> {

    private final Context mContext;
    private final TaskListener mListener;
    private final String mEmail;
    private final String mPassword;
    private final String mUsername;

    public DeleteContactTask(Context context, TaskListener listener, String email, String password, String username) {
        mContext = context;
        mListener = listener;
        mEmail = email;
        mPassword = password;
        mUsername = username;
    }

    @Override
    protected Boolean doInBackground(Void... params) {

        Map<String, String> values = new HashMap<String, String>();

        values.put("email", mEmail);
        values.put("password", mPassword);
        values.put("username", mUsername);

        String json = JsonManager.getJSON(mContext.getString(R.string.url_remove_contact), values);

        if (json == null) {
            if (mListener != null)
                mListener.onError(R.string.error_connection_server);
            return false;
        }

        try {
            JSONObject result = new JSONObject(json);
            if (result.getBoolean("signed") && !result.getBoolean("notFriend")) {
                ContactsData.removeContact(mContext, mUsername);
                return true;
            } else {
                if (mListener != null)
                    mListener.onError(R.string.error_not_found);
                return false;
            }
        } catch (Exception e) {
            if (mListener != null)
                mListener.onError(R.string.error_connection_server);
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
