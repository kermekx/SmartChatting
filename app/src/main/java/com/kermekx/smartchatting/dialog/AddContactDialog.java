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

public class AddContactDialog extends DialogFragment {

    EditText mUsername;
    Activity activity;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_add_contact, null);

        builder.setView(view);

        mUsername = (EditText) view.findViewById(R.id.username);

        Button cancel = (Button) view.findViewById(R.id.cancel);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddContactDialog.this.getDialog().cancel();
            }
        });

        Button add = (Button) view.findViewById(R.id.add);

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ((MainActivity)activity).addContact(mUsername.getText().toString());

                AddContactDialog.this.getDialog().cancel();
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
