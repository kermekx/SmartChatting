package com.kermekx.smartchatting.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.kermekx.smartchatting.R;
import com.kermekx.smartchatting.commandes.BaseTaskListener;
import com.kermekx.smartchatting.commandes.ConnectToServerTask;
import com.kermekx.smartchatting.commandes.RegisterTask;

import java.util.ArrayList;

import javax.net.ssl.SSLSocket;

/**
 * Created by kermekx on 01/03/2016.
 */
public class ServerService extends Service {

    private BroadcastReceiver receiver;
    public static String SERVER_RECEIVER = "SERVER_RECEIVER";


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
        public void onError(String error) {
            stopSelf();
        }

        @Override
        public void onError(int error) {
            stopSelf();
        }

        @Override
        public void onData(Object... object) {
            socket = (SSLSocket) object[0];
        }

        @Override
        public void onPostExecute(Boolean success) {
            if (!success)
                stopSelf();
        }

        @Override
        public void onCancelled() {
            stopSelf();
        }
    }

    private class SocketListener extends BaseTaskListener {

        @Override
        public void onData(Object... object) {
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
        }

        @Override
        public void onPostExecute(Boolean success) {
            stopSelf();
        }

        @Override
        public void onCancelled() {
            stopSelf();
        }
    }

    public class ServerReceiver extends BroadcastReceiver {

        private static final String HEADER_CONNECTION = "CONNECTION DATA";
        private static final String HEADER_REGISTER = "REGISTER DATA";

        @Override
        public void onReceive(Context context, Intent intent) {
            String header = intent.getExtras().getString("header");

            String receiver = intent.getExtras().getString("filter");

            String email;
            String password;
            String username;
            String privateKey;
            String publicKey;

            switch (header) {
                case HEADER_CONNECTION:
                    email = intent.getExtras().getString("email");
                    password = intent.getExtras().getString("password");
                    break;
                case HEADER_REGISTER:
                    email = intent.getExtras().getString("email");
                    password = intent.getExtras().getString("password");
                    username =  intent.getExtras().getString("username");
                    publicKey =  intent.getExtras().getString("publicKey");
                    privateKey =  intent.getExtras().getString("privateKey");
                    new RegisterTask(new ServiceListener(receiver), socket, email, password, username, publicKey, privateKey).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
                Bundle extras = new Bundle();

                extras.putBoolean("success", success);
                extras.putStringArrayList("errors", errors);
                extras.putStringArrayList("data", data);

                Intent broadcast = new Intent(mReceiver);
                broadcast.putExtras(extras);

                sendBroadcast(broadcast);
            }

            @Override
            public void onCancelled() {

            }
        }
    }
}
