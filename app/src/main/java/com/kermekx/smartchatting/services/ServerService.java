package com.kermekx.smartchatting.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.kermekx.smartchatting.R;
import com.kermekx.smartchatting.tasks.AddContactTask;
import com.kermekx.smartchatting.tasks.BaseTaskListener;
import com.kermekx.smartchatting.tasks.ConnectToServerTask;
import com.kermekx.smartchatting.tasks.DisconnectTask;
import com.kermekx.smartchatting.tasks.LoginTask;
import com.kermekx.smartchatting.tasks.RegisterTask;
import com.kermekx.smartchatting.tasks.RemoveContactTask;
import com.kermekx.smartchatting.tasks.SendMessageTask;
import com.kermekx.smartchatting.tasks.SocketListenerTask;
import com.kermekx.smartchatting.tasks.UpdateContactsTask;
import com.kermekx.smartchatting.listeners.AddContactListener;
import com.kermekx.smartchatting.listeners.GetContactsListener;
import com.kermekx.smartchatting.listeners.LoginListener;
import com.kermekx.smartchatting.listeners.NotificationListener;
import com.kermekx.smartchatting.listeners.RegisterListener;
import com.kermekx.smartchatting.listeners.RemoveContactListener;
import com.kermekx.smartchatting.listeners.SendMessageListener;
import com.kermekx.smartchatting.listeners.TaskListener;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocket;

/**
 * Created by kermekx on 01/03/2016.
 */
public class ServerService extends Service {

    private BroadcastReceiver receiver;
    public static String SERVER_RECEIVER = "SERVER_RECEIVER";

    public static volatile StringBuilder ready = new StringBuilder("f");
    private boolean connected = false;
    private List<TaskListener> dataListeners = new ArrayList<>();

    private static char[] password;

    public class LocalBinder extends Binder {
        ServerService getService() {
            return ServerService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();
    private SSLSocket socket;

    @Override
    public void onCreate() {
        super.onCreate();

        Logger.getLogger(getClass().getName()).log(Level.WARNING, "service created");

        synchronized (ready) {
            ready.setCharAt(0, 'f');
        }
        new ConnectToServerTask(this, new ConnectToServerTaskListener()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        receiver = new ServerReceiver();
        registerReceiver(receiver, new IntentFilter(SERVER_RECEIVER));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.getLogger(getClass().getName()).log(Level.WARNING, "service destroyed");
        unregisterReceiver(receiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private class ConnectToServerTaskListener extends BaseTaskListener {

        @Override
        public void onData(Object... object) {
            socket = (SSLSocket) object[0];
        }

        @Override
        public void onPostExecute(Boolean success) {
            if (!success) {
                connected = false;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(60000);
                        } catch (InterruptedException e) {

                        }

                        stopService(new Intent(ServerService.this, ServerService.class));
                        startService(new Intent(ServerService.this, ServerService.class));
                    }
                }).start();
            } else {
                connected = true;
                dataListeners.add(new NotificationListener(ServerService.this));
                new SocketListenerTask(new SocketListener(), socket).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                SharedPreferences settings = ServerService.this.getSharedPreferences(ServerService.this.getString(R.string.preference_file_session), 0);

                String email = settings.getString("email", null);
                String password = settings.getString("password", null);

                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "email : " + email);
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "password : " + password);

                if (email != null && password != null) {
                    new LoginTask(ServerService.this, null, new LoginListener(), socket, email, password, null, false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }

            synchronized (ready) {
                ready.setCharAt(0, 't');
                ready.notify();
            }
        }

        @Override
        public void onCancelled() {
            stopSelf();
        }
    }

    private class SocketListener extends BaseTaskListener {

        @Override
        public void onData(Object... object) {
            Logger.getLogger(getClass().getName()).log(Level.INFO, object.toString());

            synchronized (dataListeners) {
                for (TaskListener listener : dataListeners) {
                    synchronized (listener) {
                        listener.onData(object);
                    }
                }
            }
        }

        @Override
        public void onPostExecute(Boolean success) {
            connected = false;
        }

        @Override
        public void onCancelled() {
            connected = false;
        }
    }

    public class ServerReceiver extends BroadcastReceiver {

        private static final String HEADER_CONNECTION = "CONNECTION DATA";
        private static final String HEADER_REGISTER = "REGISTER DATA";
        private static final String HEADER_DISCONNECT = "DISCONNECT DATA";
        private static final String HEADER_ADD_CONTACT = "ADD CONTACT DATA";
        private static final String HEADER_REMOVE_CONTACT = "REMOVE CONTACT DATA";
        private static final String HEADER_GET_CONTACTS = "GET CONTACTS DATA";
        private static final String HEADER_SEND_MESSAGE = "SEND MESSAGE DATA";

        // Use to send a message
        private static final String MESSAGE = "SEND";

        private TaskListener listener;

        @Override
        public void onReceive(Context context, Intent intent) {
            String header = intent.getExtras().getString("header");

            String receiver = intent.getExtras().getString("filter");

            Logger.getLogger(getClass().getName()).log(Level.WARNING, receiver + " asking to " + header + " - status : " + connected);
            if (!connected) {
                Bundle extras = new Bundle();

                extras.putBoolean("connected", false);

                Intent broadcast = new Intent(receiver);
                broadcast.putExtras(extras);

                sendBroadcast(broadcast);

                stopService(new Intent(context, ServerService.class));
                startService(new Intent(context, ServerService.class));

                SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);

                String mEmail = settings.getString("email", null);
                String mPassword = settings.getString("password", null);
                String mPin = settings.getString("pin", null);

                if (mEmail != null || mPassword != null && mPin != null) {
                    Bundle extrasToSend = new Bundle();

                    extrasToSend.putString("header", HEADER_CONNECTION);
                    extrasToSend.putString("filter", "null");
                    extrasToSend.putString("email", mEmail);
                    extrasToSend.putString("password", mPassword);
                    extrasToSend.putString("pin", mPin);
                    extrasToSend.putBoolean("firstConnection", false);

                    final Intent loginIntent = new Intent(ServerService.SERVER_RECEIVER);
                    loginIntent.putExtras(extrasToSend);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (ServerService.ready.charAt(0) == 'f') {
                                Logger.getLogger(getClass().getName()).log(Level.WARNING, "Waiting!");
                                synchronized (ServerService.ready) {
                                    try {
                                        ServerService.ready.wait(10000);
                                    } catch (InterruptedException e) {

                                    }
                                }
                            }

                            if (ServerService.ready.charAt(0) == 'f') {
                                Logger.getLogger(getClass().getName()).log(Level.WARNING, "Connection Timed out!");
                            } else {
                                sendBroadcast(loginIntent);
                            }
                        }
                    }).start();
                }

                startService(new Intent(context, ServerService.class));

                return;
            }

            String email;
            String password;
            String username;
            String pin;
            String message;
            boolean firstConnection;

            switch (header) {
                case HEADER_CONNECTION:
                    email = intent.getExtras().getString("email");
                    password = intent.getExtras().getString("password");
                    pin = intent.getExtras().getString("pin");
                    firstConnection = intent.getExtras().getBoolean("firstConnection");

                    listener = new LoginListener();
                    synchronized (dataListeners) {
                        dataListeners.add(listener);
                    }

                    new LoginTask(context, new ServiceListener(receiver, intent.getExtras()), (LoginListener) listener, socket, email, password, pin, firstConnection).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                    break;
                case HEADER_REGISTER:
                    email = intent.getExtras().getString("email");
                    password = intent.getExtras().getString("password");
                    username = intent.getExtras().getString("username");
                    pin = intent.getExtras().getString("pin");

                    listener = new RegisterListener();
                    synchronized (dataListeners) {
                        dataListeners.add(listener);
                    }

                    new RegisterTask(context, new ServiceListener(receiver, intent.getExtras()), (RegisterListener) listener, socket, email, username, password, pin).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    break;
                case HEADER_DISCONNECT:
                    new DisconnectTask(context, socket).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    break;
                case HEADER_ADD_CONTACT:
                    username = intent.getExtras().getString("username");

                    listener = new AddContactListener();
                    synchronized (dataListeners) {
                        dataListeners.add(listener);
                    }

                    new AddContactTask(context, new ServiceListener(receiver, intent.getExtras()), (AddContactListener) listener, socket, username).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    break;
                case HEADER_REMOVE_CONTACT:
                    username = intent.getExtras().getString("username");

                    listener = new RemoveContactListener();
                    synchronized (dataListeners) {
                        dataListeners.add(listener);
                    }

                    Bundle extras = new Bundle();

                    new RemoveContactTask(context, new ServiceListener(receiver, intent.getExtras()), (RemoveContactListener) listener, socket, username).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    break;
                case HEADER_GET_CONTACTS:
                    listener = new GetContactsListener();
                    synchronized (dataListeners) {
                        dataListeners.add(listener);
                    }

                    new UpdateContactsTask(context, new ServiceListener(receiver, intent.getExtras()), (GetContactsListener) listener, socket).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    break;
                case HEADER_SEND_MESSAGE:
                    username = intent.getExtras().getString("username");
                    message = intent.getExtras().getString("message");
                    String senderPublicKey = intent.getExtras().getString("senderPublicKey");
                    String receiverPublicKey = intent.getExtras().getString("receiverPublicKey");

                    listener = new SendMessageListener();
                    synchronized (dataListeners) {
                        dataListeners.add(listener);
                    }

                    new SendMessageTask(context, new ServiceListener(receiver, intent.getExtras()), (SendMessageListener) listener, socket, username, message, receiverPublicKey, senderPublicKey).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    break;
                default:
                    break;
            }
        }

        private class ServiceListener extends BaseTaskListener {

            final String mReceiver;
            final Bundle mExtras;
            ArrayList<String> errors = new ArrayList<>();
            ArrayList<String> data = new ArrayList<>();

            public ServiceListener(String receiver, Bundle extras) {
                mReceiver = receiver;
                mExtras = extras;
            }

            @Override
            public void onError(String error) {
                errors.add(error);
            }

            @Override
            public void onData(String data) {
                this.data.add(data);
            }

            @Override
            public void onPostExecute(Boolean success) {
                dataListeners.remove(listener);

                mExtras.putBoolean("connected", true);
                mExtras.putBoolean("success", success);
                mExtras.putStringArrayList("errors", errors);
                mExtras.putStringArrayList("data", data);

                Intent broadcast = new Intent(mReceiver);
                broadcast.putExtras(mExtras);

                sendBroadcast(broadcast);
            }

            @Override
            public void onCancelled() {
                dataListeners.remove(listener);
            }
        }
    }

    public static void setPassword(char[] password) {
        ServerService.password = password;
    }

    public static char[] getPassword() {
        return password;
    }
}
