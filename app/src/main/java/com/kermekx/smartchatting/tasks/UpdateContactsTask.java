package com.kermekx.smartchatting.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.kermekx.smartchatting.datas.ContactsData;
import com.kermekx.smartchatting.listeners.GetContactsListener;
import com.kermekx.smartchatting.listeners.TaskListener;

import java.io.IOException;
import java.io.PrintWriter;

import javax.net.ssl.SSLSocket;

/**
 * Created by kermekx on 23/02/2016.
 * <p/>
 * This task is used to sync contact database from the server
 */
public class UpdateContactsTask extends AsyncTask<Void, Void, Boolean> {

    private static final String GET_CONTACTS_HEADER = "GET CONTACTS";

    private static final String CONTACTS_DATA = "GET CONTACTS";
    private static final String GET_CONTACTS_ERROR_DATA = "GET CONTACTS ERROR";
    private static final String END_DATA = "END OF DATA";

    private static final String CONNECTION_ERROR_DATA = "CONNECTION ERROR";

    private final Context mContext;
    private final TaskListener mListener;
    private final GetContactsListener mDataListener;
    private final SSLSocket mSocket;

    public UpdateContactsTask(Context context, TaskListener listener, GetContactsListener dataListener, SSLSocket socket) {
        mContext = context;
        mListener = listener;
        mDataListener = dataListener;
        mSocket = socket;
    }

    @Override
    protected Boolean doInBackground(Void... params) {

        try {
            PrintWriter writer = new PrintWriter(mSocket.getOutputStream());

            writer.println(GET_CONTACTS_HEADER);
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

            if (mDataListener.data.get(0).equals(CONTACTS_DATA)) {
                mDataListener.data.remove(0);

                ContactsData.removeContacts(mContext);
                for (int i = 0; i < mDataListener.data.size() / 4; i++) {
                    ContactsData.insertContact(mContext, mDataListener.data.get(i * 4), mDataListener.data.get(i * 4 + 1), mDataListener.data.get(i * 4 + 2), mDataListener.data.get(i * 4 + 3));
                }
                return true;
            } else if (mDataListener.data.get(0).equals(GET_CONTACTS_ERROR_DATA)) {
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
