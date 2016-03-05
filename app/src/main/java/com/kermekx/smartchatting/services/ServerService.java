package com.kermekx.smartchatting.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.kermekx.smartchatting.commandes.BaseTaskListener;
import com.kermekx.smartchatting.commandes.ConnectToServerTask;
import com.kermekx.smartchatting.commandes.LoginTask;
import com.kermekx.smartchatting.commandes.RegisterTask;
import com.kermekx.smartchatting.commandes.SocketListenerTask;
import com.kermekx.smartchatting.listener.LoginListener;
import com.kermekx.smartchatting.listener.RegisterListener;
import com.kermekx.smartchatting.listener.TaskListener;

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
    private boolean connected = true;
    private List<TaskListener> dataListeners = new ArrayList<>();

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
            if (!success)
                connected = false;
            else
                new SocketListenerTask(new SocketListener(), socket).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

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

            for (TaskListener listener : dataListeners)
            listener.onData(object);

            /**
            String[] data = (String[]) object;

            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder;
            String user = data[0];
            String message = data[1];

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                builder = new NotificationCompat.Builder(ServerService.this).setAutoCancel(true).setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE).setColor(getColor(R.color.primary)).setLights(getColor(R.color.primary), 300, 1000).setSmallIcon(R.drawable.ic_menu_message).setContentTitle(user).setContentText(message).setGroup("SMART_CHATTING_MESSAGE_KEY").setCategory(Notification.CATEGORY_MESSAGE).setPriority(Notification.PRIORITY_HIGH);
            } else {
                builder = new NotificationCompat.Builder(ServerService.this).setAutoCancel(true).setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE).setColor(getResources().getColor(R.color.primary)).setLights(getResources().getColor(R.color.primary), 300, 1000).setSmallIcon(R.drawable.ic_menu_message).setContentTitle(user).setContentText(message).setGroup("SMART_CHATTING_MESSAGE_KEY").setCategory(Notification.CATEGORY_MESSAGE).setPriority(Notification.PRIORITY_HIGH);
            }

            mNotificationManager.notify("SMART_CHATTING_MESSAGE_KEY".hashCode(), builder.build());
             */
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

        private TaskListener listener;

        @Override
        public void onReceive(Context context, Intent intent) {
            String header = intent.getExtras().getString("header");

            String receiver = intent.getExtras().getString("filter");

            Logger.getLogger(getClass().getName()).log(Level.INFO, "Connected : " + connected);
            Logger.getLogger(getClass().getName()).log(Level.INFO, receiver + " ask to " + header);

            if (!connected) {
                Bundle extras = new Bundle();

                extras.putBoolean("connected", false);

                Intent broadcast = new Intent(receiver);
                broadcast.putExtras(extras);

                sendBroadcast(broadcast);
                return;
            }

            String email;
            String password;
            String username;
            String pin;
            boolean firstConnection;

            switch (header) {
                case HEADER_CONNECTION:
                    email = intent.getExtras().getString("email");
                    password = intent.getExtras().getString("password");
                    pin =  intent.getExtras().getString("pin");
                    firstConnection = intent.getExtras().getBoolean("firstConnection");

                    listener = new LoginListener();
                    dataListeners.add(listener);

                    new LoginTask(context, new ServiceListener(receiver), (LoginListener) listener, socket, email, password, pin, firstConnection).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                    break;
                case HEADER_REGISTER:
                    email = intent.getExtras().getString("email");
                    password = intent.getExtras().getString("password");
                    username =  intent.getExtras().getString("username");
                    pin =  intent.getExtras().getString("pin");

                    listener = new RegisterListener();
                    dataListeners.add(listener);

                    new RegisterTask(context, new ServiceListener(receiver), (RegisterListener) listener, socket, email, username, password, pin).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    break;
                default:
                    break;
            }
        }

        private class ServiceListener extends BaseTaskListener {

            final String mReceiver;
            ArrayList<String> errors = new ArrayList<>();
            ArrayList<String> data = new ArrayList<>();

            public ServiceListener(String receiver) {
                mReceiver = receiver;
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

                Bundle extras = new Bundle();

                extras.putBoolean("connected", true);
                extras.putBoolean("success", success);
                extras.putStringArrayList("errors", errors);
                extras.putStringArrayList("data", data);

                Intent broadcast = new Intent(mReceiver);
                broadcast.putExtras(extras);

                sendBroadcast(broadcast);
            }

            @Override
            public void onCancelled() {
                dataListeners.remove(listener);
            }
        }
    }
}
