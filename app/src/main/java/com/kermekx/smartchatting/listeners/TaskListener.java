package com.kermekx.smartchatting.listeners;

/**
 * Created by kermekx on 22/02/2016.
 *
 * This listener is used to get callbacks from tasks
 */
public interface TaskListener {

    /**
     * Call when an error is occurred
     * @param error The error code is the error string id from resources
     */
    public void onError(final int error);

    public void onError(final String error);

    /**
     * Call to send result data to the listener
     * @param object Data, may vary between tasks
     */
    public void onData(Object... object);

    public void onData(final String data);

    /**
     * call when the task is finished
     * @param success is the task done successfully
     */
    public void onPostExecute(final Boolean success);

    /**
     * Call when the task has been canceled
     */
    public void onCancelled();

}
