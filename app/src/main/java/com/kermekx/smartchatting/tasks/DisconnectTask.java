package com.kermekx.smartchatting.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.kermekx.smartchatting.R;
import com.kermekx.smartchatting.datas.SmartChattingBdHelper;

import java.io.IOException;
import java.io.PrintWriter;

import javax.net.ssl.SSLSocket;

/**
 * Created by kermekx on 06/03/2016.
 */
public class DisconnectTask extends AsyncTask<Void, Void, Boolean> {

    private static final String DISCONNECT_HEADER = "DISCONNECT";
    private static final String END_DATA = "END OF DATA";

    private final Context mContext;
    private final SSLSocket mSocket;

    public DisconnectTask(Context context, SSLSocket socket) {
        mContext = context;
        mSocket = socket;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            PrintWriter writer = new PrintWriter(mSocket.getOutputStream());

            writer = new PrintWriter(mSocket.getOutputStream());
            writer.println(DISCONNECT_HEADER);
            writer.println(END_DATA);
            writer.flush();
            writer.close();
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            SharedPreferences settings = mContext.getSharedPreferences(mContext.getString(R.string.preference_file_session), 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.clear();
            editor.commit();

            SmartChattingBdHelper.getInstance(mContext).onDowngrade(SmartChattingBdHelper.getInstance(mContext).getWritableDatabase(), 0, 0);

        }
    }
}
