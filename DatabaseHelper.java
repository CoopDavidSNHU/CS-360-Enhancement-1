package com.zybooks.project2cs_360;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite access for user accounts and weight entries
 *
 * Changes in this enhancement:
 *  - login returns the account's user ID instead of true/false, so the
 *    session can be built from it
 *  - weight queries returns weight record list instead of a raw Cursor, so the
 *    activity never touches column indexes or leaks cursors
 *  - the string-parsing getWeightDataId() lookup is gone
 *  - guest rows can be cleared as a group
 *  - db.close() is no longer called after every operation, instead SQLiteOpenHelper
 *    stores the connection and closing it invalidates cursors still in use
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "WeightData.db";
    private static final int DATABASE_VERSION = 1;

    // Weight data table and columns
    public static final String TABLE_WEIGHT_DATA = "weight_data";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_WEIGHT_VALUE = "weight_value";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_USER_ID = "user_id";

    // User data table and columns
    public static final String TABLE_USER_DATA = "user_data";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";

    public DatabaseHelper(Context context) {
        // Use the application context so the helper outlives any one activity.
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createWeightTable = "CREATE TABLE " + TABLE_WEIGHT_DATA + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_WEIGHT_VALUE + " TEXT NOT NULL, " +
                COLUMN_DATE + " TEXT NOT NULL, " +
                COLUMN_USER_ID + " INTEGER NOT NULL)";
        db.execSQL(createWeightTable);

        String createUserTable = "CREATE TABLE " + TABLE_USER_DATA + " (" +
                COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USERNAME + " TEXT UNIQUE NOT NULL, " +
                COLUMN_PASSWORD + " TEXT NOT NULL)";
        db.execSQL(createUserTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEIGHT_DATA);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_DATA);
        onCreate(db);
    }

    // ACCOUNTS

    // Registers a new account, Returns false if the username is already taken.
    public boolean registerUser(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            return false;
        }

        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username.trim());
        values.put(COLUMN_PASSWORD, password);

        SQLiteDatabase db = getWritableDatabase();
        long rowId = db.insert(TABLE_USER_DATA, null, values);
        return rowId != -1;
    }

    /**
     * Validates credentials and returns the matching accounts ID.
     *
     * returns the user ID, or no user ID if the login info
     * doesn't match an account
     */
    public int getUserIdForCredentials(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            return UserSession.NO_USER_ID;
        }

        SQLiteDatabase db = getReadableDatabase();
        String selection = COLUMN_USERNAME + " = ? AND " + COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = {username.trim(), password};

        try (Cursor cursor = db.query(TABLE_USER_DATA, new String[]{COLUMN_USER_ID},
                selection, selectionArgs, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID));
            }
        }
        return UserSession.NO_USER_ID;
    }

    // This is kept so any older calling code still compiles.
    public boolean validateUser(String username, String password) {
        return getUserIdForCredentials(username, password) != UserSession.NO_USER_ID;
    }

    //WEIGHT RECORDS

    /**
     * Saves a record under the user ID it carries.
     *
     * returns the new record with its database ID filled in, or null on failure
     */
    public WeightRecord insertWeightRecord(WeightRecord record) {
        if (record == null || isBlank(record.getWeight()) || isBlank(record.getDate())) {
            return null;
        }

        ContentValues values = new ContentValues();
        values.put(COLUMN_WEIGHT_VALUE, record.getWeight().trim());
        values.put(COLUMN_DATE, record.getDate().trim());
        values.put(COLUMN_USER_ID, record.getUserId());

        SQLiteDatabase db = getWritableDatabase();
        long rowId = db.insert(TABLE_WEIGHT_DATA, null, values);
        if (rowId == -1) {
            return null;
        }
        return new WeightRecord((int) rowId, record.getWeight().trim(),
                record.getDate().trim(), record.getUserId());
    }

    //All records belonging to one user (or to the guest ID), newest first
    public List<WeightRecord> getWeightRecords(int userId) {
        List<WeightRecord> records = new ArrayList<>();
        if (userId == UserSession.NO_USER_ID) {
            return records; // No session, no data.
        }

        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_WEIGHT_DATA, null,
                COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)},
                null, null, COLUMN_DATE + " DESC, " + COLUMN_ID + " DESC")) {

            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID);
                int weightIndex = cursor.getColumnIndexOrThrow(COLUMN_WEIGHT_VALUE);
                int dateIndex = cursor.getColumnIndexOrThrow(COLUMN_DATE);
                int userIdIndex = cursor.getColumnIndexOrThrow(COLUMN_USER_ID);

                do {
                    records.add(new WeightRecord(
                            cursor.getInt(idIndex),
                            cursor.getString(weightIndex),
                            cursor.getString(dateIndex),
                            cursor.getInt(userIdIndex)));
                } while (cursor.moveToNext());
            }
        }
        return records;
    }

    /**
     * Deletes one record, but only if it belongs to the current user. The owner
     * check keeps a stale ID from one session from deleting another user's row.
     *
     * returns true if a row was removed
     */
    public boolean deleteWeightRecord(int recordId, int userId) {
        SQLiteDatabase db = getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_WEIGHT_DATA,
                COLUMN_ID + " = ? AND " + COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(recordId), String.valueOf(userId)});
        return rowsDeleted > 0;
    }

    // Removes every row created during guest sessions
    public void clearGuestData() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_WEIGHT_DATA, COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(UserSession.GUEST_USER_ID)});
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}