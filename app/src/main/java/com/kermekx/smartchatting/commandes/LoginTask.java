package com.kermekx.smartchatting.commandes;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.kermekx.smartchatting.R;
import com.kermekx.smartchatting.hash.Hasher;
import com.kermekx.smartchatting.json.JsonManager;
import com.kermekx.smartchatting.listener.TaskListener;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kermekx on 22/02/2016.
 *
 * This task is used to log into the server
 */
public class LoginTask extends AsyncTask<Void, Void, Boolean> {

    private final Context mContext;
    private final TaskListener mListener;
    private final String mEmail;
    private final String mPassword;
    private final String mPin;
    private final boolean mHashed;

    public LoginTask(Activity context, TaskListener listener, String email, String password,  String pin, boolean hashed) {
        mContext = context;
        mListener = listener;
        mEmail = email;
        mPassword = password;
        mHashed = hashed;
        mPin = pin;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        boolean shouldExecute = true;

        if (!(mEmail.contains("@") && mEmail.contains(".") && (mEmail.indexOf("@") < mEmail.lastIndexOf(".")))) {
            if (mListener != null)
                mListener.onError(R.string.error_invalid_email);
            shouldExecute = false;
        }

        if (!mHashed && (mPassword.length() < 6 || mPassword.length() > 42)) {
            if (mListener != null)
                mListener.onError(R.string.error_invalid_password);
            shouldExecute = false;
        }

        if (shouldExecute) {
            Map<String, String> values = new HashMap<String, String>();

            values.put("email", mEmail);
            values.put("password", mHashed ? mPassword : Hasher.sha256(mPassword));

            String json = JsonManager.getJSON(mContext.getString(R.string.url_connection), values);

            if (json == null) {
                if (mListener != null)
                    mListener.onError(R.string.error_connection_server);
                return false;
            }

            try {
                JSONObject result = new JSONObject(json);
                if (result.getBoolean("signed")) {
                    if (!mHashed) {
                        SharedPreferences settings = mContext.getSharedPreferences(mContext.getString(R.string.preference_file_session), 0);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("email", mEmail);
                        editor.putString("username", result.getString("username"));
                        editor.putString("password", values.get("password"));
                        editor.putString("secure", Hasher.md5(mPassword));
                        editor.commit();
                    }

                    return true;
                } else if (!result.getBoolean("verified")) {
                    if (mListener != null)
                        mListener.onError(R.string.error_verify_account);
                } else {
                    if (mListener != null)
                        mListener.onError(R.string.error_incorrect_password);
                }
                return false;
            } catch (Exception e) {
                if (mListener != null)
                    mListener.onError(R.string.error_connection_server);
                return false;
            }
        } else {
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
