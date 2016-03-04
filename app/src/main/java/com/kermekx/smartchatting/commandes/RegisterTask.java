package com.kermekx.smartchatting.commandes;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.kermekx.smartchatting.R;
import com.kermekx.smartchatting.hash.Hasher;
import com.kermekx.smartchatting.listener.DataListener;
import com.kermekx.smartchatting.listener.RegisterListener;
import com.kermekx.smartchatting.listener.TaskListener;
import com.kermekx.smartchatting.pgp.KeyGenetor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.net.ssl.SSLSocket;

/**
 * Created by kermekx on 22/02/2016.
 *
 * This task is used to register into the server
 */
public class RegisterTask extends AsyncTask<Void, Void, Boolean> {

    private static final String REGISTER_HEADER = "REGISTER";

    private static final String REGISTERED_DATA = "REGISTERED";
    private static final String REGISTER_ERROR_DATA = "CONNECTION ERROR";
    private static final String END_DATA = "END OF DATA";

    private final Context mContext;
    private final TaskListener mListener;
    private final RegisterListener mDataListener;
    private final SSLSocket mSocket;
    private final String mEmail;
    private final String mUsername;
    private final String mPassword;
    private final String mPin;
    private String mPublicKey;
    private String mPrivateKey;

    public RegisterTask(Context context, TaskListener listener, RegisterListener dataListener, SSLSocket socket, String email, String username, String password, String pin) {
        mContext = context;
        mListener = listener;
        mDataListener = dataListener;
        mSocket = socket;
        mEmail = email;
        mUsername = username;
        mPassword = password;
        mPin = pin;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        ByteArrayOutputStream publicKey = new ByteArrayOutputStream(2048);
        ByteArrayOutputStream privateKey = new ByteArrayOutputStream(4096);

        KeyGenetor.generateKeys(mEmail, mPassword, mPin, publicKey, privateKey);

        mPublicKey = new String(publicKey.toByteArray());
        mPrivateKey = new String(privateKey.toByteArray());

        try {
            PrintWriter writer = new PrintWriter(mSocket.getOutputStream());

            writer.println(REGISTER_HEADER);
            writer.println(mEmail);
            writer.println(Hasher.sha256(mPassword));
            writer.println(mUsername);
            writer.println(mPublicKey);
            writer.println(mPrivateKey);
            writer.println(END_DATA);
            writer.flush();
            writer.close();

            while (mDataListener.data.size() == 0) {
                try {
                    synchronized(mDataListener.data) {
                        mDataListener.data.wait();
                    }
                } catch (InterruptedException e) {

                }
            }

            if (mDataListener.data.get(0).equals(REGISTERED_DATA)) {
                SharedPreferences settings = mContext.getSharedPreferences(mContext.getString(R.string.preference_file_session), 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.clear();
                editor.putString("email", mEmail);
                editor.putString("username", mUsername);
                editor.putString("password", Hasher.sha256(mPassword));
                editor.putString("secure", Hasher.md5(mPassword));
                editor.putString("pinCheck", Hasher.md5(mPin));
                editor.putString("publicKey", mPublicKey);
                editor.putString("privateKey", mPrivateKey);
                editor.commit();

                return true;
            } else {
                for (int i = 1; i < mDataListener.data.size(); i ++) {
                    if (mListener != null)
                        mListener.onError(mDataListener.data.get(i));
                }

                return false;
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
