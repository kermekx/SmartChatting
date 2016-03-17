package com.kermekx.smartchatting.tasks;

import android.os.AsyncTask;

import com.kermekx.smartchatting.listeners.TaskListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocket;

/**
 * Created by kermekx on 01/03/2016.
 */
public class SocketListenerTask extends AsyncTask<Void, Void, Boolean> {

    private final TaskListener mListener;
    private final SSLSocket mSocket;

    private static final String END_DATA = "END OF DATA";

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

            List<String> data = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                Logger.getLogger(getClass().getName()).log(Level.INFO, line);
                if (line.equals(END_DATA)) {
                    if (mListener != null)
                        mListener.onData(data);
                    data = new ArrayList<>();
                } else {
                    data.add(line);
                }
            }
            Logger.getLogger(getClass().getName()).log(Level.INFO, "read : " + data);
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);

        }

        return false;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        Logger.getLogger(getClass().getName()).log(Level.WARNING, "End of connection");
        if (mListener != null)
            mListener.onPostExecute(success);
    }

    @Override
    protected void onCancelled() {
        Logger.getLogger(getClass().getName()).log(Level.WARNING, "connection cancelled");

        if (mListener != null)
            mListener.onCancelled();
    }
}
