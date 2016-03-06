package com.kermekx.smartchatting;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.TextView;

public class PinActivity extends AppCompatActivity {

    EditText mPinView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        mPinView = (EditText) findViewById(R.id.pin);

        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);

        TextView welcome = (TextView) findViewById(R.id.welcome);
        welcome.setText(getString(R.string.welcome) + " " + settings.getString("username", "") + " !");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
