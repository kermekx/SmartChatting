package com.kermekx.smartchatting.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.kermekx.smartchatting.R;
import com.kermekx.smartchatting.hash.Hasher;
import com.kermekx.smartchatting.listeners.LoginListener;
import com.kermekx.smartchatting.listeners.TaskListener;
import com.kermekx.smartchatting.pgp.PGPManager;

import java.io.IOException;
import java.io.PrintWriter;

import javax.net.ssl.SSLSocket;

/**
 * Created by kermekx on 22/02/2016.
 * <p/>
 * This task is used to log into the server
 */
public class LoginTask extends AsyncTask<Void, Void, Boolean> {

    private static final String LOGIN_HEADER = "LOGIN";
    private static final String DISCONNECT_HEADER = "DISCONNECT";
    private static final String END_DATA = "END OF DATA";

    private static final String CONNECTED_DATA = "CONNECTED";
    private static final String CONNECTION_ERROR_DATA = "CONNECTION ERROR";

    private static final String INCORRECT_PIN = "INCORRECT PIN";

    private final Context mContext;
    private final TaskListener mListener;
    private final LoginListener mDataListener;
    private final SSLSocket mSocket;
    private final String mEmail;
    private final String mPassword;
    private final String mPin;
    private final boolean mFirstConnection;

    public LoginTask(Context context, TaskListener listener, LoginListener dataListener, SSLSocket socket, String email, String password, String pin, boolean firstConnection) {
        mContext = context;
        mListener = listener;
        mDataListener = dataListener;
        mSocket = socket;
        mEmail = email;
        mPassword = password;
        mPin = pin;
        mFirstConnection = firstConnection;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            PrintWriter writer = new PrintWriter(mSocket.getOutputStream());

            writer.println(LOGIN_HEADER);
            writer.println(mEmail);
            writer.println((mFirstConnection) ? Hasher.sha256(mPassword) : mPassword);
            writer.println(END_DATA);
            writer.flush();
            writer.close();

            if (mDataListener.data.size() == 0) {
                try {
                    synchronized (mDataListener.data) {
                        mDataListener.data.wait();
                    }
                } catch (InterruptedException e) {

                }
            }

            if (mDataListener.data.size() == 0) {
                if (mListener != null)
                    mListener.onError(CONNECTION_ERROR_DATA);
                return false;
            }

            if (mDataListener.data.get(0).equals(CONNECTED_DATA)) {

                if (mFirstConnection) {

                    if (PGPManager.readPrivateKey(mDataListener.data.get(3), Hasher.md5Byte(mPassword), Hasher.sha256Byte(mPin)) == null) {
                        if (mListener != null)
                            mListener.onError(INCORRECT_PIN);

                        writer = new PrintWriter(mSocket.getOutputStream());
                        writer.println(DISCONNECT_HEADER);
                        writer.println(END_DATA);
                        writer.flush();
                        writer.close();

                        return false;
                    }

                    SharedPreferences settings = mContext.getSharedPreferences(mContext.getString(R.string.preference_file_session), 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.clear();
                    editor.putString("email", mEmail);
                    editor.putString("username", mDataListener.data.get(1));
                    editor.putString("password", Hasher.sha256(mPassword));
                    editor.putString("secure", Hasher.md5(mPassword));
                    editor.putString("pinCheck", Hasher.md5(mPin));
                    editor.putString("publicKey", mDataListener.data.get(2));
                    editor.putString("privateKey", mDataListener.data.get(3));
                    editor.commit();
                }

                return true;
            } else if (mDataListener.data.get(0).equals(CONNECTION_ERROR_DATA)) {
                for (int i = 1; i < mDataListener.data.size(); i++) {
                    if (mListener != null)
                        mListener.onError(mDataListener.data.get(i));
                }

                return false;
            }

            return false;

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
