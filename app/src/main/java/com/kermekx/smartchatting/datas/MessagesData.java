package com.kermekx.smartchatting.datas;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by kermekx on 23/02/2016.
 *
 * Messages data to use in the internal database
 */
public class MessagesData {
    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";

    public static final String SQL_CREATE_MESSAGES =
            "CREATE TABLE " + MessageEntry.TABLE_NAME + " (" +
                    MessageEntry._ID + " INTEGER PRIMARY KEY," +
                    MessageEntry.COLUMN_NAME_CONTACT + TEXT_TYPE + COMMA_SEP +
                    MessageEntry.COLUMN_NAME_MESSAGE_SENT + TEXT_TYPE + COMMA_SEP +
                    MessageEntry.COLUMN_NAME_MESSAGE_CONTENT + TEXT_TYPE +
                    " )";

    public static final String SQL_DELETE_MESSAGES =
            "DROP TABLE IF EXISTS " + MessageEntry.TABLE_NAME;

    public static long insertMessage(Context context, String contact, String isSent, String content) {
        SQLiteDatabase db = SmartChattingBdHelper.getInstance(context).getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MessageEntry.COLUMN_NAME_CONTACT, contact);
        values.put(MessageEntry.COLUMN_NAME_MESSAGE_SENT, isSent);
        values.put(MessageEntry.COLUMN_NAME_MESSAGE_CONTENT, content);

        return db.insert(MessageEntry.TABLE_NAME, null, values);
    }

    public static Cursor getMessages(Context context) {
        SQLiteDatabase db = SmartChattingBdHelper.getInstance(context).getReadableDatabase();

        String[] projections = {
                MessageEntry._ID,
                MessageEntry.COLUMN_NAME_CONTACT,
                MessageEntry.COLUMN_NAME_MESSAGE_SENT,
                MessageEntry.COLUMN_NAME_MESSAGE_CONTENT
        };

        String sortOrder =
                MessageEntry._ID + " ASC";

        return db.query(MessageEntry.TABLE_NAME, projections, null, null, null, null, sortOrder);
    }

    public static void removeMessages(Context context) {
        SQLiteDatabase db = SmartChattingBdHelper.getInstance(context).getWritableDatabase();

        db.delete(MessageEntry.TABLE_NAME, null, null);
    }

    public static abstract class MessageEntry implements BaseColumns {
        public static final String TABLE_NAME = "Messages";
        public static final String COLUMN_NAME_CONTACT = "username";
        public static final String COLUMN_NAME_MESSAGE_SENT = "isSent";
        public static final String COLUMN_NAME_MESSAGE_CONTENT = "content";
    }

}
