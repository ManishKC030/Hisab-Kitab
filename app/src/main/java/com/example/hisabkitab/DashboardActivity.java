package com.example.hisabkitab;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.*;
import android.database.Cursor;
import android.graphics.Color;
import android.view.ViewGroup;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.*;

public class DashboardActivity extends AppCompatActivity {

    LinearLayout navBtnHome, navBtnAnalytics, navBtnStatement, navBtnAccount, transactionContainer;

    Button btnAddIncome, btnAddExpense;
    Button btnFilterAll, btnFilterIncome, btnFilterExpense;

    TextView tvHelloName, tvBalance, tvIncome, tvExpense, tvSyncStatus;

    DatabaseHandler db;

    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    String currentUserUid;

    SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);

        // Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            new SessionManager(this).clearSession();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        currentUserUid = currentUser.getUid();

        // Bind UI
        navBtnHome = findViewById(R.id.navBtnHome);
        navBtnAnalytics = findViewById(R.id.navBtnAnalytics);
        navBtnStatement = findViewById(R.id.navBtnStatement);
        navBtnAccount = findViewById(R.id.navBtnAccount);
        transactionContainer = findViewById(R.id.transactionContainer);

        // Set active tab color
        int activeColor = ContextCompat.getColor(this, R.color.colorPrimary);
        ((ImageView)findViewById(R.id.navIconHome)).setColorFilter(activeColor);
        ((TextView)findViewById(R.id.navTextHome)).setTextColor(activeColor);

        btnAddIncome = findViewById(R.id.btnAddIncome);
        btnAddExpense = findViewById(R.id.btnAddExpense);

        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterIncome = findViewById(R.id.btnFilterIncome);
        btnFilterExpense = findViewById(R.id.btnFilterExpense);

        tvHelloName = findViewById(R.id.tvHelloName);
        tvBalance = findViewById(R.id.tvBalance);
        tvIncome = findViewById(R.id.tvIncome);
        tvExpense = findViewById(R.id.tvExpense);
        tvSyncStatus = findViewById(R.id.tvSyncStatus);

        db = new DatabaseHandler(this);


        // Only insert sample data once
        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        boolean dataInserted = prefs.getBoolean("sample_data_inserted_v4", false); // Updated to v4

        if (!dataInserted && currentUserUid != null) {
            db.insertSampleData(currentUserUid);
            prefs.edit().putBoolean("sample_data_inserted_v4", true).apply();
        }

        // Sync
        SyncManager.syncData(this);

        setUserName();

        // 🔔 NOTIFICATION SETUP
        checkNotificationPermission();

        loadAllTransactions();

        // Navigation
        navBtnHome.setOnClickListener(v -> {
            // Already here
        });

        navBtnStatement.setOnClickListener(v ->
                startActivity(new Intent(this, StatementActivity.class)));

        navBtnAnalytics.setOnClickListener(v ->
                startActivity(new Intent(this, AnalyticsActivity.class)));

        navBtnAccount.setOnClickListener(v ->
                startActivity(new Intent(this, AccountActivity.class)));

        // Add transaction
        btnAddIncome.setOnClickListener(v ->
                startActivity(new Intent(this, AddIncomeActivity.class)));

        btnAddExpense.setOnClickListener(v ->
                startActivity(new Intent(this, AddExpenseActivity.class)));

        // Filters
        btnFilterAll.setOnClickListener(v -> {
            setFilterUI(0);
            loadAllTransactions();
        });

        btnFilterIncome.setOnClickListener(v -> {
            setFilterUI(1);
            loadIncomeOnly();
        });

        btnFilterExpense.setOnClickListener(v -> {
            setFilterUI(2);
            loadExpenseOnly();
        });
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            } else {
                scheduleInitialNotification();
            }
        } else {
            scheduleInitialNotification();
        }
    }

    private void scheduleInitialNotification() {
        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        boolean isScheduled = prefs.getBoolean("notification_set", false);
        if (!isScheduled) {
            scheduleNotificationNow();
            prefs.edit().putBoolean("notification_set", true).apply();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scheduleInitialNotification();
            }
        }
    }

    // ==================== NOTIFICATION ====================
//    private void scheduleDailyNotification() {
//        Intent intent = new Intent(this, ReminderReceiver.class);
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(
//                this, 0, intent,
//                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );
//
//        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
//
//        Calendar calendar = Calendar.getInstance();
//        calendar.set(Calendar.HOUR_OF_DAY, 20); // 8 PM
//        calendar.set(Calendar.MINUTE, 0);
//        calendar.set(Calendar.SECOND, 0);
//
//        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
//            calendar.add(Calendar.DAY_OF_MONTH, 1);
//        }
//
//        // Use setExactAndAllowWhileIdle for precise daily alarm
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            alarmManager.setExactAndAllowWhileIdle(
//                    AlarmManager.RTC_WAKEUP,
//                    calendar.getTimeInMillis(),
//                    pendingIntent
//            );
//        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            alarmManager.setExact(
//                    AlarmManager.RTC_WAKEUP,
//                    calendar.getTimeInMillis(),
//                    pendingIntent
//            );
//        } else {
//            alarmManager.set(
//                    AlarmManager.RTC_WAKEUP,
//                    calendar.getTimeInMillis(),
//                    pendingIntent
//            );
//        }
//    }

//Sechuling notification after 10 seconds
    private void scheduleNotificationNow() {
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, 10); // trigger after 10 seconds

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        } else {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        }
    }
    // ==================== USER NAME ====================
    private void setUserName() {
        if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
            tvHelloName.setText("Hello, " + currentUser.getDisplayName());
        } else if (currentUser.getEmail() != null) {
            String emailName = currentUser.getEmail().split("@")[0];
            tvHelloName.setText("Hello, " + emailName);
        } else {
            tvHelloName.setText("Hello, User");
        }
    }

    private void setFilterUI(int selected) {
        int white = Color.WHITE;
        int darkGray = Color.DKGRAY;

        btnFilterAll.setBackgroundResource(selected == 0 ? R.drawable.filter_selected : R.drawable.filter_unselected);
        btnFilterAll.setTextColor(selected == 0 ? white : darkGray);

        btnFilterIncome.setBackgroundResource(selected == 1 ? R.drawable.filter_selected : R.drawable.filter_unselected);
        btnFilterIncome.setTextColor(selected == 1 ? white : darkGray);

        btnFilterExpense.setBackgroundResource(selected == 2 ? R.drawable.filter_selected : R.drawable.filter_unselected);
        btnFilterExpense.setTextColor(selected == 2 ? white : darkGray);
    }

    // ==================== FORMAT CURRENCY ====================
    private String formatRupee(double amount) {
        // Use Indian locale for Rs formatting
        java.text.NumberFormat formatter = java.text.NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        String result = formatter.format(amount);
        return result.replace("₹", "Rs ");
    }

    // ==================== LOAD TRANSACTIONS ====================
    private void loadAllTransactions() {
        transactionContainer.removeAllViews();

        List<TransactionItem> allTransactions = new ArrayList<>();

        double totalIncome = 0;
        double totalExpense = 0;
        boolean hasUnsynced = false;

        Cursor incomeCursor = db.getIncome(currentUserUid);
        if (incomeCursor != null) {
            while (incomeCursor.moveToNext()) {
                String title = incomeCursor.getString(incomeCursor.getColumnIndexOrThrow("title"));
                double amount = incomeCursor.getDouble(incomeCursor.getColumnIndexOrThrow("amount"));
                String date = incomeCursor.getString(incomeCursor.getColumnIndexOrThrow("date"));
                int synced = incomeCursor.getInt(incomeCursor.getColumnIndexOrThrow("synced"));
                String category = incomeCursor.getString(incomeCursor.getColumnIndexOrThrow("category"));

                totalIncome += amount;
                if (synced == 0) hasUnsynced = true;

                allTransactions.add(new TransactionItem(title, amount, date, true, synced, category));
            }
            incomeCursor.close();
        }

        Cursor expenseCursor = db.getExpenses(currentUserUid);
        if (expenseCursor != null) {
            while (expenseCursor.moveToNext()) {
                String title = expenseCursor.getString(expenseCursor.getColumnIndexOrThrow("title"));
                double amount = expenseCursor.getDouble(expenseCursor.getColumnIndexOrThrow("amount"));
                String date = expenseCursor.getString(expenseCursor.getColumnIndexOrThrow("date"));
                int synced = expenseCursor.getInt(expenseCursor.getColumnIndexOrThrow("synced"));
                String category = expenseCursor.getString(expenseCursor.getColumnIndexOrThrow("category"));

                totalExpense += amount;
                if (synced == 0) hasUnsynced = true;

                allTransactions.add(new TransactionItem(title, amount, date, false, synced, category));
            }
            expenseCursor.close();
        }

        sortTransactions(allTransactions);

        // Limit dashboard to 7-8 latest transactions as requested
        int showCount = Math.min(allTransactions.size(), 7);
        for (int i = 0; i < showCount; i++) {
            TransactionItem tx = allTransactions.get(i);
            addTransactionView(tx.title, tx.amount, tx.date, tx.category, tx.isIncome);
        }

        double balance = totalIncome - totalExpense;

        tvIncome.setText("+ " + formatRupee(totalIncome));
        tvExpense.setText("- " + formatRupee(totalExpense));
        tvBalance.setText(formatRupee(balance));

        if (hasUnsynced) {
            tvSyncStatus.setText("Not Synced");
            tvSyncStatus.setTextColor(Color.RED);
        } else {
            tvSyncStatus.setText("All Synced");
            tvSyncStatus.setTextColor(Color.WHITE);
        }
    }

    private void loadIncomeOnly() {
        transactionContainer.removeAllViews();
        List<TransactionItem> list = new ArrayList<>();

        Cursor cursor = db.getIncome(currentUserUid);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                int synced = cursor.getInt(cursor.getColumnIndexOrThrow("synced"));
                String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));

                list.add(new TransactionItem(title, amount, date, true, synced, category));
            }
            cursor.close();
        }

        sortTransactions(list);

        for (TransactionItem tx : list) {
            addTransactionView(tx.title, tx.amount, tx.date, tx.category, true);
        }
    }

    private void loadExpenseOnly() {
        transactionContainer.removeAllViews();
        List<TransactionItem> list = new ArrayList<>();

        Cursor cursor = db.getExpenses(currentUserUid);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                int synced = cursor.getInt(cursor.getColumnIndexOrThrow("synced"));
                String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));

                list.add(new TransactionItem(title, amount, date, false, synced, category));
            }
            cursor.close();
        }

        sortTransactions(list);

        for (TransactionItem tx : list) {
            addTransactionView(tx.title, tx.amount, tx.date, tx.category, false);
        }
    }

    private void sortTransactions(List<TransactionItem> list) {
        Collections.sort(list, (a, b) -> {
            try {
                SimpleDateFormat parser = new SimpleDateFormat("d/M/yyyy", Locale.US);
                Date d1 = parser.parse(a.date);
                Date d2 = parser.parse(b.date);
                if (d1 == null || d2 == null) return 0;
                return d2.compareTo(d1); // Latest first
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        });
    }

    private void addTransactionView(String title, double amount, String date, String category, boolean isIncome) {
        View view = getLayoutInflater().inflate(R.layout.item_transaction, transactionContainer, false);

        TextView tvTitle = view.findViewById(R.id.txtTitle);
        TextView tvCategory = view.findViewById(R.id.txtCategory);
        TextView tvDate = view.findViewById(R.id.txtDate);
        TextView tvAmount = view.findViewById(R.id.txtAmount);

        tvTitle.setText(title);
        tvCategory.setText(category); 
        tvDate.setText(date);
        
        if (isIncome) {
            tvAmount.setText("+ " + formatRupee(amount));
            tvAmount.setTextColor(ContextCompat.getColor(this, R.color.income_green));
        } else {
            tvAmount.setText("- " + formatRupee(amount));
            tvAmount.setTextColor(ContextCompat.getColor(this, R.color.expense_red));
        }

        transactionContainer.addView(view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SyncManager.syncData(this);
        loadAllTransactions();
    }
}