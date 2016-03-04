package com.kermekx.smartchatting;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kermekx.smartchatting.commandes.ActivationTask;
import com.kermekx.smartchatting.commandes.BaseTaskListener;
import com.kermekx.smartchatting.commandes.LoginTask;
import com.kermekx.smartchatting.services.ServerService;

/**
 * Activity launched at start of the application
 *
 * The activity try to connect
 */
public class SplashActivity extends AppCompatActivity {

    private ProgressBar mProgressView;
    private TextView mErrorView;

    private String mEmail;
    private String mPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Intent service = new Intent(this, ServerService.class);
        startService(service);

        mProgressView = (ProgressBar) findViewById(R.id.login_progress);
        mErrorView = (TextView) findViewById(R.id.error);

        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();

        if (Intent.ACTION_VIEW.equals(action) && data != null) {

            String user = data.getQueryParameter("user");
            String key = data.getQueryParameter("key");

            if (user != null && key != null) {
                new ActivationTask(this, user, key).execute();
            }
        }

        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);

        mEmail = settings.getString("email", null);
        mPassword = settings.getString("password", null);

        if (mEmail == null || mPassword == null) {
            Intent loginIntent = new Intent(SplashActivity.this, LoginActivity.class);
            SplashActivity.this.startActivity(loginIntent);
            finish();
        } else {
            new LoginTask(this, new LoginTaskListener(), mEmail, mPassword, true).execute();
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showConnectionError(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mProgressView.setVisibility(show ? View.GONE : View.VISIBLE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mErrorView.setVisibility(show ? View.VISIBLE : View.GONE);
            mErrorView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mErrorView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            mErrorView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private class LoginTaskListener extends BaseTaskListener {

        private int error;

        @Override
        public void onError(int error) {
            this.error = error;
        }

        @Override
        public void onData(Object... object) {

        }

        @Override
        public void onPostExecute(Boolean success) {
            if (success) {
                //Intent pinActivity = new Intent(SplashActivity.this, PinActivity.class);
                //SplashActivity.this.startActivity(pinActivity);
                Intent mainActivity = new Intent(SplashActivity.this, MainActivity.class);
                SplashActivity.this.startActivity(mainActivity);
                finish();
            } else if (error == R.string.error_connection_server){
                showConnectionError(true);
            } else {
                Intent loginIntent = new Intent(SplashActivity.this, LoginActivity.class);
                SplashActivity.this.startActivity(loginIntent);
                finish();
            }
        }

        @Override
        public void onCancelled() {
            error = 0;
            new LoginTask(SplashActivity.this, this, mEmail, mPassword, true).execute();
        }
    }
}