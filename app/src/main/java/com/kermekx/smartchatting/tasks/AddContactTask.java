package com.kermekx.smartchatting.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.kermekx.smartchatting.listeners.AddContactListener;
import com.kermekx.smartchatting.listeners.TaskListener;

import java.io.IOException;
import java.io.PrintWriter;

import javax.net.ssl.SSLSocket;

/**
 * Created by kermekx on 25/02/2016.
 * <p/>
 * This task is used to add a new contact
 */
public class AddContactTask extends AsyncTask<Void, Void, Boolean> {

    private static final String ADD_CONTACT_HEADER = "ADD CONTACT";

    private static final String CONTACT_ADDED_DATA = "CONTACT ADDED";
    private static final String ADD_CONTACT_ERROR_DATA = "ADD CONTACT ERROR";
    private static final String END_DATA = "END OF DATA";

    private static final String CONNECTION_ERROR_DATA = "CONNECTION ERROR";

    private final Context mContext;
    private final TaskListener mListener;
    private final AddContactListener mDataListener;
    private final SSLSocket mSocket;
    private final String mUsername;

    public AddContactTask(Context context, TaskListener listener, AddContactListener dataListener, SSLSocket socket, String username) {
        mContext = context;
        mListener = listener;
        mDataListener = dataListener;
        mSocket = socket;
        mUsername = username;
    }

    @Override
    protected Boolean doInBackground(Void... params) {

        try {
            PrintWriter writer = new PrintWriter(mSocket.getOutputStream());

            writer.println(ADD_CONTACT_HEADER);
            writer.println(mUsername);
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

            if (mDataListener.data.get(0).equals(CONTACT_ADDED_DATA)) {
                return true;
            } else if (mDataListener.data.get(0).equals(ADD_CONTACT_ERROR_DATA)) {
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
