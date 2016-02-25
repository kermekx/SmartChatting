package com.kermekx.smartchatting.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.kermekx.smartchatting.MainActivity;
import com.kermekx.smartchatting.R;

/**
 * Created by Kermekx on 08/02/2016.
 */
public class ConfirmLogoutDialog extends DialogFragment {

    EditText mUsername;
    Activity activity;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_confirm_logout, null);

        builder.setView(view);

        mUsername = (EditText) view.findViewById(R.id.username);

        Button cancel = (Button) view.findViewById(R.id.cancel);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConfirmLogoutDialog.this.getDialog().cancel();
            }
        });

        Button logout = (Button) view.findViewById(R.id.logout);

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ((MainActivity)activity).logout();

                ConfirmLogoutDialog.this.getDialog().cancel();
            }
        });

        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }
}
