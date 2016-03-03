package com.kermekx.smartchatting.commandes;

/**
 * Created by Le Dragon on 03/03/2016.
 */
public abstract class BaseTaskListener implements TaskListener {

    @Override
    public void onError(int error) {

    }

    @Override
    public void onError(String error) {

    }

    @Override
    public void onData(String data) {

    }

    @Override
    public void onData(Object... object) {
        
    }
}
