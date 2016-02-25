package com.kermekx.smartchatting.commandes;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.kermekx.smartchatting.R;
import com.kermekx.smartchatting.hash.Hasher;
import com.kermekx.smartchatting.json.JsonManager;
import com.kermekx.smartchatting.rsa.RSA;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kermekx on 22/02/2016.
 */
public class RegisterTask extends AsyncTask<Void, Void, Boolean> {

    private final Context mContext;
    private final TaskListener mListener;
    private final String mEmail;
    private final String mUsername;
    private final String mPassword;

    public RegisterTask(Context context, TaskListener listener, String email, String username, String password) {
        mContext = context;
        mListener = listener;
        mEmail = email;
        mUsername = username;
        mPassword = password;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        boolean shouldExecute = true;

        if (!(mEmail.contains("@") && mEmail.contains(".") && (mEmail.indexOf("@") < mEmail.lastIndexOf(".")))) {
            if (mListener != null)
                mListener.onError(R.string.error_invalid_email);
            shouldExecute = false;
        }

        if (mPassword.length() < 6 || mPassword.length() > 42) {
            if (mListener != null)
                mListener.onError(R.string.error_invalid_password);
            shouldExecute = false;
        }

        if (mUsername.length() == 0) {
            if (mListener != null)
                mListener.onError(R.string.error_field_required);
            shouldExecute = false;
        }

        if (shouldExecute) {
            Map<String, String> values = new HashMap<String, String>();

            values.put("email", mEmail);
            values.put("username", mUsername);
            values.put("password", Hasher.sha256(mPassword));

            String json = JsonManager.getJSON(mContext.getString(R.string.url_register), values);

            if (json == null) {
                if (mListener != null)
                    mListener.onError(R.string.error_connection_server);
                return false;
            }

            try {
                JSONObject result = new JSONObject(json);
                if (result.getBoolean("signed")) {
                    SharedPreferences settings = mContext.getSharedPreferences(mContext.getString(R.string.preference_file_session), 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("email", mEmail);
                    editor.putString("username", mUsername);
                    editor.putString("password", values.get("password"));
                    editor.putString("secure", Hasher.md5(mPassword));
                    editor.commit();

                    try {
                        values = RSA.generateKeys(Hasher.md5(mPassword));
                        values.put("email", mEmail);
                        values.put("password", Hasher.sha256(mPassword));
                        json = JsonManager.getJSON(mContext.getString(R.string.url_set_keys), values);
                    } catch (Exception e) {
                    }

                    return true;
                }

                if (result.getBoolean("emailUnavailable")) {
                    if (mListener != null)
                        mListener.onError(R.string.error_email_already_used);
                }
                if (result.getBoolean("usernameUnavailable")) {
                    if (mListener != null)
                        mListener.onError(R.string.error_username_already_used);
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
