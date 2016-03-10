package com.kermekx.smartchatting;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kermekx.smartchatting.dialog.ConfirmRemoveContactDialog;
import com.kermekx.smartchatting.icon.IconManager;
import com.kermekx.smartchatting.json.JsonManager;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ContactActivity extends AppCompatActivity {

    private String username;
    private String email;

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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.remove_contact) {
            DialogFragment confirmRemoveContactDialog = new ConfirmRemoveContactDialog();
            confirmRemoveContactDialog.onAttach(ContactActivity.this);
            confirmRemoveContactDialog.show(getFragmentManager(), "Remove Contact");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class GetContactInfoTask extends AsyncTask<Void, Void, Boolean> {

        private String mEmail;
        private final String mUsername;

        GetContactInfoTask(String username) {
            mUsername = username;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
            String email = settings.getString("email", "");
            String password = settings.getString("password", "");

            Map<String, String> values = new HashMap<String, String>();

            values.put("email", email);
            values.put("password", password);
            values.put("username", mUsername);

            String json = JsonManager.getJSON(getString(R.string.url_get_contact_info), values);

            if (json == null) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, getString(R.string.error_connection_server) + " : null JSON");
                return false;
            }

            try {
                JSONObject result = new JSONObject(json);
                if (result.getBoolean("signed") && result.getBoolean("found")) {
                    mEmail = result.getString("email");
                    return true;
                }
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, getString(R.string.error_connection_server) + " : " + result.toString());
            } catch (Exception e) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
                return false;
            }
            return false;

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                TextView emailView = (TextView) findViewById(R.id.email);
                emailView.setText(mEmail);
                email = mEmail;
                showProgress(false);
            } else {
                finish();
            }
        }

        @Override
        protected void onCancelled() {

        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {

        final RelativeLayout content = (RelativeLayout) findViewById(R.id.content);
        final ProgressBar progress = (ProgressBar) findViewById(R.id.register_progress);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            content.setVisibility(show ? View.GONE : View.VISIBLE);
            content.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    content.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            progress.setVisibility(show ? View.VISIBLE : View.GONE);
            progress.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progress.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            content.setVisibility(show ? View.VISIBLE : View.GONE);
            progress.setVisibility(show ? View.GONE : View.VISIBLE);
        }
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

            if (mIcon != null)
                return true;

            return false;

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
        new RemoveContactTask(username).execute();
    }

    public class RemoveContactTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUsername;

        RemoveContactTask(String contact) {
            mUsername = contact;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
            String email = settings.getString("email", "");
            String password = settings.getString("password", "");

            Map<String, String> values = new HashMap<String, String>();

            values.put("email", email);
            values.put("password", password);
            values.put("username", mUsername);

            String json = JsonManager.getJSON(getString(R.string.url_remove_contact), values);

            if (json == null) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, getString(R.string.error_connection_server) + " : null JSON");
                return false;
            }

            try {
                JSONObject result = new JSONObject(json);
                if (result.getBoolean("signed") && !result.getBoolean("notFriend")) {
                    return true;
                }
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, getString(R.string.error_connection_server) + " : " + result.toString());
            } catch (Exception e) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
                return false;
            }
            return false;

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                finish();
            }
        }

        @Override
        protected void onCancelled() {

        }
    }

}
