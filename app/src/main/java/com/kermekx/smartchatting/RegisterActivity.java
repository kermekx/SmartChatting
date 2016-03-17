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
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.kermekx.smartchatting.tasks.RegisterTask;
import com.kermekx.smartchatting.services.ServerService;

import java.util.ArrayList;

/**
 * Created by kermekx on 31/01/2016.
 */
public class RegisterActivity extends AppCompatActivity {

    private static final String REGISTER_RECEIVER = "REGISTER_RECEIVER";
    private static final String HEADER_REGISTER = "REGISTER DATA";

    /**
     * Keep track of the register task to ensure we can cancel it if requested.
     */
    private RegisterTask mRegisterTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private AutoCompleteTextView mUsernameView;
    private EditText mPasswordView;
    private EditText mConfirmPasswordView;
    private EditText mPinView;
    private EditText mConfirmPinView;
    private View mProgressView;
    private View mRegisterFormView;

    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);

        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);

        mUsernameView = (AutoCompleteTextView) findViewById(R.id.username);

        mPasswordView = (EditText) findViewById(R.id.password);

        mConfirmPasswordView = (EditText) findViewById(R.id.repeat_password);

        mPinView = (EditText) findViewById(R.id.pin);

        mConfirmPinView = (EditText) findViewById(R.id.repeat_pin);

        mConfirmPinView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptRegister();
                    return true;
                }
                return false;
            }
        });

        Button mRegisterButton = (Button) findViewById(R.id.email_register_button);
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRegister();
            }
        });

        mRegisterFormView = findViewById(R.id.register_form);
        mProgressView = findViewById(R.id.register_progress);
    }

    @Override
    protected void onResume() {
        super.onResume();
        receiver = new RegisterReceiver();
        registerReceiver(receiver, new IntentFilter(REGISTER_RECEIVER));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    private void attemptRegister() {
        if (mRegisterTask != null) {
            return;
        }

        mEmailView.setError(null);
        mUsernameView.setError(null);
        mPasswordView.setError(null);
        mConfirmPasswordView.setError(null);
        mPinView.setError(null);
        mConfirmPinView.setError(null);

        String email = mEmailView.getText().toString();
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();
        String confirmPassword = mConfirmPasswordView.getText().toString();
        String pin = mPinView.getText().toString();
        String confirmPin = mConfirmPinView.getText().toString();

        boolean error = false;

        if (username.length() == 0) {
            mUsernameView.setError(getString(R.string.error_field_required));
            mUsernameView.requestFocus();
            error = true;
        }

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
            mConfirmPasswordView.setError(getString(R.string.error_invalid_password));
            mConfirmPasswordView.requestFocus();
            error = true;
        } else if (!password.equals(confirmPassword)) {
            mConfirmPasswordView.setError(getString(R.string.error_invalid_confirm_password));
            mConfirmPasswordView.requestFocus();
            error = true;
        }

        if (pin.length() < 4) {
            mConfirmPinView.setError(getString(R.string.error_invalid_pin));
            mConfirmPinView.requestFocus();
            error = true;
        } else if (!pin.equals(confirmPin)) {
            mConfirmPinView.setError(getString(R.string.error_invalid_confirm_pin));
            mConfirmPinView.requestFocus();
            error = true;
        }

        if (error)
            return;

        showProgress(true);

        Bundle extras = new Bundle();

        extras.putString("header", HEADER_REGISTER);
        extras.putString("filter", REGISTER_RECEIVER);
        extras.putString("email", email);
        extras.putString("password", password);
        extras.putString("username", username);
        extras.putString("pin", pin);

        Intent service = new Intent(ServerService.SERVER_RECEIVER);
        service.putExtras(extras);

        sendBroadcast(service);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mRegisterFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public class RegisterReceiver extends BroadcastReceiver {

        private static final String USERNAME_ALREADY_USED = "USERNAME ALREADY USED";
        private static final String EMAIL_ALREADY_USED = "EMAIL ALREADY USED";
        private static final String INTERNAL_SERVER_ERROR = "INTERNAL ERROR";
        private static final String CONNECTION_ERROR_DATA = "CONNECTION ERROR";

        @Override
        public void onReceive(Context context, Intent intent) {
            Boolean connected = intent.getExtras().getBoolean("connected");

            if (connected) {
                Boolean success = intent.getExtras().getBoolean("success");

                if (success) {
                    Intent mainActivity = new Intent(RegisterActivity.this, MainActivity.class);
                    RegisterActivity.this.startActivity(mainActivity);
                    finish();
                } else {
                    ArrayList<String> errors = intent.getExtras().getStringArrayList("errors");

                    for (String error : errors) {
                        switch (error) {
                            case USERNAME_ALREADY_USED:
                                mUsernameView.setError(getString(R.string.error_username_already_used));
                                mUsernameView.requestFocus();
                                break;
                            case EMAIL_ALREADY_USED:
                                mEmailView.setError(getString(R.string.error_email_already_used));
                                mEmailView.requestFocus();
                                break;
                            case INTERNAL_SERVER_ERROR:
                                mConfirmPinView.setError(getString(R.string.error_connection_server));
                                mConfirmPinView.requestFocus();
                                break;
                            case CONNECTION_ERROR_DATA:
                                mConfirmPinView.setError(getString(R.string.error_connection_server));
                                mConfirmPinView.requestFocus();
                                break;
                            default:
                                break;
                        }
                    }
                }

                showProgress(false);

            } else {
                mConfirmPinView.setError(getString(R.string.error_connection_server));
                mConfirmPinView.requestFocus();
                showProgress(false);
            }
        }
    }
}