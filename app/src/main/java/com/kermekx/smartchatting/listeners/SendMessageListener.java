package com.kermekx.smartchatting.listeners;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by asus on 10/03/2016.
 */
public class SendMessageListener extends DataListener {

    private static final String MESSAGE_SENT_DATA = "MESSAGE SENT";
    private static final String SEND_MESSAGE_ERROR_DATA = "SEND MESSAGE ERROR";

    public volatile List<String> data = new ArrayList<>();

    @Override
    public void onData(Object... object) {
        List<String> data = (List<String>) object[0];

        if (data.get(0).equals(MESSAGE_SENT_DATA) || data.get(0).equals(SEND_MESSAGE_ERROR_DATA)) {
            synchronized(this.data) {
                for (String d : data)
                    this.data.add(d);
                this.data.notify();
            }
        }
    }
}
