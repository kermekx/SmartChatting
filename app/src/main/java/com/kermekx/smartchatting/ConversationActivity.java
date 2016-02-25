package com.kermekx.smartchatting;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import com.kermekx.smartchatting.conversation.Conversation;
import com.kermekx.smartchatting.conversation.ConversationAdapter;
import com.kermekx.smartchatting.hash.Hasher;
import com.kermekx.smartchatting.icon.IconManager;
import com.kermekx.smartchatting.json.JsonManager;
import com.kermekx.smartchatting.rsa.RSA;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConversationActivity extends AppCompatActivity {

    private String username;
    private Key receiverPublicKey;
    private Key senderPublicKey;
    private Key privateKey;

    private EditText mMessageView;
    private ListView mMessagesView;

    private int lastMessage = 0;
    private ConversationAdapter conversationAdapter;

    private Timer timer = new Timer();
    private TimerTask updateTask = new TimerTask() {
        @Override
        public void run() {
            updateMessages();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();

        setContentView(R.layout.activity_conversation);

        username = extras.getString("username");

        mMessageView = (EditText) findViewById(R.id.typed_message);

        mMessagesView = (ListView) findViewById(R.id.messages);

        ImageView sendView = (ImageView) findViewById(R.id.send_message);

        sendView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        if (conversationAdapter != null) {
            ArrayList<Boolean> isSent = new ArrayList<Boolean>();
            ArrayList<String> messages = new ArrayList<String>();

            for (int i = 0; i < conversationAdapter.getCount(); i ++) {
                Conversation conversation = conversationAdapter.getItem(i);
                isSent.add(conversation.isSent());
                messages.add(conversation.getMessage());
            }

            boolean[] isSentArray = new boolean[isSent.size()];

            for (int i = 0; i < isSent.size(); i ++)
                isSentArray[i] = isSent.get(i);

            outState.putBooleanArray("isSent", isSentArray);
            outState.putStringArrayList("messages", messages);
        }

        outState.putInt("lastMessage", lastMessage);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        timer.cancel();

        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("notify_" + username, true);
        editor.commit();

        super.onPause();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        ArrayList<String> messages = savedInstanceState.getStringArrayList("messages");
        boolean[] isSent = savedInstanceState.getBooleanArray("isSent");

        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
        String curentUsername = settings.getString("username", "");

        lastMessage = savedInstanceState.getInt("lastMessage");

        if (messages != null && isSent != null) {
            List<Conversation> conversations = new ArrayList<Conversation>();

            for (int i = 0; i < messages.size(); i ++) {
                Conversation conversation = new Conversation(isSent[i], null, messages.get(i));
                new LoadContactIconTask((isSent[i]) ? curentUsername : username, conversation).execute();
                conversations.add(conversation);
            }

            conversationAdapter = new ConversationAdapter(ConversationActivity.this, conversations);

            try {
                timer.schedule(updateTask, 2000, 2000);
            } catch (Exception e) {

            }
        }
     }

    @Override
    protected void onResume() {
        super.onResume();

        setTitle(username);

        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
        String user = settings.getString("username", "");

        new GetPublicKeyTask(username, false).execute();
        new GetPublicKeyTask(user, true).execute();

        getMessages();

        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("notify_" + username, false);
        editor.commit();
    }

    @Override
    public void onBackPressed() {
        NavUtils.navigateUpFromSameTask(ConversationActivity.this);
    }

    public void getMessages() {
        if(conversationAdapter == null)
            new GetMessagesTask().execute();
        else
            mMessagesView.setAdapter(conversationAdapter);
    }

    public class GetMessagesTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUsername;
        private final String mCurentUsername;

        private List<LoadContactIconTask> tasks = new ArrayList<LoadContactIconTask>();

        GetMessagesTask() {
            SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
            mCurentUsername = settings.getString("username", "");
            mUsername = username;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
            lastMessage = settings.getInt("lastMessage_" + mUsername, 0);

            int messageSize = settings.getInt("messages_" + mUsername + "_size", 0);

            List<Boolean> isSent = new ArrayList<Boolean>();
            List<String> messages = new ArrayList<String>();

            for (int i = 0; i < messageSize; i ++) {
                messages.add(settings.getString("messages_" + mUsername + "_content_" + i, "Not found!"));
                isSent.add(settings.getBoolean("messages_" + mUsername + "_sent_" + i, true));
            }

            List<Conversation> conversations = new ArrayList<Conversation>();

            for (int i = 0; i < messages.size(); i ++) {
                Conversation conversation = new Conversation(isSent.get(i), null, messages.get(i));
                conversations.add(conversation);
                tasks.add(new LoadContactIconTask(isSent.get(i) ? mCurentUsername : mUsername, conversation));
            }

            conversationAdapter = new ConversationAdapter(ConversationActivity.this, conversations);

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                for (LoadContactIconTask task : tasks)
                    task.execute();

                mMessagesView.setAdapter(conversationAdapter);

                if (mMessagesView.getCount() > 0) {
                    mMessagesView.setSelection(mMessagesView.getCount() - 1);
                }

                try {
                    timer.schedule(updateTask, 1, 2000);
                } catch (Exception e) {

                }
            }
        }

        @Override
        protected void onCancelled() {
        }
    }

    public void updateMessages() {
        new GetNewMessagesTask().execute();
    }

    public class GetNewMessagesTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;
        private final String mUsername;
        private final String mCurentUsername;
        private final String mSecure;

        private List<LoadContactIconTask> tasks = new ArrayList<LoadContactIconTask>();

        GetNewMessagesTask() {
            SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
            mEmail = settings.getString("email", "");
            mPassword = settings.getString("password", "");
            mCurentUsername = settings.getString("username", "");
            mUsername = username;
            mSecure = settings.getString("secure", "");
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Map<String, String> values = new HashMap<String, String>();

            values.put("email", mEmail);
            values.put("password", mPassword);
            values.put("username", mUsername);
            values.put("lastID", "" + lastMessage);

            String json = JsonManager.getJSON(getString(R.string.url_get_new_message_conversation), values);

            if (json == null) {
                return false;
            }

            try {
                JSONObject result = new JSONObject(json);
                if (result.getBoolean("signed")) {

                    if (privateKey == null) {
                        String jsonrsa = JsonManager.getJSON(getString(R.string.url_get_private_key), values);

                        if (jsonrsa == null) {
                            return false;
                        }

                        try {
                            JSONObject resultrsa = new JSONObject(jsonrsa);
                            if (resultrsa.getBoolean("signed") && resultrsa.getBoolean("key")) {

                                BigInteger modulus = new BigInteger(resultrsa.getString("modulus"));
                                BigInteger exponent = new BigInteger(Hasher.aesDecrypt(resultrsa.getString("exponent"), mSecure).replaceAll("[^0-9]", ""));

                                privateKey = RSA.recreatePrivateKey(exponent, modulus);
                            } else {
                                return false;
                            }
                        } catch (Exception e) {
                            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
                            return false;
                        }
                    }

                    JSONArray messages = result.getJSONArray("messages");

                    SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
                    SharedPreferences.Editor editor = settings.edit();

                    int messageID = settings.getInt("messages_" + mUsername + "_size", 0);

                    for (int i = 0; i < messages.length() / 3; i ++) {
                        if (messages.getInt(i * 3 + 1) > lastMessage) {
                            lastMessage = messages.getInt(i * 3 + 1);
                        }

                        String msg = RSA.decrypt(messages.getString(i * 3 + 2), privateKey);

                        final Conversation conversation = new Conversation(messages.getBoolean(i * 3), null, msg);

                        ConversationActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                conversationAdapter.add(conversation);
                            }
                        });

                        tasks.add(new LoadContactIconTask((messages.getBoolean(i * 3)) ? mCurentUsername : mUsername, conversation));

                        editor.putString("messages_" + mUsername + "_content_" + messageID, msg);
                        editor.putBoolean("messages_" + mUsername + "_sent_" + messageID, messages.getBoolean(i * 3));
                        messageID ++;
                    }
                    editor.putInt("messages_" + mUsername + "_size", messageID);
                    editor.putInt("lastMessage_" + mUsername, lastMessage);
                    editor.commit();

                    return true;
                }
            } catch (Exception e) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
                return false;
            }

            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                for (LoadContactIconTask task : tasks)
                    task.execute();

                if (tasks.size() > 0)
                    mMessagesView.setSelection(mMessagesView.getCount() - 1);
            }
        }

        @Override
        protected void onCancelled() {
        }
    }

    public class GetPublicKeyTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;
        private final String mUsername;
        private final boolean isSenderKey;

        GetPublicKeyTask(String username, boolean sender) {
            SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
            mEmail = settings.getString("email", "");
            mPassword = settings.getString("password", "");
            mUsername = username;
            isSenderKey = sender;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Map<String, String> values = new HashMap<String, String>();

            values.put("email", mEmail);
            values.put("password", mPassword);
            values.put("username", mUsername);

            String json = JsonManager.getJSON(getString(R.string.url_get_public_key), values);

            if (json == null) {
                return false;
            }

            try {
                JSONObject result = new JSONObject(json);
                if (result.getBoolean("signed") && result.getBoolean("found") && result.getBoolean("key")) {

                    BigInteger modulus = new BigInteger(result.getString("modulus"));
                    BigInteger exponent = new BigInteger(result.getString("exponent"));

                    if (isSenderKey) {
                        senderPublicKey = RSA.recreatePublicKey(exponent, modulus);
                    } else {
                        receiverPublicKey = RSA.recreatePublicKey(exponent, modulus);
                    }

                    return true;
                }
            } catch (Exception e) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
                return false;
            }

            return false;

        }

        @Override
        protected void onPostExecute(final Boolean success) {
        }

        @Override
        protected void onCancelled() {
        }
    }

    public void sendMessage() {
        String message = mMessageView.getText().toString();

        boolean send = true;

        if (TextUtils.isEmpty(message) || message.length() > 1024) {
            send = false;
        }

        if (senderPublicKey == null) {
            SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
            String user = settings.getString("username", "");

            new GetPublicKeyTask(user, true).execute();

            send = false;
        }

        if (receiverPublicKey == null) {
            new GetPublicKeyTask(username, false).execute();

            send = false;
        }

        if (send) {
            new SendMessageTask(username, message, senderPublicKey, receiverPublicKey).execute();
            mMessageView.setText("");
        }
    }

    public class SendMessageTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;
        private final String mUsername;
        private final String mMessage;;
        private final String mBackupMessage;

        SendMessageTask(String username, String message, Key senderPublicKey, Key receiverPublicKey) {
            SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
            mEmail = settings.getString("email", "");
            mPassword = settings.getString("password", "");
            mUsername = username;

            mMessage = RSA.encrypt(message, receiverPublicKey);
            mBackupMessage = RSA.encrypt(message, senderPublicKey);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Map<String, String> values = new HashMap<String, String>();

            values.put("email", mEmail);
            values.put("password", mPassword);
            values.put("username", mUsername);
            values.put("message", mMessage);
            values.put("message_backup", mBackupMessage);

            String json = JsonManager.getJSON(getString(R.string.url_send_message), values);

            if (json == null) {
                return false;
            }

            try {
                JSONObject result = new JSONObject(json);
                if (result.getBoolean("signed") && result.getBoolean("sent")) {
                    return true;
                }
            } catch (Exception e) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
                return false;
            }

            return false;

        }

        @Override
        protected void onPostExecute(final Boolean success) {

        }

        @Override
        protected void onCancelled() {
        }
    }

    public class LoadContactIconTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUsername;
        private final Object mItem;

        private Bitmap mIcon;

        LoadContactIconTask(String contact, Object i) {
            mUsername = contact;
            mItem = i;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            mIcon = IconManager.getIcon(ConversationActivity.this, mUsername);

            if (mIcon != null)
                return true;

            return false;

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
                Bitmap bitmapResized = Bitmap.createScaledBitmap(mIcon, (int) px, (int) px, false);
                Drawable drawable = new BitmapDrawable(getResources(), bitmapResized);

                if (mItem instanceof Conversation)
                    ((Conversation) mItem).setIcon(drawable);

                conversationAdapter.notifyDataSetChanged();
            }
        }

        @Override
        protected void onCancelled() {

        }
    }
}