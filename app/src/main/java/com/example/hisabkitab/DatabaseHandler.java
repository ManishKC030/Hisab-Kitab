package com.example.hisabkitab;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

public class DatabaseHandler extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "hisabkitab.db";
    private static final int DATABASE_VERSION = 4; // upgraded for income table

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // USERS TABLE
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
                "category TEXT," +
                "description TEXT," +
                "date TEXT," +
                "synced INTEGER DEFAULT 0 )");

        // INCOME TABLE
        db.execSQL("CREATE TABLE income (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "firebase_id TEXT," +
                "user_uid TEXT," +
                "title TEXT," +
                "amount REAL," +
                "category TEXT," +
                "description TEXT," +
                "date TEXT," +
                "synced INTEGER DEFAULT 0 )");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE users ADD COLUMN name TEXT");
            } catch (Exception ignored) {}
        }

        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE expenses ADD COLUMN category TEXT");
            } catch (Exception ignored) {}

            try {
                db.execSQL("ALTER TABLE expenses ADD COLUMN description TEXT");
            } catch (Exception ignored) {}
        }

        // 🔥 VERSION 4 → CREATE INCOME TABLE
        if (oldVersi
