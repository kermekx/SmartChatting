package com.kermekx.smartchatting.commandes;

import android.content.Context;
import android.os.AsyncTask;

import com.kermekx.smartchatting.listener.AddContactListener;
import com.kermekx.smartchatting.listener.RemoveContactListener;
import com.kermekx.smartchatting.listener.TaskListener;

import java.io.IOException;
import java.io.PrintWriter;

import javax.net.ssl.SSLSocket;

/**
 * Created by kermekx on 10/03/2016.
 */
public class RemoveContactTask extends AsyncTask<Void, Void, Boolean> {

    private static final String REMOVE_CONTACT_HEADER = "REMOVE CONTACT";

    private static final String CONTACT_REMOVED_DATA = "CONTACT REMOVED";
    private static final String REMOVE_CONTACT_ERROR_DATA = "REMOVE CONTACT ERROR";
    private static final String END_DATA = "END OF DATA";

    private static final String CONNECTION_ERROR_DATA = "CONNECTION ERROR";

    private final Context mContext;
    private final TaskListener mListener;
    private final RemoveContactListener mDataListener;
    private final SSLSocket mSocket;
    private final String mUsername;

    public RemoveContactTask(Context context, TaskListener listener, RemoveContactListener dataListener, SSLSocket socket, String username) {
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

            writer.println(REMOVE_CONTACT_HEADER);
            writer.println(mUsername);
            writer.println(END_DATA);
            writer.flush();
            writer.close();

            if (mDataListener.data.size() == 0) {
                try {
                    synchronized(mDataListener.data) {
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

            if (mDataListener.data.get(0).equals(CONTACT_REMOVED_DATA)) {
                return true;
            } else if (mDataListener.data.get(0).equals(REMOVE_CONTACT_ERROR_DATA)){
                for (int i = 1; i < mDataListener.data.size(); i ++) {
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
