package com.kermekx.smartchatting;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.kermekx.smartchatting.hash.Hasher;
import com.kermekx.smartchatting.pgp.PGPManager;
import com.kermekx.smartchatting.services.ServerService;

public class PinActivity extends AppCompatActivity {

    EditText mPinView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        mPinView = (EditText) findViewById(R.id.pin);
        mPinView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int id, KeyEvent event) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    unlock(mPinView.getText().toString());
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.unlock_button);
        mEmailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                unlock(mPinView.getText().toString());
            }
        });


        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);

        TextView welcome = (TextView) findViewById(R.id.welcome);
        welcome.setText(getString(R.string.welcome) + " " + settings.getString("username", "") + " !");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void unlock(String pin) {
        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
        String pinBackup = settings.getString("pinCheck", "");

        mPinView.setError(null);

        if (Hasher.md5(pin).equals(pinBackup)) {
            ServerService.setPassword(PGPManager.generateKeyPassword(Hasher.hexStringToByteArray(settings.getString("secure", "")), Hasher.sha256Byte(pin)));

            Intent mainActivity = new Intent(PinActivity.this, MainActivity.class);
            PinActivity.this.startActivity(mainActivity);
            finish();
        } else {
            mPinView.setError(getString(R.string.error_incorrect_pin));
            mPinView.requestFocus();
        }
    }
}
