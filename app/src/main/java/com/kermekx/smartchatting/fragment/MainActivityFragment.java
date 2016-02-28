package com.kermekx.smartchatting.fragment;

import android.app.Fragment;
import android.os.Bundle;

import com.wdullaer.swipeactionadapter.SwipeActionAdapter;

/**
 * Created by kermekx on 28/02/2016.
 */
public class MainActivityFragment extends Fragment {

    private SwipeActionAdapter mMessageAdapter;
    private SwipeActionAdapter mContactAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void setMessageAdapter(SwipeActionAdapter mMessageAdapter) {
        this.mMessageAdapter = mMessageAdapter;
    }

    public SwipeActionAdapter getMessageAdapter() {
        return mMessageAdapter;
    }

    public void setContactAdapter(SwipeActionAdapter mContactAdapter) {
        this.mContactAdapter = mContactAdapter;
    }

    public SwipeActionAdapter getContactAdapter() {
        return mContactAdapter;
    }
}
