package com.kermekx.smartchatting.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Parcelable;

import com.kermekx.smartchatting.tasks.LoadIconTask;
import com.kermekx.smartchatting.conversation.ConversationAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kermekx on 28/02/2016.
 */
public class ConversationFragment extends Fragment {

    private Parcelable state;
    private ConversationAdapter conversationAdapter;
    private List<LoadIconTask> mTasks = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public Parcelable getState() {
        return state;
    }

    public void setState(Parcelable state) {
        this.state = state;
    }

    public ConversationAdapter getConversationAdapter() {
        return conversationAdapter;
    }

    public void setConversationAdapter(ConversationAdapter conversationAdapter) {
        this.conversationAdapter = conversationAdapter;
    }

    public List<LoadIconTask> getTasks() {
        return mTasks;
    }

    public void setTasks(List<LoadIconTask> mTasks) {
        this.mTasks = mTasks;
    }
}
