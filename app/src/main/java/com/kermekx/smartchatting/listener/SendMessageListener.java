package com.kermekx.smartchatting.listener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by asus on 10/03/2016.
 */
public class SendMessageListener extends DataListener {

    private static final String CONNECTED_DATA = "CONNECTED";
    private static final String CONNECTION_ERROR_DATA = "CONNECTION ERROR";

    public volatile List<String> data = new ArrayList<>();

    @Override
    public void onData(Object... object) {
        List<String> data = (List<String>) object[0];

        if (data.get(0).equals(CONNECTION_ERROR_DATA) || data.get(0).equals(CONNECTED_DATA)) {
            synchronized(this.data) {
                for (String d : data)
                    this.data.add(d);
                this.data.notify();
            }
        }
    }
}
