package com.kermekx.smartchatting;

import android.app.FragmentManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.kermekx.smartchatting.commandes.BaseTaskListener;
import com.kermekx.smartchatting.commandes.GetMessagesTask;
import com.kermekx.smartchatting.commandes.GetPrivateKeyTask;
import com.kermekx.smartchatting.commandes.LoadIconTask;
import com.kermekx.smartchatting.commandes.UpdateMessagesTask;
import com.kermekx.smartchatting.conversation.Conversation;
import com.kermekx.smartchatting.conversation.ConversationAdapter;
import com.kermekx.smartchatting.fragment.ConversationFragment;
import com.kermekx.smartchatting.json.JsonManager;
import com.kermekx.smartchatting.listener.TaskListener;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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

    private EditText mMessageView;
    private ListView mMessagesView;
    private ImageView sendView;
    private ImageView mImageView;

    private ConversationFragment fragment;

    private List<LoadIconTask> mTasks = new ArrayList<>();

    private File f;

    private Timer timer = new Timer();
    private TimerTask updateTask = new TimerTask() {
        @Override
        public void run() {
            SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
            TaskListener listener = new UpdateMessagesTaskListener();
            AsyncTask<Void, Void, Boolean> task = new UpdateMessagesTask(ConversationActivity.this, listener, settings.getString("email", ""), settings.getString("password", ""));
            new GetPrivateKeyTask(ConversationActivity.this, new GetPrivateKeyTaskListener(task, listener), settings.getString("email", ""), settings.getString("password", "")).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

        mImageView = (ImageView) findViewById(R.id.viewImage);

        sendView = (ImageView) findViewById(R.id.send_message);

        sendView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
        mMessagesView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // Copier l'item de la ListView des messages
                Conversation conversation = (Conversation) (mMessagesView.getItemAtPosition(position));
                String copie = conversation.getMessage().trim();

                ClipboardManager mClipBoard;
                mClipBoard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

                ClipData mClip;
                mClip = ClipData.newPlainText("Message", copie);
                mClipBoard.setPrimaryClip(mClip);

                // Get l'item copier afin de le coller
                ClipData paste = mClipBoard.getPrimaryClip();
                ClipData.Item item = paste.getItemAt(0);

                Toast.makeText(getApplicationContext(), item.getText().toString() + "Copied", Toast.LENGTH_LONG).show();
                return true;
            }
        });

        try {
            timer.schedule(updateTask, 2000, 2000);
        } catch (Exception e) {

        }
    }

    @Override
    protected void onPause() {

        fragment.setState(mMessagesView.onSaveInstanceState());

        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("notify_" + username, true);
        editor.commit();

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        setTitle(username);

        FragmentManager fm = getFragmentManager();
        fragment = (ConversationFragment) fm.findFragmentByTag("ConversationFragment");

        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);

        if (fragment == null) {
            fragment = new ConversationFragment();
            fm.beginTransaction().add(fragment, "ConversationFragment").commit();

            TaskListener listener = new GetMessagesTaskListener();
            AsyncTask<Void, Void, Boolean> task = new GetMessagesTask(ConversationActivity.this, listener);
            new GetPrivateKeyTask(ConversationActivity.this, new GetPrivateKeyTaskListener(task, listener), settings.getString("email", ""), settings.getString("password", "")).execute();

        } else if (fragment.getConversationAdapter() != null) {
            mMessagesView.setAdapter(fragment.getConversationAdapter());

            mMessagesView.onRestoreInstanceState(fragment.getState());
        }

        String user = settings.getString("username", "");

        new GetPublicKeyTask(username, false).execute();
        new GetPublicKeyTask(user, true).execute();

        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("notify_" + username, false);
        editor.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        timer.cancel();

        for (LoadIconTask task : mTasks)
            task.cancel(true);
    }

    @Override
    public void onBackPressed() {
        NavUtils.navigateUpFromSameTask(ConversationActivity.this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.conversation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_photo) {
            mImageView.setVisibility(View.VISIBLE);
            /*mMessagesView.setVisibility(View.GONE);
            mMessageView.setVisibility(View.GONE);
            sendView.setVisibility(View.GONE);*/

            Context context = getApplicationContext();
            PackageManager packageManager = context.getPackageManager();
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                Toast.makeText(getApplicationContext(), "This device does not have a camera.", Toast.LENGTH_LONG).show();
                return false;
            } else {
                openCamera();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        f = new File(android.os.Environment.getExternalStorageDirectory(), "temp.jpg");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                File f = new File(Environment.getExternalStorageDirectory().toString());
                for (File temp : f.listFiles()) {
                    if (temp.getName().equals("temp.jpg")) {
                        f = temp;
                        break;
                    }
                }
                try {
                    Bitmap bitmap;
                    BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();

                    bitmap = BitmapFactory.decodeFile(f.getAbsolutePath(),
                            bitmapOptions);

                    mImageView.setImageBitmap(bitmap);

                    String path = android.os.Environment
                            .getExternalStorageDirectory()
                            + File.separator
                            + "Phoenix" + File.separator + "default";
                    f.delete();
                    OutputStream outFile = null;
                    File file = new File(path, String.valueOf(System.currentTimeMillis()) + ".jpg");
                    try {
                        outFile = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outFile);
                        outFile.flush();
                        outFile.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (requestCode == 2) {

                Uri selectedImage = data.getData();
                String[] filePath = {MediaStore.Images.Media.DATA};
                Cursor c = getContentResolver().query(selectedImage, filePath, null, null, null);
                c.moveToFirst();
                int columnIndex = c.getColumnIndex(filePath[0]);
                String picturePath = c.getString(columnIndex);
                c.close();
                Bitmap thumbnail = (BitmapFactory.decodeFile(picturePath));
                //Log.w("path of image from gallery", picturePath+"");
                mImageView.setImageBitmap(thumbnail);
            }
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

                    /**
                    if (isSenderKey) {
                        //senderPublicKey = RSA.recreatePublicKey(exponent, modulus);
                    } else {
                        //receiverPublicKey = RSA.recreatePublicKey(exponent, modulus);
                    }
                     */

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
            //new SendMessageTask(username, message, senderPublicKey, receiverPublicKey).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            mMessageView.setText("");
        }
    }

    private class UpdateMessagesTaskListener extends BaseTaskListener {

        private Key mKey;

        private List<Conversation> conversations = new ArrayList<>();
        private List<LoadIconTask> tasks = new ArrayList<>();

        @Override
        public void onError(int error) {

        }

        @Override
        public void onData(Object... object) {
            if (object[0] instanceof Key) {
                mKey = (Key) object[0];
            } else {
                String[] data = (String[]) object;
                if (data[1].equals(username)) {
                    Conversation conversation = new Conversation(Integer.parseInt(data[0]), Boolean.parseBoolean(data[2]), null, getString(R.string.decrypting), data[3]);
                    conversations.add(conversation);
                    tasks.add(new LoadIconTask(ConversationActivity.this, new LoadIconTaskListener(conversation, mKey), data[1], 48));
                }
            }
        }

        @Override
        public void onPostExecute(Boolean success) {
            if (success && conversations.size() > 0) {
                for (Conversation conversation : conversations) {
                    fragment.getConversationAdapter().add(conversation);
                }

                for (LoadIconTask task : tasks) {
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }

                fragment.getConversationAdapter().notifyDataSetChanged();

                if (mMessagesView.getCount() > 0) {
                    mMessagesView.setSelection(mMessagesView.getCount() - 1);
                }
            }
        }

        @Override
        public void onCancelled() {

        }
    }

    private class GetMessagesTaskListener extends BaseTaskListener {

        private Key mKey;

        private List<Conversation> conversations = new ArrayList<>();

        @Override
        public void onError(int error) {

        }

        @Override
        public void onData(Object... object) {
            if (object[0] instanceof Key) {
                mKey = (Key) object[0];
            } else {
                String[] data = (String[]) object;
                if (data[1].equals(username)) {
                    Conversation conversation = new Conversation(Integer.parseInt(data[0]), Boolean.parseBoolean(data[2]), null, getString(R.string.decrypting), data[3]);
                    conversations.add(conversation);
                }
            }
        }

        @Override
        public void onPostExecute(Boolean success) {
            if (success) {
                List<LoadIconTask> tasks = new ArrayList<>();
                Collections.sort(conversations);

                fragment.setConversationAdapter(new ConversationAdapter(ConversationActivity.this, conversations));
                mMessagesView.setAdapter(fragment.getConversationAdapter());

                if (mMessagesView.getCount() > 0) {
                    mMessagesView.setSelection(mMessagesView.getCount() - 1);
                }

                SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
                String user = settings.getString("username", "");

                for (Conversation conversation : conversations)
                    tasks.add(0, new LoadIconTask(ConversationActivity.this, new LoadIconTaskListener(conversation, mKey), conversation.isSent() ? user : username, 48));

                for (LoadIconTask task : tasks)
                    task.execute();

                mTasks = tasks;
            }
        }

        @Override
        public void onCancelled() {

        }
    }

    private class LoadIconTaskListener extends BaseTaskListener {

        private final Object mItem;
        private final Key mKey;

        public LoadIconTaskListener(Object item, Key key) {
            mItem = item;
            mKey = key;
        }

        @Override
        public void onError(int error) {

        }

        @Override
        public void onData(Object... object) {
            if (object instanceof Drawable[]) {
                Drawable drawable = (Drawable) object[0];
                if (mItem instanceof Conversation) {
                    ((Conversation) mItem).setIcon(drawable);
                    //((Conversation) mItem).decrypt(mKey);
                }
            } else {
                if (mItem instanceof Conversation) {
                    //((Conversation) mItem).decrypt(mKey);
                }
            }
        }

        @Override
        public void onPostExecute(Boolean success) {
            fragment.getConversationAdapter().notifyDataSetChanged();
        }

        @Override
        public void onCancelled() {

        }
    }

    private class GetPrivateKeyTaskListener extends BaseTaskListener {

        private final AsyncTask<Void, Void, Boolean> mTask;
        private final TaskListener mListener;

        public GetPrivateKeyTaskListener(AsyncTask<Void, Void, Boolean> task, TaskListener listener) {
            mTask = task;
            mListener = listener;
        }

        @Override
        public void onError(int error) {

        }

        @Override
        public void onData(Object... object) {
            mListener.onData(object);
            mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        @Override
        public void onPostExecute(Boolean success) {
            if (!success) {
                Snackbar.make(mMessagesView, getString(R.string.error_connection_server), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        }

        @Override
        public void onCancelled() {

        }
    }
}