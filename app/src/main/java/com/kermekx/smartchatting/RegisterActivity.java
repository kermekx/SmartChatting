package com.kermekx.smartchatting;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
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

import com.kermekx.smartchatting.commandes.RegisterTask;
import com.kermekx.smartchatting.commandes.TaskListener;

/**
 * Created by kermekx on 31/01/2016.
 */
public class RegisterActivity extends AppCompatActivity {

    /**
     * Keep track of the register task to ensure we can cancel it if requested.
     */
    private RegisterTask mRegisterTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private AutoCompleteTextView mUsernameView;
    private EditText mPasswordView;
    private EditText mConfirmPasswordView;
    private View mProgressView;
    private View mRegisterFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);

        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);

        mUsernameView = (AutoCompleteTextView) findViewById(R.id.username);

        mPasswordView = (EditText) findViewById(R.id.password);

        mConfirmPasswordView = (EditText) findViewById(R.id.repeat_password);
        mConfirmPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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

    private void attemptRegister() {
        if (mRegisterTask != null) {
            return;
        }

        mEmailView.setError(null);
        mUsernameView.setError(null);
        mPasswordView.setError(null);
        mConfirmPasswordView.setError(null);

        String email = mEmailView.getText().toString();
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();
        String confirmPassword = mConfirmPasswordView.getText().toString();

        if (!password.equals(confirmPassword)) {
            mConfirmPasswordView.setError(getString(R.string.error_invalid_confirm_password));
            mConfirmPasswordView.requestFocus();
            return;
        }

        showProgress(true);
        mRegisterTask = new RegisterTask(this, new RegisterTaskListener(), email, username, password);
        mRegisterTask.execute((Void) null);
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

    private class RegisterTaskListener implements TaskListener {

        @Override
        public void onError(final int error) {
            RegisterActivity.this.runOnUiThread(new Runnable() {
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
                    } else if (error == R.string.error_field_required){
                        mUsernameView.setError(getString(error));
                        mUsernameView.requestFocus();
                    } else {
                        mConfirmPasswordView.setError(getString(error));
                        mConfirmPasswordView.requestFocus();
                    }
                }
            });
        }

        @Override
        public void onData(Object... object) {

        }

        @Override
        public void onPostExecute(Boolean success) {
            mRegisterTask = null;
            showProgress(false);
            if (success) {
                Intent mainActivity = new Intent(RegisterActivity.this, MainActivity.class);
                RegisterActivity.this.startActivity(mainActivity);
                finish();
            }
        }

        @Override
        public void onCancelled() {
            mRegisterTask = null;
            showProgress(false);
        }
    }
}