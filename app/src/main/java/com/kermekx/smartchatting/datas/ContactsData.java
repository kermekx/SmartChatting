package com.kermekx.smartchatting.datas;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

/**
 * Created by kermex on 23/02/2016.
 * <p/>
 * Contacts data to use in the internal database
 */
public class ContactsData {

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";

    public static final String SQL_CREATE_CONTACTS =
            "CREATE TABLE " + ContactEntry.TABLE_NAME + " (" +
                    ContactEntry._ID + " INTEGER PRIMARY KEY," +
                    ContactEntry.COLUMN_NAME_CONTACT_ID + TEXT_TYPE + COMMA_SEP +
                    ContactEntry.COLUMN_NAME_CONTACT_USERNAME + TEXT_TYPE + COMMA_SEP +
                    ContactEntry.COLUMN_NAME_CONTACT_EMAIL + TEXT_TYPE + COMMA_SEP +
                    ContactEntry.COLUMN_NAME_CONTACT_PUBLIC_KEY + TEXT_TYPE +
                    " )";

    public static final String SQL_DELETE_CONTACTS =
            "DROP TABLE IF EXISTS " + ContactEntry.TABLE_NAME;

    public static long insertContact(Context context, String userID, String username, String email, String publicKey) {
        SQLiteDatabase db = SmartChattingBdHelper.getInstance(context).getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ContactEntry.COLUMN_NAME_CONTACT_ID, userID);
        values.put(ContactEntry.COLUMN_NAME_CONTACT_USERNAME, username);
        values.put(ContactEntry.COLUMN_NAME_CONTACT_EMAIL, email);
        values.put(ContactEntry.COLUMN_NAME_CONTACT_PUBLIC_KEY, publicKey);

        return db.insert(ContactEntry.TABLE_NAME, null, values);
    }

    public static Cursor getContacts(Context context) {
        SQLiteDatabase db = SmartChattingBdHelper.getInstance(context).getReadableDatabase();

        String[] projections = {
                ContactEntry.COLUMN_NAME_CONTACT_ID,
                ContactEntry.COLUMN_NAME_CONTACT_USERNAME,
                ContactEntry.COLUMN_NAME_CONTACT_EMAIL,
                ContactEntry.COLUMN_NAME_CONTACT_PUBLIC_KEY
        };

        String sortOrder =
                ContactEntry.COLUMN_NAME_CONTACT_USERNAME + " ASC";

        return db.query(ContactEntry.TABLE_NAME, projections, null, null, null, null, sortOrder);
    }

    public static String[] getContact(Context context, String username) {
        SQLiteDatabase db = SmartChattingBdHelper.getInstance(context).getReadableDatabase();

        String query = ContactEntry.COLUMN_NAME_CONTACT_USERNAME + "=?";
        String[] values = new String[]{username};

        String[] projections = {
                ContactEntry.COLUMN_NAME_CONTACT_ID,
                ContactEntry.COLUMN_NAME_CONTACT_USERNAME,
                ContactEntry.COLUMN_NAME_CONTACT_EMAIL,
                ContactEntry.COLUMN_NAME_CONTACT_PUBLIC_KEY
        };

        Cursor cursor = db.query(ContactEntry.TABLE_NAME, projections, query, values, null, null, null, null);

        if (cursor == null || !cursor.moveToFirst())
            return null;

        //0 : contact ID
        //1 : username
        //2 : email
        //3 : publicKey
        return new String[]{
                cursor.getString(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3)
        };
    }

    public static void removeContacts(Context context) {
        SQLiteDatabase db = SmartChattingBdHelper.getInstance(context).getWritableDatabase();

        db.delete(ContactEntry.TABLE_NAME, null, null);
    }

    public static void removeContact(Context context, String username) {
        SQLiteDatabase db = SmartChattingBdHelper.getInstance(context).getWritableDatabase();

        db.delete(ContactEntry.TABLE_NAME, ContactEntry.COLUMN_NAME_CONTACT_USERNAME + " = ?", new String[]{username});
    }

    public static abstract class ContactEntry implements BaseColumns {
        public static final String TABLE_NAME = "Contacts";
        public static final String COLUMN_NAME_CONTACT_ID = "userID";
        public static final String COLUMN_NAME_CONTACT_USERNAME = "username";
        public static final String COLUMN_NAME_CONTACT_EMAIL = "email";
        public static final String COLUMN_NAME_CONTACT_PUBLIC_KEY = "publickey";
    }
}
