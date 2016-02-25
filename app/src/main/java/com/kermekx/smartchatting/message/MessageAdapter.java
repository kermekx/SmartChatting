package com.kermekx.smartchatting.message;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.kermekx.smartchatting.R;
import com.kermekx.smartchatting.contact.Contact;

import java.util.List;

/**
 * Created by kermekx on 03/02/2016.
 *
 * Messages adapter to display latest messages list view
 */
public class MessageAdapter extends ArrayAdapter<Message> {

    public MessageAdapter(Context context, List<Message> messages) {
        super(context, 0, messages);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.message_list_item, parent, false);
        }

        MessageViewHolder viewHolder = (MessageViewHolder) convertView.getTag();
        if (viewHolder == null) {
            viewHolder = new MessageViewHolder();
            viewHolder.avatar = (ImageView) convertView.findViewById(R.id.avatar);
            viewHolder.username = (TextView) convertView.findViewById(R.id.username);
            viewHolder.lastMessage = (TextView) convertView.findViewById(R.id.last_message);
            convertView.setTag(viewHolder);
        }

        Message message = getItem(position);

        viewHolder.username.setText(message.getUsername());
        viewHolder.lastMessage.setText(message.getLastMessage());

        if(message.getIcon() != null)
            viewHolder.avatar.setImageDrawable(message.getIcon());
        else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            viewHolder.avatar.setImageDrawable(getContext().getDrawable(android.R.drawable.sym_def_app_icon));
        } else {
            viewHolder.avatar.setImageDrawable(getContext().getResources().getDrawable(android.R.drawable.sym_def_app_icon));
        }

        return convertView;
    }

    private class MessageViewHolder {
        public ImageView avatar;
        public TextView username;
        public TextView lastMessage;
    }
}
