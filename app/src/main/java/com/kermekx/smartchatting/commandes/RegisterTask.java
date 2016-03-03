package com.kermekx.smartchatting.commandes;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.kermekx.smartchatting.R;
import com.kermekx.smartchatting.hash.Hasher;
import com.kermekx.smartchatting.json.JsonManager;
import com.kermekx.smartchatting.rsa.RSA;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLSocket;

/**
 * Created by kermekx on 22/02/2016.
 *
 * This task is used to register into the server
 */
public class RegisterTask extends AsyncTask<Void, Void, Boolean> {

    private static final String REGISTER_HEADER = "REGISTER";
    private static final String END_DATA = "END OF DATA";

    private static final String REGISTERED_DATA = "REGISTERED";
    private static final String REGISTER_ERROR_DATA = "CONNECTION ERROR";

    private final Context mContext;
    private final TaskListener mListener;
    private final SSLSocket mSocket;
    private final String mEmail;
    private final String mUsername;
    private final String mPassword;
    private final String mPublicKey;
    private final String mPrivateKey;

    public RegisterTask(Context context, TaskListener listener, SSLSocket socket, String email, String username, String password, String publicKey, String privateKey) {
        mContext = context;
        mListener = listener;
        mSocket = socket;
        mEmail = email;
        mUsername = username;
        mPassword = password;
        mPublicKey = publicKey;
        mPrivateKey = privateKey;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            PrintWriter writer = new PrintWriter(mSocket.getOutputStream());

            writer.println(REGISTER_HEADER);
            writer.println(mEmail);
            writer.println(mPassword);
            writer.println(mUsername);
            writer.println(mPublicKey);
            writer.println(mPrivateKey);
            writer.println(END_DATA);
            writer.flush();
            writer.close();

            BufferedReader reader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

            String line;

            while ((line = reader.readLine()) != null) {
                switch (line) {
                    case REGISTERED_DATA:
                        while ((line = reader.readLine()) != null && !line.equals(END_DATA)) {
                            if (mListener != null)
                                mListener.onData(line);
                        }
                        return true;
                        break;
                    case REGISTER_ERROR_DATA:
                        while ((line = reader.readLine()) != null && !line.equals(END_DATA)) {
                            if (mListener != null)
                                mListener.onError(line);
                        }
                        return false;
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
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
