package com.kermekx.smartchatting.commandes;

/**
 * Created by kermekx on 22/02/2016.
 */
public interface TaskListener {

    public void onError(final int error);
    public void onData(Object... object);
    public void onPostExecute(final Boolean success);
    public void onCancelled();

}
