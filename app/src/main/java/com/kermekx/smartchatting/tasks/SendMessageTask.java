package com.kermekx.smartchatting.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.kermekx.smartchatting.datas.MessagesData;
import com.kermekx.smartchatting.listeners.SendMessageListener;
import com.kermekx.smartchatting.listeners.TaskListener;
import com.kermekx.smartchatting.pgp.PGPManager;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocket;

/**
 * Created by asus on 10/03/2016.
 */
public class SendMessageTask extends AsyncTask<Void, Void, Boolean> {

    private static final String SEND_MESSAGE_HEADER = "SEND MESSAGE";
    private static final String MESSAGE_SENT_DATA = "MESSAGE SENT";
    private static final String SEND_MESSAGE_ERROR_DATA = "SEND MESSAGE ERROR";
    private static final String END_DATA = "END OF DATA";

    private static final String CONNECTION_ERROR_DATA = "CONNECTION ERROR";

    private final Context mContext;
    private final TaskListener mListener;
    private final SendMessageListener mDataListener;
    private final SSLSocket mSocket;
    private final String mUsername;
    private final byte[] mMessage;
    private final String mReceiverPublicKey;
    private final String mSenderPublicKey;

    public SendMessageTask(Context context, TaskListener taskListen, SendMessageListener messageListener, SSLSocket socket, String username, String message, String receiverPublicKey, String senderPublicKey) {
        mContext = context;
        mListener = taskListen;
        mDataListener = messageListener;
        mSocket = socket;
        mUsername = username;
        mMessage = message.getBytes();
        mReceiverPublicKey = receiverPublicKey;
        mSenderPublicKey = senderPublicKey;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            ByteArrayOutputStream backupData = new ByteArrayOutputStream();

            if (!PGPManager.encode(mReceiverPublicKey, mMessage, data) || !PGPManager.encode(mSenderPublicKey, mMessage, backupData)) {
                return false;
            }

            PrintWriter writer = new PrintWriter(mSocket.getOutputStream());

            writer.println(SEND_MESSAGE_HEADER);
            writer.println(mUsername);
            writer.println(new String(data.toByteArray()));
            writer.println(END_DATA);
            writer.flush();
            data.close();
            writer.close();

            byte[] backupMessage = backupData.toByteArray();
            backupData.close();

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

            if (mDataListener.data.get(0).equals(MESSAGE_SENT_DATA)) {
                MessagesData.insertMessage(mContext, mUsername, "true", new String(backupMessage));

                return true;
            } else if (mDataListener.data.get(0).equals(SEND_MESSAGE_ERROR_DATA)) {
                for (int i = 1; i < mDataListener.data.size(); i++) {
                    if (mListener != null)
                        mListener.onError(mDataListener.data.get(i));
                }

                return false;
            }
            if (mListener != null)
                mListener.onError(CONNECTION_ERROR_DATA);

            return false;
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
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