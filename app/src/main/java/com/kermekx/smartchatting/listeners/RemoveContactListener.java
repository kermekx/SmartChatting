package com.kermekx.smartchatting.listeners;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kermekx on 10/03/2016.
 */
public class RemoveContactListener extends DataListener {

    private static final String CONTACT_REMOVED_DATA = "CONTACT REMOVED";
    private static final String REMOVE_CONTACT_ERROR_DATA = "REMOVE CONTACT ERROR";

    public volatile List<String> data = new ArrayList<>();

    @Override
    public void onData(Object... object) {
        List<String> data = (List<String>) object[0];

        if (data.get(0).equals(CONTACT_REMOVED_DATA) || data.get(0).equals(REMOVE_CONTACT_ERROR_DATA)) {
            synchronized(this.data) {
                for (String d : data)
                    this.data.add(d);
                this.data.notify();
            }
        }
    }
}