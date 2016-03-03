package com.kermekx.smartchatting.commandes;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocket;

/**
 * Created by kermekx on 01/03/2016.
 */
public class SocketListenerTask extends AsyncTask<Void, Void, Boolean> {

    private final TaskListener mListener;
    private final SSLSocket mSocket;

    private final String MESSAGE_HEADER = "MESSAGE";

    public SocketListenerTask (TaskListener listener, SSLSocket socket) {
        mListener = listener;
        mSocket = socket;
    }

    @Override
    protected Boolean doInBackground(Void... params) {

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

            String line;

            Logger.getLogger(getClass().getName()).log(Level.INFO, "Reading info!");

            while ((line = reader.readLine()) != null) {
                Logger.getLogger(getClass().getName()).log(Level.INFO, line);
                if (line.equals(MESSAGE_HEADER)) {
                    String contact = reader.readLine();
                    Logger.getLogger(getClass().getName()).log(Level.INFO, contact);
                    String message = reader.readLine();
                    Logger.getLogger(getClass().getName()).log(Level.INFO, message);
                    if (mListener != null)
                        mListener.onData(new String[] {contact, message});
                }
            }
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);

        }

        return false;
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
