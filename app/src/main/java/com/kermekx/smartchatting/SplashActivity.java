package com.kermekx.smartchatting;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kermekx.smartchatting.tasks.ActivationTask;
import com.kermekx.smartchatting.services.ServerService;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Activity launched at start of the application
 * <p/>
 * The activity try to connect
 */
public class SplashActivity extends AppCompatActivity {

    private static final String SPLASH_RECEIVER = "SPLASH_RECEIVER";
    private static final String HEADER_CONNECTION = "CONNECTION DATA";

    private ProgressBar mProgressView;
    private TextView mErrorView;

    private String mEmail;
    private String mPassword;

    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Intent service = new Intent(this, ServerService.class);
        startService(service);

        mProgressView = (ProgressBar) findViewById(R.id.login_progress);
        mErrorView = (TextView) findViewById(R.id.error);
    }

    @Override
    protected void onResume() {
        super.onResume();
        receiver = new SplashReceiver();
        registerReceiver(receiver, new IntentFilter(SPLASH_RECEIVER));

        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);

        mEmail = settings.getString("email", null);
        mPassword = settings.getString("password", null);

        if (mEmail == null || mPassword == null) {
            Intent loginIntent = new Intent(SplashActivity.this, LoginActivity.class);
            SplashActivity.this.startActivity(loginIntent);
            finish();
        } else {
            Bundle extras = new Bundle();

            extras.putString("header", HEADER_CONNECTION);
            extras.putString("filter", SPLASH_RECEIVER);
            extras.putString("email", mEmail);
            extras.putString("password", mPassword);
            extras.putString("pin", null);
            extras.putBoolean("firstConnection", false);

            final Intent loginIntent = new Intent(ServerService.SERVER_RECEIVER);
            loginIntent.putExtras(extras);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (ServerService.ready.charAt(0) == 'f') {
                        Logger.getLogger(getClass().getName()).log(Level.WARNING, "Waiting!");
                        synchronized (ServerService.ready) {
                            try {
                                ServerService.ready.wait(10000);
                            } catch (InterruptedException e) {

                            }
                        }
                    }

                    if (ServerService.ready.charAt(0) == 'f') {
                        Logger.getLogger(getClass().getName()).log(Level.WARNING, "Connection Timed out!");
                        showConnectionError(true);
                    } else {
                        sendBroadcast(loginIntent);
                    }
                }
            }).start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
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

    public class SplashReceiver extends BroadcastReceiver {

        private static final String INTERNAL_SERVER_ERROR = "INTERNAL ERROR";
        private static final String CONNECTION_ERROR_DATA = "CONNECTION ERROR";

        @Override
        public void onReceive(Context context, Intent intent) {
            Boolean connected = intent.getExtras().getBoolean("connected");

            Logger.getLogger(getClass().getName()).log(Level.INFO, "connected : " + connected);

            if (connected) {
                Boolean success = intent.getExtras().getBoolean("success");

                if (success) {
                    Intent pinActivity = new Intent(SplashActivity.this, PinActivity.class);
                    SplashActivity.this.startActivity(pinActivity);
                    finish();
                } else {
                    boolean internalError = false;

                    ArrayList<String> errors = intent.getExtras().getStringArrayList("errors");

                    for (String error : errors) {
                        switch (error) {
                            case INTERNAL_SERVER_ERROR:
                                internalError = true;
                                break;
                            case CONNECTION_ERROR_DATA:
                                internalError = true;
                                break;
                            default:
                                break;
                        }
                    }

                    if (internalError) {
                        showConnectionError(true);
                    } else {
                        Intent loginIntent = new Intent(SplashActivity.this, LoginActivity.class);
                        SplashActivity.this.startActivity(loginIntent);
                        finish();
                    }
                }
            } else {
                showConnectionError(true);
            }
        }
    }
}