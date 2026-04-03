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
    public void insertSampleData() {
        String[][] sampleTransactions = {
                // {date, title, category, type, amount}
                {"2026-01-02", "Salary", "Income", "Income", "50000"},
                {"2026-01-03", "Food", "Food", "Expense", "450"},
                {"2026-01-03", "Bus fare", "Transport", "Expense", "300"},
                {"2026-01-04", "Electricity Bill", "Utilities", "Expense", "1200"},
                {"2026-01-05", "Freelance Project", "Freelance", "Income", "12000"},
                {"2026-01-06", "Shopping", "Shopping", "Expense", "2500"},
                {"2026-01-07", "Movie Night", "Entertainment", "Expense", "1000"},
                {"2026-01-10", "Gift from Friend", "Gift", "Income", "5000"},
                {"2026-01-12", "Lunch", "Food", "Expense", "600"},
                {"2026-01-15", "Bus fare", "Transport", "Expense", "350"}
                // ...add more entries for Feb, Mar, Apr
        };

        for (String[] tx : sampleTransactions) {
            ContentValues values = new ContentValues();
            values.put("date", tx[0]);
            values.put("title", tx[1]);
            values.put("category", tx[2]);
            values.put("type", tx[3]);
            values.put("amount", Double.parseDouble(tx[4]));
            values.put("synced", 1); // default synced

            SQLiteDatabase db = this.getWritableDatabase();
            db.insert("transactions", null, values);
        }
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
        if (oldVersion < 4) {
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS income (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "firebase_id TEXT," +
                        "user_uid TEXT," +
                        "title TEXT," +
                        "amount REAL," +
                        "category TEXT," +
                        "description TEXT," +
                        "date TEXT," +
                        "synced INTEGER DEFAULT 0 )");
            } catch (Exception ignored) {}
        }
    }

    // ===========================
    // 🔹 USER METHODS
    // ===========================

    public long insertUser(String firebaseUid, String email, String password, String name) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("firebase_uid", firebaseUid);
        values.put("email", email);
        values.put("password", password);
        values.put("name", name);

        return db.insert("users", null, values);
    }

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

    public String getUsername(String email, String password) {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT name FROM users WHERE email=? AND password=?",
                new String[]{email, password}
        );

        String name = null;

        if (cursor.moveToFirst()) {
            int index = cursor.getColumnIndex("name");
            if (index != -1) {
                name = cursor.getString(index);
            }
        }

        cursor.close();
        return name;
    }
    public void deleteUser(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("users", "email=?", new String[]{email});
        db.close();
    }

    public void clearUserData(String userUid) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("expenses", "user_uid=?", new String[]{userUid});
        db.delete("income", "user_uid=?", new String[]{userUid});
        db.delete("users", "firebase_uid=?", new String[]{userUid});
        db.close();
    }

    // ===========================
    // 🔹 EXPENSE METHODS
    // ===========================

    public long insertExpense(String firebaseId,
                              String userUid,
                              String title,
                              double amount,
                              String category,
                              String description,
                              String date,
                              int synced) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("firebase_id", firebaseId);
        values.put("user_uid", userUid);
        values.put("title", title);
        values.put("amount", amount);
        values.put("category", category);
        values.put("description", description);
        values.put("date", date);
        values.put("synced", synced);

        return db.insert("expenses", null, values);
    }

    public Cursor getExpenses(String userUid) {
        if (userUid == null) return null;
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM expenses WHERE user_uid=? ORDER BY date DESC",
                new String[]{userUid}
        );
    }

    public Cursor getUnsyncedExpenses() {

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM expenses WHERE synced=0",
                null
        );
    }

    public void markExpenseAsSynced(int id, String firebaseId) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("synced", 1);
        values.put("firebase_id", firebaseId);

        db.update("expenses", values, "id=?",
                new String[]{String.valueOf(id)});
    }

    // ===========================
    // 🔹 INCOME METHODS
    // ===========================

    public long insertIncome(String firebaseId,
                             String userUid,
                             String title,
                             double amount,
                             String category,
                             String description,
                             String date,
                             int synced) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("firebase_id", firebaseId);
        values.put("user_uid", userUid);
        values.put("title", title);
        values.put("amount", amount);
        values.put("category", category);
        values.put("description", description);
        values.put("date", date);
        values.put("synced", synced);

        return db.insert("income", null, values);
    }

    public Cursor getIncome(String userUid) {
        if (userUid == null) return null;
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM income WHERE user_uid=? ORDER BY date DESC",
                new String[]{userUid}
        );
    }

    public Cursor getUnsyncedIncome() {

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM income WHERE synced=0",
                null
        );
    }

    public void markIncomeAsSynced(int id, String firebaseId) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("synced", 1);
        values.put("firebase_id", firebaseId);

        db.update("income", values, "id=?",
                new String[]{String.valueOf(id)});
    }
}
