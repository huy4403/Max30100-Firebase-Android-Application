package com.iot.heartmonitor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NotificationDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "notifications.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME = "notifications";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_MESSAGE = "message";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    public NotificationDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_MESSAGE + " TEXT, " +
                COLUMN_TIMESTAMP + " TEXT)";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void insertNotification(String title, String message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Lấy thời gian hiện tại và định dạng lại
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String formattedTimestamp = sdf.format(new Date());

        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_MESSAGE, message);
        values.put(COLUMN_TIMESTAMP, formattedTimestamp);

        db.insert(TABLE_NAME, null, values);
        db.close();
    }


    public List<String> getAllNotifications() {
        List<String> notifications = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, COLUMN_TIMESTAMP + " DESC");

        if (cursor != null) {
            int titleColumnIndex = cursor.getColumnIndex(COLUMN_TITLE);
            int messageColumnIndex = cursor.getColumnIndex(COLUMN_MESSAGE);
            int timestampColumnIndex = cursor.getColumnIndex(COLUMN_TIMESTAMP);

            if (titleColumnIndex >= 0 && messageColumnIndex >= 0 && timestampColumnIndex >= 0) {
                while (cursor.moveToNext()) {
                    String title = cursor.getString(titleColumnIndex);
                    String message = cursor.getString(messageColumnIndex);
                    String timestamp = cursor.getString(timestampColumnIndex);

                    notifications.add("[" + timestamp + "] " + title + ": " + message);
                }
            }
            cursor.close();
        }
        db.close();
        return notifications;
    }


    public void deleteAllNotifications() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
        db.close();
    }
}
