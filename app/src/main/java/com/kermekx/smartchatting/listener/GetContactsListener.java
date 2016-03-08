package com.kermekx.smartchatting.listener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kermekx on 08/03/2016.
 */
public class GetContactsListener extends DataListener {

    private static final String CONTACTS_DATA = "CONTACTS ADDED";
    private static final String GET_CONTACTS_ERROR_DATA = "GET CONTACTS ERROR";

    public volatile List<String> data = new ArrayList<>();

    @Override
    public void onData(Object... object) {
        List<String> data = (List<String>) object[0];

        if (data.get(0).equals(CONTACTS_DATA) || data.get(0).equals(GET_CONTACTS_ERROR_DATA)) {
            synchronized(this.data) {
                for (String d : data)
                    this.data.add(d);
                this.data.notify();
            }
        }
    }
}
