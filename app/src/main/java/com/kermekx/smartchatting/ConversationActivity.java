package com.kermekx.smartchatting;

import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.kermekx.smartchatting.tasks.BaseTaskListener;
import com.kermekx.smartchatting.tasks.GetMessagesTask;
import com.kermekx.smartchatting.conversation.Conversation;
import com.kermekx.smartchatting.conversation.ConversationAdapter;
import com.kermekx.smartchatting.fragment.ConversationFragment;
import com.kermekx.smartchatting.pgp.PGPManager;
import com.kermekx.smartchatting.services.ServerService;

import org.bouncycastle.openpgp.PGPSecretKeyRing;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConversationActivity extends AppCompatActivity {

    private static final String HEADER_SEND_MESSAGE = "SEND MESSAGE DATA";

    private String username;
    private String receiverPublicKeyBlock;
    private String senderPublicKeyBlock;

    private EditText mMessageView;
    private ListView mMessagesView;
    private ImageView sendView;
    private ImageView mImageView;

    private String secretKeyRingBlock;

    private ConversationFragment fragment;

    private File f;

    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

    private static final String SEND_MESSAGE_RECEIVER = "SEND_MESSAGE_RECEIVER";
    private BroadcastReceiver sendMessageReceiver;

    public static final String MESSAGE_RECEIVER = "MESSAGE_RECEIVER_";
    private BroadcastReceiver messageReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();

        setContentView(R.layout.activity_conversation);

        username = extras.getString("username");
        receiverPublicKeyBlock = extras.getString("publicKey");

        mMessageView = (EditText) findViewById(R.id.typed_message);

        mMessagesView = (ListView) findViewById(R.id.messages);

        mImageView = (ImageView) findViewById(R.id.viewImage);

        sendView = (ImageView) findViewById(R.id.send_message);

        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
        secretKeyRingBlock = settings.getString("privateKey", null);

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
    }

    @Override
    protected void onPause() {
        fragment.setState(mMessagesView.onSaveInstanceState());

        unregisterReceiver(sendMessageReceiver);
        unregisterReceiver(messageReceiver);

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

            new GetMessagesTask(this, new GetMessagesTaskListener()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else if (fragment.getConversationAdapter() != null) {
            mMessagesView.setAdapter(fragment.getConversationAdapter());

            mMessagesView.onRestoreInstanceState(fragment.getState());
        }

        sendMessageReceiver = new SendMessageReceiver();
        registerReceiver(sendMessageReceiver, new IntentFilter(SEND_MESSAGE_RECEIVER));

        messageReceiver = new MessageReceiver();
        registerReceiver(messageReceiver, new IntentFilter(MESSAGE_RECEIVER + username));

        senderPublicKeyBlock = settings.getString("publicKey", "");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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

            if (Build.VERSION.SDK_INT >= 23) {
                String permission = android.Manifest.permission.CAMERA;
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(ConversationActivity.this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_CODE_ASK_PERMISSIONS);
                } else {
                    Context context = getApplicationContext();
                    PackageManager packageManager = context.getPackageManager();
                    if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                        Toast.makeText(getApplicationContext(), "This device does not have a camera.", Toast.LENGTH_LONG).show();
                        return false;
                    } else {
                        openCamera();
                    }
                }
            } else {
                Context context = getApplicationContext();
                PackageManager packageManager = context.getPackageManager();
                if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                    Toast.makeText(getApplicationContext(), "This device does not have a camera.", Toast.LENGTH_LONG).show();
                    return false;
                } else {
                    openCamera();
                }
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(getApplication(), "Permission required", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (Build.VERSION.SDK_INT >= 23) {
            String permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(ConversationActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_ASK_PERMISSIONS);
            } else {
                f = new File(android.os.Environment.getExternalStorageDirectory(), "temp.jpg");
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                startActivityForResult(intent, 1);
            }
        }
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
                    if (mImageView != null) {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;

                        BitmapFactory.decodeFile(f.getAbsolutePath(), options);
                        int imageHeight = options.outHeight;
                        int imageWidth = options.outWidth;
                        float reqSizes = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mImageView.getHeight(), getResources().getDisplayMetrics());
                        int inSampleSize = 1;

                        if (imageHeight > reqSizes || imageWidth > reqSizes) {
                            while ((imageHeight / inSampleSize) > reqSizes
                                    && (imageWidth / inSampleSize) > reqSizes) {
                                inSampleSize *= 2;
                            }
                        }

                        options = new BitmapFactory.Options();
                        options.inSampleSize = inSampleSize;
                        options.inJustDecodeBounds = false;

                        mImageView.setImageBitmap(BitmapFactory.decodeFile(f.getAbsolutePath(), options));

                    }
                    f.delete();
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
                mImageView.setImageBitmap(thumbnail);
            }
        }
    }

    public void sendMessage() {
        String message = mMessageView.getText().toString();

        boolean send = true;

        if (TextUtils.isEmpty(message.trim()) || message.length() > 1024) {
            send = false;
        }

        if (senderPublicKeyBlock == null || receiverPublicKeyBlock == null) {
            Snackbar.make(mMessagesView, "Cannot send message without public keys!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            send = false;
        }

        if (send) {
            Bundle extras = new Bundle();

            extras.putString("header", HEADER_SEND_MESSAGE);
            extras.putString("filter", SEND_MESSAGE_RECEIVER);
            extras.putString("username", username);
            extras.putString("message", "M" + message.trim());
            extras.putSerializable("senderPublicKey", senderPublicKeyBlock);
            extras.putSerializable("receiverPublicKey", receiverPublicKeyBlock);

            Intent service = new Intent(ServerService.SERVER_RECEIVER);
            service.putExtras(extras);

            sendBroadcast(service);

            mMessageView.setText("");
        }
    }

    private class GetMessagesTaskListener extends BaseTaskListener {

        private List<Conversation> conversations = new ArrayList<>();

        @Override
        public void onError(int error) {

        }

        @Override
        public void onData(Object... object) {
            String[] data = (String[]) object;
            if (data[1].equals(username)) {
                Conversation conversation = new Conversation(Integer.parseInt(data[0]), Boolean.parseBoolean(data[2]), null, getString(R.string.decrypting), data[3]);
                conversations.add(conversation);
            }
        }

        @Override
        public void onPostExecute(Boolean success) {
            if (success) {
                fragment.setConversationAdapter(new ConversationAdapter(ConversationActivity.this, conversations));
                mMessagesView.setAdapter(fragment.getConversationAdapter());

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE);
                        PGPSecretKeyRing secretKeyRing = PGPManager.readSecreteKeyRing(secretKeyRingBlock);

                        for (int i = conversations.size() - 1; i >= 0; i--) {
                            ByteArrayOutputStream data = new ByteArrayOutputStream();

                            PGPManager.decode(secretKeyRing, ServerService.getPassword(), conversations.get(i).getCryptedMessage(), data);

                            byte[] mes = data.toByteArray();

                            if (mes[0] == 'M') {
                                conversations.get(i).setMessage(new String(data.toByteArray(), 1, mes.length - 1));

                                ConversationActivity.this.runOnUiThread(new Runnable() {
                                    public void run() {
                                        fragment.getConversationAdapter().notifyDataSetChanged();
                                    }
                                });
                            }
                        }
                    }
                }).start();
            }

            if (conversations.size() > 0) {
                mMessagesView.setSelection(mMessagesView.getCount() - 1);
            }
        }

        @Override
        public void onCancelled() {

        }
    }

    public class SendMessageReceiver extends BroadcastReceiver {

        private static final String USER_NOT_FOUND_ERROR = "USER NOT FOUND";
        private static final String INTERNAL_SERVER_ERROR = "INTERNAL ERROR";
        private static final String CONNECTION_ERROR_DATA = "CONNECTION ERROR";

        @Override
        public void onReceive(Context context, Intent intent) {
            Boolean connected = intent.getExtras().getBoolean("connected");

            if (connected) {
                Boolean success = intent.getExtras().getBoolean("success");

                if (success) {
                    fragment.getConversationAdapter().add(new Conversation(0, true, null, intent.getExtras().getString("message").substring(1), null));
                    fragment.getConversationAdapter().notifyDataSetChanged();
                    mMessagesView.setSelection(mMessagesView.getCount() - 1);
                } else {
                    ArrayList<String> errors = intent.getExtras().getStringArrayList("errors");

                    for (String error : errors) {
                        switch (error) {
                            case USER_NOT_FOUND_ERROR:
                                Snackbar.make(mMessagesView, username + " " + getString(R.string.error_not_found), Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                                break;
                            case INTERNAL_SERVER_ERROR:
                                Snackbar.make(mMessagesView, getString(R.string.error_connection_server), Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                                break;
                            case CONNECTION_ERROR_DATA:
                                Snackbar.make(mMessagesView, getString(R.string.error_connection_server), Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                                break;
                            default:
                                break;
                        }
                    }
                }
            } else {
                Snackbar.make(mMessagesView, getString(R.string.error_connection_server), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        }
    }

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            fragment.getConversationAdapter().add(new Conversation(0, false, null, intent.getExtras().getString("message").substring(1), null));
            fragment.getConversationAdapter().notifyDataSetChanged();

            Snackbar.make(mMessagesView, "New message", Snackbar.LENGTH_LONG).setAction("Show", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mMessagesView.setSelection(mMessagesView.getCount() - 1);
                }
            }).show();
        }
    }
}