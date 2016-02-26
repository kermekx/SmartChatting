package com.kermekx.smartchatting;

import android.app.AlarmManager;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.kermekx.smartchatting.commandes.AddContactTask;
import com.kermekx.smartchatting.commandes.DeleteContactTask;
import com.kermekx.smartchatting.commandes.GetContactsTask;
import com.kermekx.smartchatting.commandes.GetMessagesTask;
import com.kermekx.smartchatting.commandes.GetPrivateKeyTask;
import com.kermekx.smartchatting.commandes.LoadIconTask;
import com.kermekx.smartchatting.commandes.TaskListener;
import com.kermekx.smartchatting.commandes.UpdateContactsTask;
import com.kermekx.smartchatting.commandes.UpdateMessagesTask;
import com.kermekx.smartchatting.contact.Contact;
import com.kermekx.smartchatting.contact.ContactAdapter;
import com.kermekx.smartchatting.datas.SmartChattingBdHelper;
import com.kermekx.smartchatting.dialog.AddContactDialog;
import com.kermekx.smartchatting.dialog.ConfirmLogoutDialog;
import com.kermekx.smartchatting.message.Message;
import com.kermekx.smartchatting.message.MessageAdapter;
import com.kermekx.smartchatting.rsa.RSA;
import com.kermekx.smartchatting.schedule.NewMessage;
import com.wdullaer.swipeactionadapter.SwipeActionAdapter;
import com.wdullaer.swipeactionadapter.SwipeDirection;

import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private PendingIntent pendingIntent;
    private AlarmManager manager;

    private SwipeActionAdapter mMessageAdapter;
    private SwipeActionAdapter mContactAdapter;

    private int menuId = R.id.nav_message;

    private SwipeRefreshLayout mRefresh;
    private ListView mListView;

    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        mRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (menuId == R.id.nav_message) {
                    SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
                    new UpdateMessagesTask(MainActivity.this, new UpdateMessagesTaskListener(), settings.getString("email", ""), settings.getString("password", "")).execute();
                } else if (menuId == R.id.nav_contact) {
                    SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
                    new UpdateContactsTask(MainActivity.this, new UpdateContactsTaskListener(), settings.getString("email", ""), settings.getString("password", "")).execute();
                }
            }
        });

        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);

        TextView email = (TextView) navigationView.getHeaderView(0).findViewById(R.id.header_email);
        TextView username = (TextView) navigationView.getHeaderView(0).findViewById(R.id.header_username);

        email.setText(settings.getString("email", "erreur!"));
        username.setText(settings.getString("username", "erreur!"));

        loadIcons(settings.getString("username", "erreur!"));

        mListView = (ListView) findViewById(R.id.contacts);

        Intent newMessageIntent = new Intent(MainActivity.this, NewMessage.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, newMessageIntent, 0);

        stopNewMessage();
        startNewMessage();

        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mAdView != null) {
            mAdView.resume();
        }

        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
        new GetPrivateKeyTask(this, new GetPrivateKeyTaskListener(), settings.getString("email", ""), settings.getString("password", "")).execute();
        new GetContactsTask(this, new GetContactsTaskListener()).execute();

        if (menuId == R.id.nav_message) {
            setTitle(getString(R.string.title_activity_main));
            mListView.setAdapter(mMessageAdapter);

            mListView.setOnItemClickListener(selectConversation);
        } else if (menuId == R.id.nav_contact) {
            setTitle(getString(R.string.title_activity_main_contact));
            mListView.setAdapter(mContactAdapter);

            mListView.setOnItemClickListener(selectContact);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putInt("menu_id", menuId);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        menuId = savedInstanceState.getInt("menu_id");
    }

    @Override
    protected void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_add_contact) {
            DialogFragment addContactDialog = new AddContactDialog();
            addContactDialog.onAttach(MainActivity.this);
            addContactDialog.show(getFragmentManager(), "Add Contact");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_message) {
            setTitle(getString(R.string.title_activity_main));
            mListView.setAdapter(mMessageAdapter);
            menuId = id;

            mListView.setOnItemClickListener(selectConversation);

            updateMessages();
        } else if (id == R.id.nav_contact) {
            setTitle(getString(R.string.title_activity_main_contact));
            mListView.setAdapter(mContactAdapter);
            menuId = id;

            mListView.setOnItemClickListener(selectContact);

            updateContacts();
        } else if (id == R.id.nav_support) {
            Intent contactActivity = new Intent(MainActivity.this, ContactActivity.class);
            Bundle extra = new Bundle();
            extra.putString("username", "Team Smart Chatting");
            contactActivity.putExtras(extra);
            MainActivity.this.startActivity(contactActivity);
        } else if (id == R.id.nav_disconnect) {
            DialogFragment confirmLogoutDialog = new ConfirmLogoutDialog();
            confirmLogoutDialog.onAttach(MainActivity.this);
            confirmLogoutDialog.show(getFragmentManager(), "Confirm Logout");
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void addContact(String contact) {
        if (contact.isEmpty()) {
            Snackbar.make(mListView, getString(R.string.error_add_contact_empty), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        } else {
            SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
            new AddContactTask(MainActivity.this, new AddContactTaskListener(contact), settings.getString("email", ""), settings.getString("password", ""), contact).execute();
        }
    }

    public void logout() {
        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.commit();

        SmartChattingBdHelper.getInstance(this).onDowngrade(SmartChattingBdHelper.getInstance(this).getWritableDatabase(), 0, 0);

        stopNewMessage();

        Intent loginActivity = new Intent(MainActivity.this, LoginActivity.class);
        MainActivity.this.startActivity(loginActivity);
        finish();
    }

    public void loadIcons(String user) {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        loadIcon(user, (ImageView) navigationView.getHeaderView(0).findViewById(R.id.imageView));
    }

    public void loadIcon(String username, ImageView imageView) {
        new LoadIconTask(this, new LoadIconTaskListener(imageView), username, 48).execute();
    }

    public void startNewMessage() {
        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove("senders");
        editor.remove("messages");
        editor.commit();

        manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        int interval = 60000;

        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);
    }

    public void stopNewMessage() {
        manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntent);
    }

    private class GetMessagesTaskListener implements TaskListener {

        private final Key mKey;

        List<Integer> mId = new ArrayList<>();
        List<String> mContacts = new ArrayList<>();
        List<String> mIsSent = new ArrayList<>();
        List<String> mMessages = new ArrayList<>();

        public GetMessagesTaskListener(Key key) {
            mKey = key;
        }

        @Override
        public void onError(int error) {

        }

        @Override
        public void onData(Object... object) {
            String[] data = (String []) object;
            int index;

            if ((index = mContacts.indexOf(data[1])) >= 0) {
                if (mId.get(index) > Integer.parseInt(data[0]))
                    return;
                mId.remove(index);
                mContacts.remove(index);
                mIsSent.remove(index);
                mMessages.remove(index);
            }

            mId.add(Integer.parseInt(data[0]));
            mContacts.add(data[1]);
            mIsSent.add(data[2]);
            mMessages.add(data[3]);
        }

        @Override
        public void onPostExecute(Boolean success) {
            if (success) {
                List<Message> messages = new ArrayList<>();

                for(int i = mMessages.size() - 1; i >= 0; i --) {
                    Message message = new Message(mContacts.get(i), RSA.decrypt(mMessages.get(i), mKey));
                    new LoadIconTask(MainActivity.this, new LoadIconTaskListener(message), mContacts.get(i), 48).execute();
                    messages.add(message);
                }

                mMessageAdapter = new SwipeActionAdapter(new MessageAdapter(MainActivity.this, messages));
                mMessageAdapter.setListView(mListView);

                if (menuId == R.id.nav_message)
                    mListView.setAdapter(mMessageAdapter);
            }
        }

        @Override
        public void onCancelled() {

        }
    }

    private class GetContactsTaskListener implements TaskListener {

        List<Contact> contacts = new ArrayList<>();
        private List<LoadIconTask> tasks = new ArrayList<>();

        @Override
        public void onError(int error) {

        }

        @Override
        public void onData(Object... object) {
            String[] data = (String []) object;
            Contact contact = new Contact(data[1], null);
            contacts.add(contact);
            tasks.add(new LoadIconTask(MainActivity.this, new LoadIconTaskListener(contact), data[1], 48));
        }

        @Override
        public void onPostExecute(Boolean success) {
            if (success) {
                mContactAdapter = new SwipeActionAdapter(new ContactAdapter(MainActivity.this, contacts));
                mContactAdapter.setListView(mListView);

                mContactAdapter.addBackground(SwipeDirection.DIRECTION_NORMAL_LEFT, R.layout.row_bg_delete)
                        .addBackground(SwipeDirection.DIRECTION_NORMAL_RIGHT, R.layout.row_bg_chat);

                mContactAdapter.setSwipeActionListener(mContactSwipeActionListener);
                if (menuId == R.id.nav_contact) {
                    mListView.setAdapter(mContactAdapter);
                }

                for (LoadIconTask task : tasks)
                    task.execute();
            }
        }

        @Override
        public void onCancelled() {

        }
    }

    public void updateMessages() {
        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
        new UpdateMessagesTask(this, new UpdateMessagesTaskListener(), settings.getString("email", ""), settings.getString("password", "")).execute();
        mRefresh.setRefreshing(true);
    }

    public class UpdateMessagesTaskListener implements TaskListener {

        @Override
        public void onError(int error) {

        }

        @Override
        public void onData(Object... object) {

        }

        @Override
        public void onPostExecute(Boolean success) {
            if (success) {
                SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
                new GetPrivateKeyTask(MainActivity.this, new GetPrivateKeyTaskListener(), settings.getString("email", ""), settings.getString("password", "")).execute();
                mRefresh.setRefreshing(false);
            } else {
                Snackbar.make(mListView, getString(R.string.error_connection_server), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                mRefresh.setRefreshing(false);
            }
        }

        @Override
        public void onCancelled() {

        }
    }

    public void updateContacts() {
        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
        new UpdateContactsTask(this, new UpdateContactsTaskListener(), settings.getString("email", ""), settings.getString("password", "")).execute();
        mRefresh.setRefreshing(true);
    }

    public class UpdateContactsTaskListener implements TaskListener {

        @Override
        public void onError(int error) {

        }

        @Override
        public void onData(Object... object) {

        }

        @Override
        public void onPostExecute(Boolean success) {
            if (success) {
                new GetContactsTask(MainActivity.this, new GetContactsTaskListener()).execute();
                mRefresh.setRefreshing(false);
            } else {
                Snackbar.make(mListView, getString(R.string.error_connection_server), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                mRefresh.setRefreshing(false);
            }
        }

        @Override
        public void onCancelled() {

        }
    }

    private class GetPrivateKeyTaskListener implements TaskListener {

        @Override
        public void onError(int error) {

        }

        @Override
        public void onData(Object... object) {
            new GetMessagesTask(MainActivity.this, new GetMessagesTaskListener((Key) object[0])).execute();
        }

        @Override
        public void onPostExecute(Boolean success) {
            if (!success) {
                Snackbar.make(mListView, getString(R.string.error_connection_server), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                mRefresh.setRefreshing(false);
            }
        }

        @Override
        public void onCancelled() {

        }
    }

    private class LoadIconTaskListener implements TaskListener {

        private final Object mItem;

        public LoadIconTaskListener(Object item) {
            mItem = item;
        }

        @Override
        public void onError(int error) {

        }

        @Override
        public void onData(Object... object) {

            if (object instanceof Drawable[]) {
                Drawable drawable = (Drawable) object[0];
                if (mItem instanceof Contact) {
                    ((Contact) mItem).setIcon(drawable);
                    mContactAdapter.notifyDataSetChanged();
                } else if (mItem instanceof Message) {
                    ((Message) mItem).setIcon(drawable);
                    mMessageAdapter.notifyDataSetChanged();
                } else if (mItem instanceof ImageView) {
                    ((ImageView) mItem).setImageDrawable(drawable);
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

    private class AddContactTaskListener implements TaskListener {
        private final String mUsername;
        int error = -1;

        public AddContactTaskListener(String username) {
            mRefresh.setRefreshing(true);
            mUsername = username;
        }

        @Override
        public void onError(int error) {
            this.error = error;
        }

        @Override
        public void onData(Object... object) {

        }

        @Override
        public void onPostExecute(Boolean success) {
            if (success) {
                SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
                new UpdateContactsTask(MainActivity.this, new UpdateContactsTaskListener(), settings.getString("email", ""), settings.getString("password", "")).execute();
                Snackbar.make(mListView, mUsername + " " + getString(R.string.success_added), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            } else {
                String err;

                if (error == R.string.error_already_added || error == R.string.error_not_found) {
                    err = mUsername + " " + getString(error);
                } else {
                    err =  getString(error);
                }

                Snackbar.make(mListView, err, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                mRefresh.setRefreshing(false);
            }
        }

        @Override
        public void onCancelled() {

        }
    }

    private class DeleteContactTaskListener implements TaskListener {

        private final String mUsername;
        int error = -1;

        public DeleteContactTaskListener(String username) {
            mUsername = username;
        }

        @Override
        public void onError(int error) {
            this.error = error;
        }

        @Override
        public void onData(Object... object) {

        }

        @Override
        public void onPostExecute(Boolean success) {
            if (success) {
                new GetContactsTask(MainActivity.this, new GetContactsTaskListener()).execute();
                Snackbar.make(mListView, mUsername + " " + getString(R.string.success_contact_deleted), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.action_cancel), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
                                new AddContactTask(MainActivity.this, new AddContactTaskListener(mUsername), settings.getString("email", ""), settings.getString("password", ""), mUsername).execute();
                            }
                        }).show();
            } else {
                Snackbar.make(mListView, getString(error), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
            mRefresh.setRefreshing(false);
        }

        @Override
        public void onCancelled() {

        }
    }

    AdapterView.OnItemClickListener selectConversation = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            TextView username = (TextView) view.findViewById(R.id.username);

            Intent conversationActivity = new Intent(MainActivity.this, ConversationActivity.class);
            Bundle extra = new Bundle();
            extra.putString("username", username.getText().toString());
            conversationActivity.putExtras(extra);
            MainActivity.this.startActivity(conversationActivity);
        }
    };

    AdapterView.OnItemClickListener selectContact = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            TextView username = (TextView) view.findViewById(R.id.username);

            Intent contactActivity = new Intent(MainActivity.this, ContactActivity.class);
            Bundle extra = new Bundle();
            extra.putString("username", username.getText().toString());
            contactActivity.putExtras(extra);
            MainActivity.this.startActivity(contactActivity);
        }
    };

    SwipeActionAdapter.SwipeActionListener mContactSwipeActionListener = new SwipeActionAdapter.SwipeActionListener() {
        @Override
        public boolean hasActions(int i, SwipeDirection swipeDirection) {
            mRefresh.setEnabled(false);
            if (swipeDirection.isLeft())
                return true;

            if (swipeDirection.isRight())
                return true;

            return false;
        }

        @Override
        public boolean shouldDismiss(int i, SwipeDirection swipeDirection) {
            return swipeDirection == SwipeDirection.DIRECTION_FAR_LEFT;
        }

        @Override
        public void onSwipe(int[] positions, SwipeDirection[] swipeDirections) {
            mRefresh.setEnabled(true);

            for(int i = 0; i < positions.length; i++) {
                SwipeDirection direction = swipeDirections[i];
                int position = positions[i];

                String username = ((Contact) mContactAdapter.getItem(position)).getUsername();

                if (direction == SwipeDirection.DIRECTION_FAR_LEFT) {
                    mRefresh.setRefreshing(true);
                    SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);
                    new DeleteContactTask(MainActivity.this, new DeleteContactTaskListener(username), settings.getString("email", ""), settings.getString("password", ""), username).execute();
                } else if (direction.isRight()) {
                    Intent conversationActivity = new Intent(MainActivity.this, ConversationActivity.class);
                    Bundle extra = new Bundle();
                    extra.putString("username", username);
                    conversationActivity.putExtras(extra);
                    MainActivity.this.startActivity(conversationActivity);
                }
            }
        }
    };
}