package com.example.hisabkitab;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

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
    public void insertSampleData(String userUid) {
        SQLiteDatabase db = this.getWritableDatabase();
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.US);
        
        // {DaysOffset, Title, Category, Type, Amount}
        Object[][] sampleData = {
                // --- MONTH 0 (Current) ---
                {0, "Salary - Month 0", "Salary", "Income", 85000.0},
                {-2, "Grocery Shopping", "Food", "Expense", 4500.0},
                {-5, "Electricity Bill", "Bills", "Expense", 2200.0},
                {-10, "Internet Bill", "Bills", "Expense", 1600.0},
                {-15, "Freelance Gig", "Freelance", "Income", 12000.0},
                
                // --- MONTH 1 ---
                {-32, "Salary - Month 1", "Salary", "Income", 85000.0},
                {-35, "House Rent", "Bills", "Expense", 20000.0},
                {-40, "Fuel", "Transport", "Expense", 3500.0},
                {-45, "Dining Out", "Food", "Expense", 2800.0},
                {-50, "Online Course", "Other", "Expense", 5000.0},

                // --- MONTH 2 ---
                {-62, "Salary - Month 2", "Salary", "Income", 85000.0},
                {-65, "Shopping", "Shopping", "Expense", 7000.0},
                {-70, "Water Bill", "Bills", "Expense", 600.0},
                {-75, "Medical Checkup", "Health", "Expense", 1500.0},
                {-80, "Investment Dividend", "Investment", "Income", 3000.0},

                // --- MONTH 3 ---
                {-92, "Salary - Month 3", "Salary", "Income", 85000.0},
                {-95, "Insurance Premium", "Bills", "Expense", 12000.0},
                {-100, "New Shoes", "Shopping", "Expense", 4500.0},
                {-110, "Gift for Friend", "Gift", "Expense", 2000.0},

                // --- MONTH 4 ---
                {-122, "Salary - Month 4", "Salary", "Income", 82000.0},
                {-125, "Car Repair", "Transport", "Expense", 15000.0},
                {-130, "Grocery", "Food", "Expense", 5000.0},

                // --- MONTH 5 ---
                {-152, "Salary - Month 5", "Salary", "Income", 82000.0},
                {-155, "Vacation Trip", "Other", "Expense", 30000.0},
                {-160, "Restaurant", "Food", "Expense", 4000.0}
        };

        for (Object[] item : sampleData) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, (int) item[0]);
            String dateStr = sdf.format(cal.getTime());

            ContentValues values = new ContentValues();
            values.put("user_uid", userUid);
            values.put("date", dateStr);
            values.put("title", (String) item[1]);
            values.put("category", (String) item[2]);
            values.put("amount", (Double) item[4]);
            values.put("description", "Expanded realistic sample data.");
            values.put("synced", 1);

            String table = ((String) item[3]).equalsIgnoreCase("Income") ? "income" : "expenses";
            db.insert(table, null, values);
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
