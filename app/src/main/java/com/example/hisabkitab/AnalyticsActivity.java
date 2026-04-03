package com.example.hisabkitab;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;

public class AnalyticsActivity extends AppCompatActivity {

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

        // Firebase initialization
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) { finish(); return; }
        userUid = currentUser.getUid();

        // UI references
        incomeIndicator = findViewById(R.id.incomeIndicator);
        expenseIndicator = findViewById(R.id.expenseIndicator);
        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);
        btnIncome = findViewById(R.id.btnIncome);
        btnExpense = findViewById(R.id.btnExpense);
        txtPrediction = findViewById(R.id.txtPrediction);
        txtInsights = findViewById(R.id.txtInsights);

        db = new DatabaseHandler(this);

        // Button toggles
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

    // Setup PieChart style
    private void setupPieChart() {
        pieChart.setUsePercentValues(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleRadius(50f);
        pieChart.setCenterText(showIncome ? "Income" : "Expense");
        pieChart.setCenterTextSize(20f);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setCenterTextColor(Color.BLACK);
        pieChart.getLegend().setTextColor(Color.DKGRAY);
    }

    // Load PieChart data
    void loadPieChart() {
        setupPieChart();

        Map<String, Float> categoryMap = new HashMap<>();
        Cursor cursor = showIncome ? db.getIncome(userUid) : db.getExpenses(userUid);

        while(cursor.moveToNext()){
            String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
            float amount = cursor.getFloat(cursor.getColumnIndexOrThrow("amount"));
            categoryMap.put(category, categoryMap.getOrDefault(category, 0f) + amount);
        }

        List<PieEntry> entries = new ArrayList<>();
        for(String key : categoryMap.keySet())
            entries.add(new PieEntry(categoryMap.get(key), key));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(
                Color.parseColor("#2ECC71"),
                Color.parseColor("#3498DB"),
                Color.parseColor("#F1C40F"),
                Color.parseColor("#E67E22"),
                Color.parseColor("#E74C3C")
        );
        dataSet.setSliceSpace(2f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.invalidate();
    }

    // Load BarChart for monthly spending trend
    void loadBarChart() {
        Map<String, Float> monthMap = new TreeMap<>();
        Cursor cursor = db.getExpenses(userUid);

        while(cursor.moveToNext()){
            String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
            float amount = cursor.getFloat(cursor.getColumnIndexOrThrow("amount"));
            String month = date.substring(0,7);
            monthMap.put(month, monthMap.getOrDefault(month,0f)+amount);
        }

        List<BarEntry> entries = new ArrayList<>();
        int index = 0;
        for(String key : monthMap.keySet()){
            entries.add(new BarEntry(index, monthMap.get(key)));
            index++;
        }

        BarDataSet set = new BarDataSet(entries,"Monthly Expense");
        set.setColor(Color.parseColor("#2ECC71"));
        BarData data = new BarData(set);
        data.setBarWidth(0.6f);

        barChart.setData(data);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setLabelRotationAngle(-45);
        barChart.getAxisLeft().setTextColor(Color.DKGRAY);
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setTextColor(Color.DKGRAY);
        barChart.invalidate();
    }

    // Calculate predicted next month expense using Linear Regression + Moving Average
    void calculatePrediction() {
        Cursor cursor = db.getExpenses(userUid);
        Map<String, Float> monthMap = new TreeMap<>();

        while(cursor.moveToNext()){
            String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
            float amount = cursor.getFloat(cursor.getColumnIndexOrThrow("amount"));
            String month = date.substring(0,7);
            monthMap.put(month, monthMap.getOrDefault(month,0f)+amount);
        }

        // Moving Average
        List<Float> values = new ArrayList<>(monthMap.values());
        float movingAvg = 0;
        if(values.size() >= 3) {
            movingAvg = (values.get(values.size()-1) + values.get(values.size()-2) + values.get(values.size()-3))/3;
        } else {
            float sum = 0; for(float v: values) sum+=v; movingAvg = sum/values.size();
        }

        // Linear Regression
        float sumX=0, sumY=0, sumXY=0, sumX2=0;
        int n = values.size();
        for(int i=0;i<n;i++){
            float x = i+1;
            float y = values.get(i);
            sumX += x; sumY += y; sumXY += x*y; sumX2 += x*x;
        }
        float slope = (n*sumXY - sumX*sumY)/(n*sumX2 - sumX*sumX);
        float intercept = (sumY - slope*sumX)/n;
        float linearPred = slope*(n+1) + intercept;

        float prediction = (movingAvg + linearPred)/2;
        txtPrediction.setText("Rs "+String.format("%.2f", prediction));
    }

    // Generate category-based smart insights
    void generateInsights() {
        Cursor cursor = db.getExpenses(userUid);
        Map<String, Float> categoryMap = new HashMap<>();

        while(cursor.moveToNext()){
            String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
            float amount = cursor.getFloat(cursor.getColumnIndexOrThrow("amount"));
            categoryMap.put(category, categoryMap.getOrDefault(category, 0f)+amount);
        }

        String topCategory = ""; float max=0;
        for(String k : categoryMap.keySet()){
            if(categoryMap.get(k) > max){ max=categoryMap.get(k); topCategory=k; }
        }

        txtInsights.setText(
                "Highest spending category: "+topCategory+
                        "\nTotal spent: Rs "+String.format("%.2f", max)
        );
    }
}