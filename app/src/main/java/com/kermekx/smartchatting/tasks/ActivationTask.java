package com.kermekx.smartchatting.tasks;

import android.content.Context;
import android.os.AsyncTask;

/**
 * Created by kermekx on 22/02/2016.
 * <p/>
 * This task is used to activate user account using key provided by confirmation mail
 */
public class ActivationTask extends AsyncTask<Void, Void, Boolean> {

    private final Context mContext;

    public ActivationTask(Context context) {
        mContext = context;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        //TODO : activate account
        return false;
    }
}
