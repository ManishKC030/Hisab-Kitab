package com.example.hisabkitab;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

public class DatabaseHandler extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "hisabkitab.db";
    private static final int DATABASE_VERSION = 2; // incremented to add "name"

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // USERS TABLE with "name"
        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "firebase_uid TEXT," +
                "email TEXT," +
                "password TEXT," +
                "name TEXT)");

        // EXPENSE TABLE
        db.execSQL("CREATE TABLE expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "firebase_id TEXT," +
                "user_uid TEXT," +
                "title TEXT," +
                "amount REAL," +
                "date TEXT," +
                "synced INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Safe upgrade: add "name" column if not exists
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE users ADD COLUMN name TEXT");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 🔹 Insert user into SQLite
    public long insertUser(String firebaseUid, String email, String password, String name) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("firebase_uid", firebaseUid);
        values.put("email", email);
        values.put("password", password);
        values.put("name", name);

        return db.insert("users", null, values);
    }

    // 🔹 Check user credentials (offline login)
    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM users WHERE email=? AND password=?",
                new String[]{email, password}
        );

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // 🔹 Get username for a given email+password (offline login greeting)
    public String getUsername(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT name FROM users WHERE email=? AND password=?",
                new String[]{email, password}
        );

        String name = null;
        if (cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndex("name"));
        }
        cursor.close();
        return name;
    }

    // 🔹 Insert expense
    public long insertExpense(String firebaseId, String userUid, String title, double amount, String date, int synced) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("firebase_id", firebaseId);
        values.put("user_uid", userUid);
        values.put("title", title);
        values.put("amount", amount);
        values.put("date", date);
        values.put("synced", synced);

        return db.insert("expenses", null, values);
    }

    // 🔹 Get all expenses for a user
    public Cursor getExpenses(String userUid) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM expenses WHERE user_uid=? ORDER BY date DESC",
                new String[]{userUid}
        );
    }
}
