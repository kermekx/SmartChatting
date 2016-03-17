package com.kermekx.smartchatting.listeners;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kermekx on 08/03/2016.
 */
public class AddContactListener extends DataListener {

    private static final String CONTACT_ADDED_DATA = "CONTACT ADDED";
    private static final String ADD_CONTACT_ERROR_DATA = "ADD CONTACT ERROR";

    public volatile List<String> data = new ArrayList<>();

    @Override
    public void onData(Object... object) {
        List<String> data = (List<String>) object[0];

        if (data.get(0).equals(CONTACT_ADDED_DATA) || data.get(0).equals(ADD_CONTACT_ERROR_DATA)) {
            synchronized(this.data) {
                for (String d : data)
                    this.data.add(d);
                this.data.notify();
            }
        }
    }
}
