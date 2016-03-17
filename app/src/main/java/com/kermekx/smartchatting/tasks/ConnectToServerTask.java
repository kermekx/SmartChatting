package com.kermekx.smartchatting.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;

import com.kermekx.smartchatting.R;
import com.kermekx.smartchatting.listeners.TaskListener;

import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Created by kermekx on 01/03/2016.
 */
public class ConnectToServerTask extends AsyncTask<Void, Void, Boolean> {

    private final Context mContext;
    private final TaskListener mListener;

    public ConnectToServerTask(Context context, TaskListener listener) {
        mContext = context;
        mListener = listener;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            int bks_version;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                bks_version = R.raw.keystore; //The BKS file
            } else {
                bks_version = R.raw.keystorev1; //The BKS (v-1) file
            }

            KeyStore store = KeyStore.getInstance("BKS");
            InputStream in = mContext.getResources().openRawResource(bks_version);
            store.load(in, null);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            tmf.init(store);
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, tmf.getTrustManagers(), new SecureRandom());

            SSLSocketFactory ssf = sslcontext.getSocketFactory();
            Socket sock = ssf.createSocket("51.255.107.101", 26042);
            mListener.onData(sock);

            Logger.getLogger(getClass().getName()).log(Level.INFO, "connected!");

            return true;
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "catch");

            mListener.onError(0);

            return false;
        }
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
