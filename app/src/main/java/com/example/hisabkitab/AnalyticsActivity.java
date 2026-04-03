package com.example.hisabkitab;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.*;
import android.graphics.Color;
import java.text.SimpleDateFormat;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.*;

public class AnalyticsActivity extends AppCompatActivity {
    LinearLayout navBtnHome, navBtnAnalytics, navBtnStatement, navBtnAccount;
    PieChart pieChart;
    BarChart barChart;

    Button btnIncome, btnExpense;

    TextView txtPrediction, txtInsights;

    DatabaseHandler db;

    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    String userUid;
    View incomeIndicator, expenseIndicator;

    boolean showIncome = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.analytics);
        // Initialize Firebase auth
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            finish(); // exit if user not logged in
            return;
        }

        userUid = currentUser.getUid();

        // Bind UI
        navBtnHome = findViewById(R.id.navBtnHome);
        navBtnAnalytics = findViewById(R.id.navBtnAnalytics);
        navBtnStatement = findViewById(R.id.navBtnStatement);
        navBtnAccount = findViewById(R.id.navBtnAccount);

        incomeIndicator = findViewById(R.id.incomeIndicator);
        expenseIndicator = findViewById(R.id.expenseIndicator);

        // Set active tab color
        int activeColor = androidx.core.content.ContextCompat.getColor(this, R.color.colorPrimary);
        ((ImageView)findViewById(R.id.navIconAnalytics)).setColorFilter(activeColor);
        ((TextView)findViewById(R.id.navTextAnalytics)).setTextColor(activeColor);

        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);

        btnIncome = findViewById(R.id.btnIncome);
        btnExpense = findViewById(R.id.btnExpense);

        txtPrediction = findViewById(R.id.txtPrediction);
        txtInsights = findViewById(R.id.txtInsights);

        db = new DatabaseHandler(this);
        // Navigation
        navBtnAnalytics.setOnClickListener(v -> {
            // Already here
        });

        navBtnStatement.setOnClickListener(v ->
                startActivity(new Intent(this, StatementActivity.class)));

        navBtnHome.setOnClickListener(v ->
                startActivity(new Intent(this, DashboardActivity.class)));

        navBtnAccount.setOnClickListener(v ->
                startActivity(new Intent(this, AccountActivity.class)));


        btnIncome.setOnClickListener(v -> {

            showIncome = true;
            loadPieChart();

            incomeIndicator.setVisibility(View.VISIBLE);
            expenseIndicator.setVisibility(View.INVISIBLE);
        });

        btnExpense.setOnClickListener(v -> {

            showIncome = false;
            loadPieChart();

            incomeIndicator.setVisibility(View.INVISIBLE);
            expenseIndicator.setVisibility(View.VISIBLE);
        });
        loadPieChart();
        loadBarChart();
        calculatePrediction();
        generateInsights();
    }

    void loadPieChart() {
        if (userUid == null) return;
        Map<String, Float> categoryMap = new HashMap<>();

        Cursor cursor;
        if (showIncome)
            cursor = db.getIncome(userUid);
        else
            cursor = db.getExpenses(userUid);

        if (cursor == null) return;

        while (cursor.moveToNext()) {
            String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
            float amount = cursor.getFloat(cursor.getColumnIndexOrThrow("amount"));

            categoryMap.put(category, categoryMap.getOrDefault(category, 0f) + amount);
        }
        cursor.close();

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : categoryMap.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        
        // Dynamic colors for better visualization
        int[] colors = {
                0xFF2ECC71, // Emerald
                0xFF3498DB, // Peter River
                0xFF9B59B6, // Amethyst
                0xFFF1C40F, // Sun Flower
                0xFFE67E22, // Carrot
                0xFFE74C3C, // Alizarin
                0xFF1ABC9C, // Turquoise
                0xFF34495E  // Wet Asphalt
        };
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText(showIncome ? "Income Distribution" : "Expense Distribution");
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    void loadBarChart() {
        if (userUid == null) return;
        Map<String, Float> monthMap = new TreeMap<>((o1, o2) -> {
            try {
                SimpleDateFormat monthFormat = new SimpleDateFormat("MMM yyyy", Locale.US);
                return monthFormat.parse(o1).compareTo(monthFormat.parse(o2));
            } catch (Exception e) {
                return o1.compareTo(o2);
            }
        });

        Cursor cursor = db.getExpenses(userUid);
        if (cursor == null) return;

        SimpleDateFormat inputFormat = new SimpleDateFormat("d/M/yyyy", Locale.US);
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM yyyy", Locale.US);

        while (cursor.moveToNext()) {
            try {
                String dateStr = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                float amount = cursor.getFloat(cursor.getColumnIndexOrThrow("amount"));
                
                Date date = inputFormat.parse(dateStr);
                String monthKey = monthFormat.format(date);

                monthMap.put(monthKey, monthMap.getOrDefault(monthKey, 0f) + amount);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        cursor.close();

        List<BarEntry> entries = new ArrayList<>();
        int index = 0;
        for (float val : monthMap.values()) {
            entries.add(new BarEntry(index++, val));
        }

        BarDataSet set = new BarDataSet(entries, "Monthly Expenses (NPR)");
        set.setColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorPrimary));
        set.setValueTextSize(10f);

        BarData data = new BarData(set);
        barChart.setData(data);
        barChart.getXAxis().setLabelCount(monthMap.size());
        barChart.getDescription().setEnabled(false);
        barChart.animateX(1000);
        barChart.invalidate();
    }

    void calculatePrediction() {
        if (userUid == null) return;
        Cursor cursor = db.getExpenses(userUid);
        if (cursor == null) return;

        Map<String, Float> monthMap = new HashMap<>();
        SimpleDateFormat inputFormat = new SimpleDateFormat("d/M/yyyy", Locale.US);
        SimpleDateFormat monthFormat = new SimpleDateFormat("M/yyyy", Locale.US);

        while (cursor.moveToNext()) {
            try {
                String dateStr = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                float amount = cursor.getFloat(cursor.getColumnIndexOrThrow("amount"));
                Date date = inputFormat.parse(dateStr);
                String monthKey = monthFormat.format(date);

                monthMap.put(monthKey, monthMap.getOrDefault(monthKey, 0f) + amount);
            } catch (Exception e) { }
        }
        cursor.close();

        if (monthMap.isEmpty()) {
            txtPrediction.setText("Rs 0.00");
            return;
        }

        float total = 0;

        for(Float val : monthMap.values())
            total += val;

        float prediction = total / monthMap.size();

        txtPrediction.setText(String.format(Locale.getDefault(), "Rs %.2f", prediction));
    }

    void generateInsights(){
        if (userUid == null) return;
        Cursor cursor = db.getExpenses(userUid);
        if (cursor == null) return;

        Map<String, Float> categoryMap = new HashMap<>();

        while(cursor.moveToNext()){

            String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
            float amount = cursor.getFloat(cursor.getColumnIndexOrThrow("amount"));

            if(categoryMap.containsKey(category))
                categoryMap.put(category, categoryMap.get(category)+amount);
            else
                categoryMap.put(category, amount);
        }
        cursor.close();

        if (categoryMap.isEmpty()) {
            txtInsights.setText("No expense data available for insights.");
            return;
        }

        String topCategory = "";
        float max = 0;

        for(String key : categoryMap.keySet()){

            if(categoryMap.get(key) > max){
                max = categoryMap.get(key);
                topCategory = key;
            }
        }

        txtInsights.setText(
                "Highest spending category: "+topCategory+
                        "\nTotal spent: Rs "+String.format(Locale.getDefault(), "%.2f", max)
        );
    }
}