package com.kermekx.smartchatting.conversation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.kermekx.smartchatting.R;

import java.util.List;

/**
 * Created by kermekx on 12/02/2016.
 *
 * Conversation adapter to display messages list view
 */
public class ConversationAdapter extends ArrayAdapter<Conversation> {

    public ConversationAdapter(Context context, List<Conversation> conversation) {
        super(context, 0, conversation);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Conversation conversation = getItem(position);

        if (conversation.isSent())
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_sended_message, parent, false);
        else
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_received_message, parent, false);

        ConversationViewHolder viewHolder = (ConversationViewHolder) convertView.getTag();
        if (viewHolder == null) {
            viewHolder = new ConversationViewHolder();
            viewHolder.avatar = (ImageView) convertView.findViewById(R.id.avatar);
            viewHolder.message = (TextView) convertView.findViewById(R.id.message);
            convertView.setTag(viewHolder);
        }

        viewHolder.message.setText(conversation.getMessage());

        if(conversation.getIcon() != null)
            viewHolder.avatar.setImageDrawable(conversation.getIcon());

        return convertView;
    }

    private class ConversationViewHolder {
        public ImageView avatar;
        public TextView message;
    }
}
