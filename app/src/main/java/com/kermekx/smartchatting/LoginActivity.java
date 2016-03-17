package com.kermekx.smartchatting;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.kermekx.smartchatting.tasks.LoginTask;
import com.kermekx.smartchatting.services.ServerService;

import java.util.ArrayList;

public class LoginActivity extends AppCompatActivity {

    private static final String LOGIN_RECEIVER = "LOGIN_RECEIVER";
    private static final String HEADER_CONNECTION = "CONNECTION DATA";

    private static LoginActivity INSTANCE;

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private LoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private EditText mPinView;
    private TextView mErrorView;
    private View mProgressView;
    private View mLoginFormView;

    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        INSTANCE = this;

        setContentView(R.layout.activity_login);
        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        mPinView = (EditText) findViewById(R.id.loginPin);
        mErrorView = (TextView) findViewById(R.id.loginError);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        Button mRegisterButton = (Button) findViewById(R.id.register_button);
        mRegisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent registerActivity = new Intent(LoginActivity.this, RegisterActivity.class);
                LoginActivity.this.startActivity(registerActivity);
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    @Override
    protected void onResume() {
        super.onResume();
        receiver = new LoginReceiver();
        registerReceiver(receiver, new IntentFilter(LOGIN_RECEIVER));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    private void attemptLogin() {
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        String pin = mPinView.getText().toString();

        attemptLogin(email, password, pin, false);
    }

    private void attemptLogin(String email, String password, String pin, boolean hashed) {
        if (mAuthTask != null) {
            return;
        }

        mEmailView.setError(null);
        mPasswordView.setError(null);
        mPinView.setError(null);

        showConnectionError(false);

        email = mEmailView.getText().toString();
        password = mPasswordView.getText().toString();
        pin = mPinView.getText().toString();

        boolean error = false;

        if (email.length() == 0) {
            mEmailView.setError(getString(R.string.error_field_required));
            mEmailView.requestFocus();
            error = true;
        } else if (!(email.contains("@") && email.contains(".") && (email.indexOf("@") < email.lastIndexOf(".")))) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            mEmailView.requestFocus();
            error = true;
        }

        if (password.length() < 6 || password.length() > 42) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            mPasswordView.requestFocus();
            error = true;
        }

        if (pin.length() < 4) {
            mPinView.setError(getString(R.string.error_invalid_pin));
            mPinView.requestFocus();
            error = true;
        }

        if (error)
            return;

        showProgress(true);
        Bundle extras = new Bundle();
        extras.putString("email", email);
        extras.putString("password", password);
        extras.putString("pin", pin);
        extras.putString("filter", LOGIN_RECEIVER);
        extras.putString("header", HEADER_CONNECTION);
        extras.putBoolean("firstConnection", true);
        Intent toServerService = new Intent(ServerService.SERVER_RECEIVER);
        toServerService.putExtras(extras);
        sendBroadcast(toServerService);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showConnectionError(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

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
        }
    }

    public class LoginReceiver extends BroadcastReceiver {

        private static final String INTERNAL_SERVER_ERROR = "INTERNAL ERROR";
        private static final String BAD_LOGIN = "BAD LOGIN";
        private static final String UNVERIFIED = "UNVERIFIED";
        private static final String NO_KEYS = "NO KEYS FOUND";
        private static final String INCORRECT_PIN = "INCORRECT PIN";
        private static final String CONNECTION_ERROR_DATA = "CONNECTION ERROR";

        @Override
        public void onReceive(Context context, Intent intent) {
            Boolean connected = intent.getExtras().getBoolean("connected");

            if (connected) {
                Boolean success = intent.getExtras().getBoolean("success");
                if (success) {
                    Intent mainActivity = new Intent(LoginActivity.this, MainActivity.class);
                    LoginActivity.this.startActivity(mainActivity);
                    finish();
                } else {
                    ArrayList<String> errors = intent.getExtras().getStringArrayList("errors");

                    for (String error : errors) {
                        switch (error) {
                            case INTERNAL_SERVER_ERROR:
                                showConnectionError(true);
                                break;
                            case CONNECTION_ERROR_DATA:
                                showConnectionError(true);
                                break;
                            case BAD_LOGIN:
                                mPasswordView.setError(getString(R.string.error_incorrect_password));
                                mPasswordView.requestFocus();
                                break;
                            case UNVERIFIED:
                                mEmailView.setError(getString(R.string.error_verify_account));
                                mEmailView.requestFocus();
                                break;
                            case NO_KEYS:
                                showConnectionError(true);
                                break;
                            case INCORRECT_PIN:
                                mPinView.setError(getString(R.string.error_incorrect_pin));
                                mPinView.requestFocus();
                                break;
                            default:
                                break;
                        }
                    }
                }
                showProgress(false);
            } else {
                showProgress(false);
                showConnectionError(true);
            }
        }
    }
}