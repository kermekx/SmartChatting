package com.kermekx.smartchatting.commandes;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.kermekx.smartchatting.R;
import com.kermekx.smartchatting.json.JsonManager;
import com.kermekx.smartchatting.listener.SendMessageListener;
import com.kermekx.smartchatting.listener.TaskListener;

import org.json.JSONObject;

import java.io.PrintWriter;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocket;

/**
 * Created by asus on 10/03/2016.
 */
public class SendMessageTask extends AsyncTask<Void, Void, Boolean> {

    private final String mEmail;
    private final String mPassword;
    private final String mUsername;
    private final String mMessage;

    private final Context mContext;
    private final String mBackupMessage;
    private final TaskListener mListener;
    private final SendMessageListener mDataListener;
    private final SSLSocket mSocket;

    public SendMessageTask(Context context, TaskListener taskListen, SendMessageListener messageListener, SSLSocket socket, String username, String message, Key senderPublicKey, Key receiverPublicKey) {
        mContext = context;
        SharedPreferences settings = mContext.getSharedPreferences(mContext.getString(R.string.preference_file_session), 0);
        mEmail = settings.getString("email", "");
        mPassword = settings.getString("password", "");
        mUsername = username;
        mListener = taskListen;
        mDataListener = messageListener;
        mSocket = socket;
        mMessage = message;
        mBackupMessage = message;
        /**
        mMessage = RSA.encrypt(message, receiverPublicKey);
        mBackupMessage = RSA.encrypt(message, senderPublicKey);
         */
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            PrintWriter write = new PrintWriter(String.valueOf(mSocket.getInputStream()));
            write.println();

        } catch (Exception e){

        }
        Map<String, String> values = new HashMap<String, String>();

        values.put("email", mEmail);
        values.put("password", mPassword);
        values.put("username", mUsername);
        values.put("message", mMessage);
        values.put("message_backup", mBackupMessage);

        String json = JsonManager.getJSON(mContext.getString(R.string.url_send_message), values);

        if (json == null) {
            return false;
        }

        try {
            JSONObject result = new JSONObject(json);
            if (result.getBoolean("signed") && result.getBoolean("sent")) {
                return true;
            }
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
            return false;
        }

        return false;

    }

    @Override
    protected void onPostExecute(final Boolean success) {

    }

    @Override
    protected void onCancelled() {
    }
}