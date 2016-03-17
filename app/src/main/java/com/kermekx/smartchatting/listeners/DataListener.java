package com.kermekx.smartchatting.listeners;

import com.kermekx.smartchatting.tasks.BaseTaskListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kermekx on 04/03/2016.
 */
public class DataListener extends BaseTaskListener {

    public final List<String> data = new ArrayList<>();

    private int id = -1;

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public void onData(Object... object) {
        List<String> data = (List<String>) object[0];

        if (Integer.valueOf(data.get(1)) == id) {
            id = -1;
            synchronized (this.data) {
                for (String d : data)
                    this.data.add(d);
                this.data.notify();
            }
        }
    }

    @Override
    public void onPostExecute(Boolean success) {

    }

    @Override
    public void onCancelled() {

    }
}
