package com.kermekx.smartchatting;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.kermekx.smartchatting.commandes.BaseTaskListener;
import com.kermekx.smartchatting.commandes.LoginTask;

public class LoginActivity extends AppCompatActivity {

    private static LoginActivity INSTANCE;

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private LoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private EditText mPinView;
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

        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
        mEmailView.setText(settings.getString("email", ""));

        if (!TextUtils.isEmpty(mEmailView.getText())) {
            String password = settings.getString("password", null);
            if (password != null) {
                attemptLogin(mEmailView.getText().toString(), password, mPinView.getText().toString(), true);
            }
        }
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
        mAuthTask = new LoginTask(this, new LoginTaskListener(), email, password, pin, hashed);
        mAuthTask.execute();
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

    private class LoginTaskListener extends BaseTaskListener {

        @Override
        public void onError(final int error) {
            LoginActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (error == R.string.error_invalid_email) {
                        if (mEmailView.getText().toString().length() == 0) {
                            mEmailView.setError(getString(R.string.error_field_required));
                        } else {
                            mEmailView.setError(getString(error));
                        }
                        mEmailView.requestFocus();
                    } else if (error == R.string.error_invalid_password) {
                        if (mPasswordView.getText().toString().length() == 0) {
                            mPasswordView.setError(getString(R.string.error_field_required));
                        } else {
                            mPasswordView.setError(getString(error));
                        }
                        mPasswordView.requestFocus();
                    } else {
                        mPasswordView.setError(getString(error));
                        mPasswordView.requestFocus();
                    }
                }
            });
        }

        @Override
        public void onData(Object... object) {

        }

        @Override
        public void onPostExecute(Boolean success) {
            mAuthTask = null;
            showProgress(false);
            if (success) {
                Intent mainActivity = new Intent(LoginActivity.this, MainActivity.class);
                LoginActivity.this.startActivity(mainActivity);
                finish();
            }
        }

        @Override
        public void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}