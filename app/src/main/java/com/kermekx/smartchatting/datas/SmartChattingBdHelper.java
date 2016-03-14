package com.kermekx.smartchatting.datas;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by kermekx on 23/02/2016.
 *
 * Database manager
 */
public class SmartChattingBdHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 8;
    public static final String DATABASE_NAME = "SmartChatting.db";

    private static SmartChattingBdHelper instance;

    public SmartChattingBdHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(ContactsData.SQL_CREATE_CONTACTS);
        db.execSQL(MessagesData.SQL_CREATE_MESSAGES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Logger.getLogger(getClass().getName()).log(Level.INFO, "Drop data");
        db.execSQL(ContactsData.SQL_DELETE_CONTACTS);
        db.execSQL(MessagesData.SQL_DELETE_MESSAGES);

        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public static SmartChattingBdHelper getInstance(Context context) {
        if (instance == null) {
            instance = new SmartChattingBdHelper(context);
        }
        return instance;
    }
}
