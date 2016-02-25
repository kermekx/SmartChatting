package com.kermekx.smartchatting.contact;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.kermekx.smartchatting.R;

import java.util.List;

/**
 * Created by kermekx on 03/02/2016.
 *
 * Contact adapter to display contacts list view
 */
public class ContactAdapter extends ArrayAdapter<Contact> {

    public ContactAdapter(Context context, List<Contact> contacts) {
        super(context, 0, contacts);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.contact_list_item, parent, false);
        }

        ContactViewHolder viewHolder = (ContactViewHolder) convertView.getTag();
        if (viewHolder == null) {
            viewHolder = new ContactViewHolder();
            viewHolder.avatar = (ImageView) convertView.findViewById(R.id.avatar);
            viewHolder.username = (TextView) convertView.findViewById(R.id.username);
            convertView.setTag(viewHolder);
        }

        Contact contact = getItem(position);

        viewHolder.username.setText(contact.getUsername());

        if(contact.getIcon() != null)
            viewHolder.avatar.setImageDrawable(contact.getIcon());
        else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            viewHolder.avatar.setImageDrawable(getContext().getDrawable(android.R.drawable.sym_def_app_icon));
        } else {
            viewHolder.avatar.setImageDrawable(getContext().getResources().getDrawable(android.R.drawable.sym_def_app_icon));
        }

        return convertView;
    }

    private class ContactViewHolder {
        public ImageView avatar;
        public TextView username;
    }
}
