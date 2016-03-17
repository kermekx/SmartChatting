package com.kermekx.smartchatting.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.kermekx.smartchatting.R;
import com.kermekx.smartchatting.hash.Hasher;
import com.kermekx.smartchatting.json.JsonManager;
import com.kermekx.smartchatting.listeners.TaskListener;

import org.json.JSONObject;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by kermekx on 23/02/2016.
 * <p/>
 * This task is used to get the private key
 */
public class GetPrivateKeyTask extends AsyncTask<Void, Void, Boolean> {

    private final Context mContext;
    private final TaskListener mListener;
    private final String mEmail;
    private final String mPassword;

    public GetPrivateKeyTask(Context context, TaskListener listener, String email, String password) {
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

        String json = JsonManager.getJSON(mContext.getString(R.string.url_get_private_key), values);

        if (json == null) {
            return false;
        }

        try {
            JSONObject result = new JSONObject(json);
            if (result.getBoolean("signed") && result.getBoolean("key")) {
                SharedPreferences settings = mContext.getSharedPreferences(mContext.getString(R.string.preference_file_session), 0);
                String secure = settings.getString("secure", "");

                BigInteger modulus = new BigInteger(result.getString("modulus"));
                BigInteger exponent = new BigInteger(Hasher.aesDecrypt(result.getString("exponent"), secure).replaceAll("[^0-9]", ""));

                /**
                if (mListener != null)
                    mListener.onData(RSA.recreatePrivateKey(exponent, modulus));
                 */

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
