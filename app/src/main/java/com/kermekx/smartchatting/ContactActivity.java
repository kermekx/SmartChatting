package com.kermekx.smartchatting;

import android.app.DialogFragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.kermekx.smartchatting.dialog.ConfirmRemoveContactDialog;
import com.kermekx.smartchatting.icon.IconManager;
import com.kermekx.smartchatting.services.ServerService;

public class ContactActivity extends AppCompatActivity {

    private static final String HEADER_REMOVE_CONTACT = "REMOVE CONTACT DATA";
    private static final String REMOVE_CONTACT_RECEIVER = "REMOVE_CONTACT_RECEIVER";

    private String username;
    private String email;
    private String publicKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();

        setContentView(R.layout.activity_contact);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        username = extras.getString("username");
        email = extras.getString("email");
        publicKey = extras.getString("publicKey");

        ImageView sendMail = (ImageView) findViewById(R.id.send_mail);

        sendMail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", email, null));
                intent.putExtra(Intent.EXTRA_EMAIL, email);

                startActivity(Intent.createChooser(intent, "Send email to " + username));
            }
        });

        ImageView sendMessage = (ImageView) findViewById(R.id.send_message);

        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent conversationActivity = new Intent(ContactActivity.this, ConversationActivity.class);
                Bundle extra = new Bundle();
                extra.putString("username", username);
                extra.putString("email", email);
                extra.putString("publicKey", publicKey);
                conversationActivity.putExtras(extra);
                ContactActivity.this.startActivity(conversationActivity);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        setTitle(username);
        ((TextView) findViewById(R.id.email)).setText(email);

        new LoadContactIconTask(username).execute();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("email", email);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        email = savedInstanceState.getString("email");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        if ("contact@smart-chatting.com".equals(email))
            getMenuInflater().inflate(R.menu.contact_us, menu);
        else
            getMenuInflater().inflate(R.menu.contact, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.remove_contact) {
            DialogFragment confirmRemoveContactDialog = new ConfirmRemoveContactDialog();
            confirmRemoveContactDialog.onAttach(ContactActivity.this);
            confirmRemoveContactDialog.show(getFragmentManager(), "Remove Contact");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class LoadContactIconTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUsername;

        private Bitmap mIcon;

        LoadContactIconTask(String contact) {
            mUsername = contact;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            mIcon = IconManager.getIcon(ContactActivity.this, mUsername);

            return mIcon != null;

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 128, getResources().getDisplayMetrics());
                Bitmap bitmapResized = Bitmap.createScaledBitmap(mIcon, (int) px, (int) px, false);
                Drawable drawable = new BitmapDrawable(getResources(), bitmapResized);

                ((ImageView) findViewById(R.id.icon)).setImageDrawable(drawable);
            }
        }

        @Override
        protected void onCancelled() {

        }
    }

    public void removeContact() {
        Bundle extras = new Bundle();

        extras.putString("header", HEADER_REMOVE_CONTACT);
        extras.putString("filter", REMOVE_CONTACT_RECEIVER);
        extras.putString("username", username);

        Intent service = new Intent(ServerService.SERVER_RECEIVER);
        service.putExtras(extras);

        sendBroadcast(service);

        finish();
    }
}
