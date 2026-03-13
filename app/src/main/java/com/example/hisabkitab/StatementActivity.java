package com.example.hisabkitab;

import android.os.Bundle;
import android.database.Cursor;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.components.XAxis;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.*;

public class StatementActivity extends AppCompatActivity {

    LineChart lineChart;
    RecyclerView recycler;

    Spinner spinnerDate, spinnerCategory;

    DatabaseHandler db;

    List<TransactionItem> transactions = new ArrayList<>();
    TransactionAdapter adapter;

    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    String userUid; // will hold actual Firebase UID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.statement);

        // Initialize Firebase auth
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            finish(); // exit if user not logged in
            return;
        }

        userUid = currentUser.getUid(); // correct UID

        db = new DatabaseHandler(this);

        lineChart = findViewById(R.id.lineChart);
        recycler = findViewById(R.id.recyclerTransactions);

        spinnerDate = findViewById(R.id.spinnerDate);
        spinnerCategory = findViewById(R.id.spinnerCategory);

        // Setup RecyclerView
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(transactions);
        recycler.setAdapter(adapter);

        setupSpinners();

        loadGraph();
        loadRecentTransactions();
    }

    // Setup date and category filters
    void setupSpinners() {
        String[] dateFilters = {"Last 7 Days", "Last 30 Days", "All Time"};
        String[] categories = {"All", "Food", "Transport", "Shopping", "Salary", "Bills"};

        spinnerDate.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, dateFilters));

        spinnerCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, categories));
    }

    // Load Income & Expense chart
    void loadGraph() {
        List<Entry> incomeEntries = new ArrayList<>();
        List<Entry> expenseEntries = new ArrayList<>();

        float index = 1;

        Cursor income = db.getIncome(userUid);
        if (income != null) {
            while (income.moveToNext()) {
                double amount = income.getDouble(income.getColumnIndexOrThrow("amount"));
                incomeEntries.add(new Entry(index, (float) amount));
                index++;
            }
            income.close();
        }

        index = 1;
        Cursor expense = db.getExpenses(userUid);
        if (expense != null) {
            while (expense.moveToNext()) {
                double amount = expense.getDouble(expense.getColumnIndexOrThrow("amount"));
                expenseEntries.add(new Entry(index, (float) amount));
                index++;
            }
            expense.close();
        }

        LineDataSet incomeSet = new LineDataSet(incomeEntries, "Income");
        incomeSet.setColor(0xFF2ECC71);
        incomeSet.setCircleColor(0xFF2ECC71);

        LineDataSet expenseSet = new LineDataSet(expenseEntries, "Expense");
        expenseSet.setColor(0xFFE74C3C);
        expenseSet.setCircleColor(0xFFE74C3C);

        LineData data = new LineData(incomeSet, expenseSet);

        lineChart.setData(data);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.invalidate();
    }

    // Load recent transactions into RecyclerView
    void loadRecentTransactions() {
        transactions.clear();

        // Load income
        Cursor income = db.getIncome(userUid);
        if (income != null) {
            while (income.moveToNext()) {
                String title = income.getString(income.getColumnIndexOrThrow("title"));
                String category = income.getString(income.getColumnIndexOrThrow("category"));
                double amount = income.getDouble(income.getColumnIndexOrThrow("amount"));
                String date = income.getString(income.getColumnIndexOrThrow("date"));
                int synced = income.getInt(income.getColumnIndexOrThrow("synced"));

                transactions.add(new TransactionItem(title, amount, date, true, synced, category));
            }
            income.close();
        }

        // Load expense
        Cursor expense = db.getExpenses(userUid);
        if (expense != null) {
            while (expense.moveToNext()) {
                String title = expense.getString(expense.getColumnIndexOrThrow("title"));
                String category = expense.getString(expense.getColumnIndexOrThrow("category"));
                double amount = expense.getDouble(expense.getColumnIndexOrThrow("amount"));
                String date = expense.getString(expense.getColumnIndexOrThrow("date"));
                int synced = expense.getInt(expense.getColumnIndexOrThrow("synced"));

                transactions.add(new TransactionItem(title, amount, date, false, synced, category));
            }
            expense.close();
        }

        // Sort by date descending
        Collections.sort(transactions, (a, b) -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
                Date d1 = sdf.parse(a.date);
                Date d2 = sdf.parse(b.date);
                return d2.compareTo(d1);
            } catch (Exception e) {
                return 0;
            }
        });

        adapter.notifyDataSetChanged(); // update RecyclerView
    }
}