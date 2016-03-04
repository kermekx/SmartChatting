package com.kermekx.smartchatting.commandes;

import android.content.Context;
import android.os.AsyncTask;

import com.kermekx.smartchatting.R;
import com.kermekx.smartchatting.datas.ContactsData;
import com.kermekx.smartchatting.json.JsonManager;
import com.kermekx.smartchatting.listener.TaskListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kermekx on 23/02/2016.
 *
 * This task is used to sync contact database from the server
 */
public class UpdateContactsTask extends AsyncTask<Void, Void, Boolean> {

    private final Context mContext;
    private final TaskListener mListener;
    private final String mEmail;
    private final String mPassword;

    public UpdateContactsTask(Context context, TaskListener listener, String email, String password) {
        mContext = context;
        mListener = listener;
        mEmail = email;
        mPassword = password;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        Map<String, String> values = new HashMap<String, String>();

        values.put("email", mEmail);
        values.put("password", mPassword);

        String json = JsonManager.getJSON(mContext.getString(R.string.url_get_contact), values);

        if (json == null) {
            if (mListener != null)
                mListener.onError(R.string.error_connection_server);
            return false;
        }

        try {
            JSONObject result = new JSONObject(json);
            if (result.getBoolean("signed")) {
                JSONArray contacts = result.getJSONArray("contacts");
                ContactsData.removeContacts(mContext);
                for (int i = 0; i < contacts.length() / 5; i ++) {
                    ContactsData.insertContact(mContext, contacts.getString(i * 5), contacts.getString(i * 5 + 1), contacts.getString(i * 5 + 2), contacts.getString(i * 5 + 3), contacts.getString(i * 5 + 4));
                }
                return true;
            }
            if (mListener != null)
                mListener.onError(R.string.error_connection_server);
            return false;
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
