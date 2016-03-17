package com.kermekx.smartchatting.listeners;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kermekx on 04/03/2016.
 */
public class RegisterListener extends DataListener {

    private static final String REGISTERED_DATA = "REGISTERED";
    private static final String REGISTER_ERROR_DATA = "REGISTER ERROR";

    public volatile List<String> data = new ArrayList<>();

    @Override
    public void onData(Object... object) {
        List<String> data = (List<String>) object[0];

        if (data.get(0).equals(REGISTER_ERROR_DATA) || data.get(0).equals(REGISTERED_DATA)) {
            synchronized(this.data) {
                for (String d : data)
                    this.data.add(d);
                this.data.notify();
            }
        }
    }
}