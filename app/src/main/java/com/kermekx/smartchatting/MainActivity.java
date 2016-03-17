package com.kermekx.smartchatting;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
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
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.kermekx.smartchatting.tasks.BaseTaskListener;
import com.kermekx.smartchatting.tasks.GetContactsTask;
import com.kermekx.smartchatting.tasks.GetMessagesTask;
import com.kermekx.smartchatting.tasks.LoadIconTask;
import com.kermekx.smartchatting.contact.Contact;
import com.kermekx.smartchatting.contact.ContactAdapter;
import com.kermekx.smartchatting.datas.ContactsData;
import com.kermekx.smartchatting.dialog.AddContactDialog;
import com.kermekx.smartchatting.dialog.ConfirmLogoutDialog;
import com.kermekx.smartchatting.fragment.MainActivityFragment;
import com.kermekx.smartchatting.message.Message;
import com.kermekx.smartchatting.message.MessageAdapter;
import com.kermekx.smartchatting.pgp.PGPManager;
import com.kermekx.smartchatting.services.ServerService;
import com.wdullaer.swipeactionadapter.SwipeActionAdapter;
import com.wdullaer.swipeactionadapter.SwipeDirection;

import org.bouncycastle.openpgp.PGPSecretKeyRing;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String HEADER_DISCONNECT = "DISCONNECT DATA";
    private static final String HEADER_ADD_CONTACT = "ADD CONTACT DATA";
    private static final String HEADER_REMOVE_CONTACT = "REMOVE CONTACT DATA";
    private static final String HEADER_GET_CONTACTS = "GET CONTACTS DATA";

    private ListView mListView;

    private MainActivityFragment fragment;

    private int menuId = R.id.nav_message;

    private SwipeRefreshLayout mRefresh;

    private AdView mAdView;

    private String secretKeyRingBlock;

    private static final String ADD_CONTACT_RECEIVER = "ADD_CONTACT_RECEIVER";
    private BroadcastReceiver addContactReceiver;

    private static final String REMOVE_CONTACT_RECEIVER = "REMOVE_CONTACT_RECEIVER";
    private BroadcastReceiver removeContactReceiver;

    private static final String GET_CONTACTS_RECEIVER = "GET_CONTACTS_RECEIVER";
    private BroadcastReceiver getContactsReceiver;

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
                    mRefresh.setRefreshing(true);
                    new GetMessagesTask(MainActivity.this, new GetMessagesTaskListener()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else if (menuId == R.id.nav_contact) {
                    mRefresh.setRefreshing(true);

                    Bundle extras = new Bundle();

                    extras.putString("header", HEADER_GET_CONTACTS);
                    extras.putString("filter", GET_CONTACTS_RECEIVER);

                    Intent service = new Intent(ServerService.SERVER_RECEIVER);
                    service.putExtras(extras);

                    sendBroadcast(service);
                }
            }
        });

        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_session), 0);

        secretKeyRingBlock = settings.getString("privateKey", null);

        TextView email = (TextView) navigationView.getHeaderView(0).findViewById(R.id.header_email);
        TextView username = (TextView) navigationView.getHeaderView(0).findViewById(R.id.header_username);

        email.setText(settings.getString("email", "erreur!"));
        username.setText(settings.getString("username", "erreur!"));

        loadIcons(settings.getString("username", "erreur!"));

        mListView = (ListView) findViewById(R.id.contacts);

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

        FragmentManager fm = getFragmentManager();
        fragment = (MainActivityFragment) fm.findFragmentByTag("MainActivityFragment");

        if (fragment == null) {
            fragment = new MainActivityFragment();
            fm.beginTransaction().add(fragment, "MainActivityFragment").commit();

            new GetMessagesTask(this, new GetMessagesTaskListener()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            new GetContactsTask(this, new GetContactsTaskListener()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        if (menuId == R.id.nav_message && fragment.getMessageAdapter() != null) {
            setTitle(getString(R.string.title_activity_main));

            mListView.setAdapter(fragment.getMessageAdapter());
            setListViewHeightBasedOnChildren(mListView);
            mListView.setOnItemClickListener(selectConversation);
        } else if (menuId == R.id.nav_contact && fragment.getContactAdapter() != null) {
            setTitle(getString(R.string.title_activity_main_contact));

            mListView.setAdapter(fragment.getContactAdapter());
            setListViewHeightBasedOnChildren(mListView);
            mListView.setOnItemClickListener(selectContact);
        }

        addContactReceiver = new AddContactReceiver();
        registerReceiver(addContactReceiver, new IntentFilter(ADD_CONTACT_RECEIVER));

        removeContactReceiver = new RemoveContactReceiver();
        registerReceiver(removeContactReceiver, new IntentFilter(REMOVE_CONTACT_RECEIVER));

        getContactsReceiver = new GetContactsReceiver();
        registerReceiver(getContactsReceiver, new IntentFilter(GET_CONTACTS_RECEIVER));
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

        unregisterReceiver(addContactReceiver);
        unregisterReceiver(removeContactReceiver);
        unregisterReceiver(getContactsReceiver);
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

            if (fragment.getMessageAdapter() != null) {
                mListView.setAdapter(fragment.getMessageAdapter());
                setListViewHeightBasedOnChildren(mListView);
                fragment.getMessageAdapter().setSwipeActionListener(mSwipeActionListener);
                menuId = id;

                mListView.setOnItemClickListener(selectConversation);
            }
        } else if (id == R.id.nav_contact) {
            setTitle(getString(R.string.title_activity_main_contact));

            if (fragment.getContactAdapter() != null) {
                mListView.setAdapter(fragment.getContactAdapter());
                setListViewHeightBasedOnChildren(mListView);
                fragment.getContactAdapter().setSwipeActionListener(mSwipeActionListener);
                menuId = id;

                mListView.setOnItemClickListener(selectContact);
            }

            updateContacts();
        } else if (id == R.id.nav_support) {
            Intent contactActivity = new Intent(MainActivity.this, ContactActivity.class);
            Bundle extra = new Bundle();
            extra.putString("username", "Team Smart Chatting");
            extra.putString("email", "contact@smart-chatting.com");
            //TODO : put publicKey
            extra.putString("publicKey", "null");
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
            Bundle extras = new Bundle();

            extras.putString("header", HEADER_ADD_CONTACT);
            extras.putString("filter", ADD_CONTACT_RECEIVER);
            extras.putString("username", contact);

            Intent service = new Intent(ServerService.SERVER_RECEIVER);
            service.putExtras(extras);

            sendBroadcast(service);
        }
    }

    public void logout() {
        Bundle extras = new Bundle();

        extras.putString("header", HEADER_DISCONNECT);
        extras.putString("filter", "null");


        Intent service = new Intent(ServerService.SERVER_RECEIVER);
        service.putExtras(extras);

        sendBroadcast(service);

        Intent loginActivity = new Intent(MainActivity.this, LoginActivity.class);
        MainActivity.this.startActivity(loginActivity);
        finish();
    }

    public void loadIcons(String user) {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        loadIcon(user, (ImageView) navigationView.getHeaderView(0).findViewById(R.id.imageView));
    }

    public void loadIcon(String username, ImageView imageView) {
        new LoadIconTask(this, new LoadIconTaskListener(imageView), username, 48).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class GetMessagesTaskListener extends BaseTaskListener {

        List<Integer> mId = new ArrayList<>();
        List<String> mContacts = new ArrayList<>();
        List<String> mIsSent = new ArrayList<>();
        List<String> mMessages = new ArrayList<>();

        @Override
        public void onError(int error) {

        }

        @Override
        public void onData(Object... object) {
            String[] data = (String[]) object;
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
                final List<Message> messages = new ArrayList<>();

                for (int i = mMessages.size() - 1; i >= 0; i--) {
                    String[] infos = ContactsData.getContact(MainActivity.this, mContacts.get(i));

                    Message message;
                    if (infos != null) {
                        message = new Message(mContacts.get(i), getString(R.string.decrypting), infos[2], infos[3]);
                    } else {
                        message = new Message(mContacts.get(i), getString(R.string.decrypting), null, null);
                    }

                    messages.add(message);
                }

                final SwipeActionAdapter mMessageAdapter = new SwipeActionAdapter(new MessageAdapter(MainActivity.this, messages));
                mMessageAdapter.setListView(mListView);

                mMessageAdapter.addBackground(SwipeDirection.DIRECTION_NORMAL_LEFT, R.layout.row_bg_delete)
                        .addBackground(SwipeDirection.DIRECTION_NORMAL_RIGHT, R.layout.row_bg_chat);

                mMessageAdapter.setSwipeActionListener(mSwipeActionListener);

                fragment.setMessageAdapter(mMessageAdapter);

                if (menuId == R.id.nav_message) {
                    mListView.setAdapter(mMessageAdapter);
                    setListViewHeightBasedOnChildren(mListView);
                    mListView.setOnItemClickListener(selectConversation);
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE);
                        PGPSecretKeyRing secretKeyRing = PGPManager.readSecreteKeyRing(secretKeyRingBlock);

                        for (int i = mMessages.size() - 1; i >= 0; i--) {
                            ByteArrayOutputStream data = new ByteArrayOutputStream();

                            PGPManager.decode(secretKeyRing, ServerService.getPassword(), mMessages.get(i), data);

                            byte[] mes = data.toByteArray();

                            if (mes[0] == 'M') {
                                messages.get(mMessages.size() - (i + 1)).setLastMessage(new String(data.toByteArray(), 1, mes.length - 1));

                                MainActivity.this.runOnUiThread(new Runnable() {
                                    public void run() {
                                        mMessageAdapter.notifyDataSetChanged();
                                    }
                                });
                            }
                        }
                    }
                }).start();
            }

            mRefresh.setRefreshing(false);
        }

        @Override
        public void onCancelled() {

        }
    }

    private class GetContactsTaskListener extends BaseTaskListener {

        List<Contact> contacts = new ArrayList<>();
        private List<LoadIconTask> tasks = new ArrayList<>();

        @Override
        public void onError(int error) {

        }

        @Override
        public void onData(Object... object) {
            String[] data = (String[]) object;
            Contact contact = new Contact(data[1], data[2], data[3], null);
            contacts.add(contact);
            //tasks.add(new LoadIconTask(MainActivity.this, new LoadIconTaskListener(contact), data[1], 48));
        }

        @Override
        public void onPostExecute(Boolean success) {
            if (success) {
                SwipeActionAdapter mContactAdapter = new SwipeActionAdapter(new ContactAdapter(MainActivity.this, contacts));
                mContactAdapter.setListView(mListView);

                mContactAdapter.addBackground(SwipeDirection.DIRECTION_NORMAL_LEFT, R.layout.row_bg_delete)
                        .addBackground(SwipeDirection.DIRECTION_NORMAL_RIGHT, R.layout.row_bg_chat);

                mContactAdapter.setSwipeActionListener(mSwipeActionListener);

                fragment.setContactAdapter(mContactAdapter);

                if (menuId == R.id.nav_contact) {
                    mListView.setAdapter(mContactAdapter);
                    setListViewHeightBasedOnChildren(mListView);
                    mListView.setOnItemClickListener(selectContact);
                }

                for (LoadIconTask task : tasks)
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }

        @Override
        public void onCancelled() {

        }
    }

    public void updateContacts() {
        mRefresh.setRefreshing(true);

        Bundle extras = new Bundle();

        extras.putString("header", HEADER_GET_CONTACTS);
        extras.putString("filter", GET_CONTACTS_RECEIVER);

        Intent service = new Intent(ServerService.SERVER_RECEIVER);
        service.putExtras(extras);

        sendBroadcast(service);
    }

    private class LoadIconTaskListener extends BaseTaskListener {

        private final Object mItem;

        private SwipeActionAdapter adapter;

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
                    adapter = fragment.getContactAdapter();
                } else if (mItem instanceof Message) {
                    ((Message) mItem).setIcon(drawable);
                    adapter = fragment.getMessageAdapter();
                } else if (mItem instanceof ImageView) {
                    ((ImageView) mItem).setImageDrawable(drawable);
                }
            }
        }

        @Override
        public void onPostExecute(Boolean success) {
            if (success && adapter != null) {
                adapter.notifyDataSetChanged();
            }
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
            Bundle extras = new Bundle();
            extras.putString("username", username.getText().toString());
            extras.putString("email", ((Contact) fragment.getContactAdapter().getItem(position)).getEmail());
            extras.putString("publicKey", ((Contact) fragment.getContactAdapter().getItem(position)).getPublicKey());
            contactActivity.putExtras(extras);
            MainActivity.this.startActivity(contactActivity);
        }
    };

    SwipeActionAdapter.SwipeActionListener mSwipeActionListener = new SwipeActionAdapter.SwipeActionListener() {
        @Override
        public boolean hasActions(int i, SwipeDirection swipeDirection) {
            mRefresh.setEnabled(false);

            if (swipeDirection.isLeft())
                return true;

            if (swipeDirection.isRight() && (menuId == R.id.nav_contact || (menuId == R.id.nav_message && ((Message) fragment.getMessageAdapter().getItem(i)).getPublicKey() != null)))
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

            if (menuId == R.id.nav_contact) {
                for (int i = 0; i < positions.length; i++) {
                    SwipeDirection direction = swipeDirections[i];
                    int position = positions[i];

                    String username = ((Contact) fragment.getContactAdapter().getItem(position)).getUsername();
                    String email = ((Contact) fragment.getContactAdapter().getItem(position)).getEmail();
                    String publicKey = ((Contact) fragment.getContactAdapter().getItem(position)).getPublicKey();

                    if (direction == SwipeDirection.DIRECTION_FAR_LEFT) {
                        mRefresh.setRefreshing(true);

                        Bundle extras = new Bundle();

                        extras.putString("header", HEADER_REMOVE_CONTACT);
                        extras.putString("filter", REMOVE_CONTACT_RECEIVER);
                        extras.putString("username", username);

                        Intent service = new Intent(ServerService.SERVER_RECEIVER);
                        service.putExtras(extras);

                        sendBroadcast(service);
                    } else if (direction.isRight()) {
                        Intent conversationActivity = new Intent(MainActivity.this, ConversationActivity.class);
                        Bundle extra = new Bundle();
                        extra.putString("username", username);
                        extra.putString("email", email);
                        extra.putString("publicKey", publicKey);
                        conversationActivity.putExtras(extra);
                        MainActivity.this.startActivity(conversationActivity);
                    }
                }
            } else if (menuId == R.id.nav_message) {
                for (int i = 0; i < positions.length; i++) {
                    SwipeDirection direction = swipeDirections[i];
                    int position = positions[i];

                    String username = ((Message) fragment.getMessageAdapter().getItem(position)).getUsername();
                    String email = ((Message) fragment.getMessageAdapter().getItem(position)).getEmail();
                    String publicKey = ((Message) fragment.getMessageAdapter().getItem(position)).getPublicKey();

                    if (direction == SwipeDirection.DIRECTION_FAR_LEFT) {
                        Snackbar.make(mListView, "Delete message not implemented", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    } else if (direction.isRight()) {
                        Intent conversationActivity = new Intent(MainActivity.this, ConversationActivity.class);
                        Bundle extra = new Bundle();
                        extra.putString("username", username);
                        extra.putString("email", email);
                        extra.putString("publicKey", publicKey);
                        conversationActivity.putExtras(extra);
                        MainActivity.this.startActivity(conversationActivity);
                    }
                }
            }
        }
    };

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight
                + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    public class AddContactReceiver extends BroadcastReceiver {

        private static final String USER_NOT_FOUND_ERROR = "USER NOT FOUND";
        private static final String USER_ALREADY_ADDED_ERROR = "USER ALREADY ADDED";
        private static final String CANNOT_ADD_YOURSELF_ERROR = "CANNOT ADD YOURSELF";
        private static final String INTERNAL_SERVER_ERROR = "INTERNAL ERROR";
        private static final String CONNECTION_ERROR_DATA = "CONNECTION ERROR";

        @Override
        public void onReceive(Context context, Intent intent) {
            Boolean connected = intent.getExtras().getBoolean("connected");

            if (connected) {
                Boolean success = intent.getExtras().getBoolean("success");
                String adding = intent.getExtras().getString("username");

                if (success) {
                    Snackbar.make(mListView, adding + " " + getString(R.string.success_added), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                    Bundle extras = new Bundle();

                    extras.putString("header", HEADER_GET_CONTACTS);
                    extras.putString("filter", GET_CONTACTS_RECEIVER);

                    Intent service = new Intent(ServerService.SERVER_RECEIVER);
                    service.putExtras(extras);

                    sendBroadcast(service);
                } else {
                    ArrayList<String> errors = intent.getExtras().getStringArrayList("errors");

                    for (String error : errors) {
                        switch (error) {
                            case USER_NOT_FOUND_ERROR:
                                Snackbar.make(mListView, adding + " " + getString(R.string.error_not_found), Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                                break;
                            case USER_ALREADY_ADDED_ERROR:
                                Snackbar.make(mListView, adding + " " + getString(R.string.error_already_added), Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                                break;
                            case CANNOT_ADD_YOURSELF_ERROR:
                                Snackbar.make(mListView, getString(R.string.error_yourself), Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                                break;
                            case INTERNAL_SERVER_ERROR:
                                Snackbar.make(mListView, getString(R.string.error_connection_server), Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                                break;
                            case CONNECTION_ERROR_DATA:
                                Snackbar.make(mListView, getString(R.string.error_connection_server), Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                                break;
                            default:
                                break;
                        }

                        mRefresh.setRefreshing(false);
                    }
                }
            } else {
                Snackbar.make(mListView, getString(R.string.error_connection_server), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                mRefresh.setRefreshing(false);
            }
        }
    }

    public class RemoveContactReceiver extends BroadcastReceiver {

        private static final String USER_NOT_FOUND_ERROR = "USER NOT FOUND";
        private static final String NOT_FRIEND_ERROR = "NOT FRIEND";
        private static final String INTERNAL_SERVER_ERROR = "INTERNAL ERROR";
        private static final String CONNECTION_ERROR_DATA = "CONNECTION ERROR";

        @Override
        public void onReceive(Context context, Intent intent) {
            Boolean connected = intent.getExtras().getBoolean("connected");

            if (connected) {
                Boolean success = intent.getExtras().getBoolean("success");
                final String removed = intent.getExtras().getString("username");

                if (success) {
                    new GetContactsTask(MainActivity.this, new GetContactsTaskListener()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    Snackbar.make(mListView, removed + " " + getString(R.string.success_contact_deleted), Snackbar.LENGTH_LONG)
                            .setAction(getString(R.string.action_cancel), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Bundle extras = new Bundle();

                                    extras.putString("header", HEADER_ADD_CONTACT);
                                    extras.putString("filter", ADD_CONTACT_RECEIVER);
                                    extras.putString("username", removed);

                                    Intent service = new Intent(ServerService.SERVER_RECEIVER);
                                    service.putExtras(extras);

                                    sendBroadcast(service);
                                }
                            }).show();
                    mRefresh.setRefreshing(false);
                } else {
                    ArrayList<String> errors = intent.getExtras().getStringArrayList("errors");

                    for (String error : errors) {
                        switch (error) {
                            case USER_NOT_FOUND_ERROR:
                                Snackbar.make(mListView, removed + " " + getString(R.string.error_not_found), Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                                break;
                            case NOT_FRIEND_ERROR:
                                Snackbar.make(mListView, removed + " " + getString(R.string.error_not_found), Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                                break;
                            case INTERNAL_SERVER_ERROR:
                                Snackbar.make(mListView, getString(R.string.error_connection_server), Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                                break;
                            case CONNECTION_ERROR_DATA:
                                Snackbar.make(mListView, getString(R.string.error_connection_server), Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                                break;
                            default:
                                break;
                        }

                        mRefresh.setRefreshing(false);
                    }
                }
            } else {
                Snackbar.make(mListView, getString(R.string.error_connection_server), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                mRefresh.setRefreshing(false);
            }
        }
    }

    public class GetContactsReceiver extends BroadcastReceiver {

        private static final String INTERNAL_SERVER_ERROR = "INTERNAL ERROR";
        private static final String CONNECTION_ERROR_DATA = "CONNECTION ERROR";

        @Override
        public void onReceive(Context context, Intent intent) {
            Boolean connected = intent.getExtras().getBoolean("connected");

            if (connected) {
                Boolean success = intent.getExtras().getBoolean("success");

                if (success) {
                    new GetContactsTask(MainActivity.this, new GetContactsTaskListener()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    mRefresh.setRefreshing(false);
                } else {
                    ArrayList<String> errors = intent.getExtras().getStringArrayList("errors");

                    for (String error : errors) {
                        switch (error) {
                            case INTERNAL_SERVER_ERROR:
                                Snackbar.make(mListView, getString(R.string.error_connection_server), Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                                break;
                            case CONNECTION_ERROR_DATA:
                                Snackbar.make(mListView, getString(R.string.error_connection_server), Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                                break;
                            default:
                                break;
                        }

                        mRefresh.setRefreshing(false);
                    }
                }
            } else {
                Snackbar.make(mListView, getString(R.string.error_connection_server), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                mRefresh.setRefreshing(false);
            }
        }
    }
}