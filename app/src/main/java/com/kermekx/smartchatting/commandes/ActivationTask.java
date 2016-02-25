package com.kermekx.smartchatting.commandes;

import android.content.Context;
import android.os.AsyncTask;

import com.kermekx.smartchatting.R;
import com.kermekx.smartchatting.json.JsonManager;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by kermekx on 22/02/2016.
 *
 * This task is used to activate user account using key provided by confirmation mail
 */
public class ActivationTask extends AsyncTask<Void, Void, Boolean> {

    private final Context mContext;
    private final String mUser;
    private final String mKey;

    public ActivationTask(Context context, String user, String key) {
        mContext = context;
        mUser = user;
        mKey = key;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        Map<String, String> values = new HashMap<String, String>();

        values.put("user", mUser);
        values.put("key", mKey);

        String json = JsonManager.getJSON(mContext.getString(R.string.url_activation), values);

        if (json == null) {
            return false;
        }

        try {
            JSONObject result = new JSONObject(json);
            if (result.getBoolean("verified")) {
                return true;
            }
            return false;
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
            return false;
        }
    }

    @Override
    protected void onCancelled() {
        new ActivationTask(mContext, mUser, mKey).execute();
    }
}
