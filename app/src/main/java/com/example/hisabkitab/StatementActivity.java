package com.example.hisabkitab;

import android.os.Bundle;
import android.database.Cursor;
import android.util.Log;
import android.widget.*;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.*;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.*;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

import android.content.Intent;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.OutputStream;
import java.io.IOException;

public class StatementActivity extends AppCompatActivity {

    LineChart lineChart;
    RecyclerView recycler;
    Spinner spinnerDate, spinnerCategory;
    Button btnFilterAll, btnFilterIncome, btnFilterExpense, btnExportPDF;

    DatabaseHandler db;
    List<TransactionItem> allTransactions = new ArrayList<>();
    List<TransactionItem> filteredTransactions = new ArrayList<>();
    TransactionAdapter adapter;

    String selectedType = "All";
    String selectedDateRange = "All Time";
    String selectedCategory = "All";

    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    String userUid;

    boolean isSpinnerInitialized = false;

    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.statement);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        userUid = currentUser.getUid();

        db = new DatabaseHandler(this);

        lineChart = findViewById(R.id.lineChart);
        recycler = findViewById(R.id.recyclerTransactions);
        spinnerDate = findViewById(R.id.spinnerDate);
        spinnerCategory = findViewById(R.id.spinnerCategory);

        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterIncome = findViewById(R.id.btnFilterIncome);
        btnFilterExpense = findViewById(R.id.btnFilterExpense);
        btnExportPDF = findViewById(R.id.btnExportPDF);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(filteredTransactions);
        recycler.setAdapter(adapter);

        setupSpinners();
        setupFilterListeners();

        // Prevent auto-trigger
        spinnerDate.setSelection(0, false);
        spinnerCategory.setSelection(0, false);
        isSpinnerInitialized = true;

        updateFilterUI();
        loadData();

        btnExportPDF.setOnClickListener(v -> exportStatementAsPDF());
    }

    void setupFilterListeners() {

        btnFilterAll.setOnClickListener(v -> {
            selectedType = "All";
            updateFilterUI();
            applyFilters();
        });

        btnFilterIncome.setOnClickListener(v -> {
            selectedType = "Income";
            updateFilterUI();
            applyFilters();
        });

        btnFilterExpense.setOnClickListener(v -> {
            selectedType = "Expense";
            updateFilterUI();
            applyFilters();
        });

        spinnerDate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (!isSpinnerInitialized) return;

                selectedDateRange = parent.getItemAtPosition(position).toString();
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (!isSpinnerInitialized) return;

                selectedCategory = parent.getItemAtPosition(position).toString();
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    void updateFilterUI() {
        int white = Color.WHITE;
        int darkGray = Color.DKGRAY;

        btnFilterAll.setBackgroundResource(selectedType.equals("All") ? R.drawable.filter_selected : R.drawable.filter_unselected);
        btnFilterAll.setTextColor(selectedType.equals("All") ? white : darkGray);

        btnFilterIncome.setBackgroundResource(selectedType.equals("Income") ? R.drawable.filter_selected : R.drawable.filter_unselected);
        btnFilterIncome.setTextColor(selectedType.equals("Income") ? white : darkGray);

        btnFilterExpense.setBackgroundResource(selectedType.equals("Expense") ? R.drawable.filter_selected : R.drawable.filter_unselected);
        btnFilterExpense.setTextColor(selectedType.equals("Expense") ? white : darkGray);
    }

    void loadData() {
        allTransactions.clear();

        Cursor income = db.getIncome(userUid);
        if (income != null) {
            while (income.moveToNext()) {
                allTransactions.add(new TransactionItem(
                        income.getString(income.getColumnIndexOrThrow("title")),
                        income.getDouble(income.getColumnIndexOrThrow("amount")),
                        income.getString(income.getColumnIndexOrThrow("date")),
                        true,
                        income.getInt(income.getColumnIndexOrThrow("synced")),
                        income.getString(income.getColumnIndexOrThrow("category"))
                ));
            }
            income.close();
        }

        Cursor expense = db.getExpenses(userUid);
        if (expense != null) {
            while (expense.moveToNext()) {
                allTransactions.add(new TransactionItem(
                        expense.getString(expense.getColumnIndexOrThrow("title")),
                        expense.getDouble(expense.getColumnIndexOrThrow("amount")),
                        expense.getString(expense.getColumnIndexOrThrow("date")),
                        false,
                        expense.getInt(expense.getColumnIndexOrThrow("synced")),
                        expense.getString(expense.getColumnIndexOrThrow("category"))
                ));
            }
            expense.close();
        }

        Log.d("DATA_DEBUG", "Total transactions: " + allTransactions.size());

        applyFilters();
    }

    void applyFilters() {
        filteredTransactions.clear();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        Date now = new Date();

        for (TransactionItem item : allTransactions) {

            if (!selectedType.equals("All")) {
                if (item.isIncome != selectedType.equals("Income")) continue;
            }

            if (!selectedCategory.equals("All")) {
                if (!item.category.equalsIgnoreCase(selectedCategory)) continue;
            }

            if (!selectedDateRange.equals("All Time")) {
                try {
                    Date itemDate = sdf.parse(item.date);
                    if (itemDate == null) continue;

                    long diff = now.getTime() - itemDate.getTime();
                    long days = diff / (1000 * 60 * 60 * 24);

                    if (selectedDateRange.equals("Last 7 Days") && days > 7) continue;
                    if (selectedDateRange.equals("Last 30 Days") && days > 30) continue;

                } catch (Exception e) {
                    Log.e("DATE_PARSE", "Error: " + item.date);
                    continue;
                }
            }

            filteredTransactions.add(item);
        }

        Collections.sort(filteredTransactions, (a, b) -> {
            try {
                Date d1 = sdf.parse(a.date);
                Date d2 = sdf.parse(b.date);
                return d2.compareTo(d1);
            } catch (Exception e) {
                return 0;
            }
        });

        adapter.notifyDataSetChanged();
        updateGraph(filteredTransactions);
    }

    void updateGraph(List<TransactionItem> list) {
        List<Entry> incomeEntries = new ArrayList<>();
        List<Entry> expenseEntries = new ArrayList<>();

        float i = 1, e = 1;

        for (TransactionItem item : list) {
            if (item.isIncome) {
                incomeEntries.add(new Entry(i++, (float) item.amount));
            } else {
                expenseEntries.add(new Entry(e++, (float) item.amount));
            }
        }

        LineDataSet incomeSet = new LineDataSet(incomeEntries, "Income");
        incomeSet.setColor(ContextCompat.getColor(this, R.color.income_green));

        LineDataSet expenseSet = new LineDataSet(expenseEntries, "Expense");
        expenseSet.setColor(ContextCompat.getColor(this, R.color.expense_red));

        lineChart.setData(new LineData(incomeSet, expenseSet));
        lineChart.invalidate();
    }

    void setupSpinners() {
        String[] dateFilters = {"All Time", "Last 7 Days", "Last 30 Days"};
        String[] categories = {"All", "Food", "Transport", "Shopping", "Salary", "Bills"};

        spinnerDate.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, dateFilters));
        spinnerCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories));
    }

    // -------------------- PDF EXPORT --------------------

    private void exportStatementAsPDF() {

        if (filteredTransactions.isEmpty()) {
            Toast.makeText(this, "No transactions to export", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
                return;
            }
        }

        generateAndSavePDF();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generateAndSavePDF();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void generateAndSavePDF() {

        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();

        int pageWidth = 595;
        int pageHeight = 842;
        int y = 50;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        paint.setTextSize(18f);
        paint.setFakeBoldText(true);
        canvas.drawText("Statement Report", 180, y, paint);
        y += 40;

        paint.setTextSize(12f);
        paint.setFakeBoldText(false);

        canvas.drawText("Date: " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()), 20, y, paint);
        y += 30;

        canvas.drawText("Title   Category   Type   Amount", 20, y, paint);
        y += 20;

        for (TransactionItem item : filteredTransactions) {

            if (y > pageHeight - 40) {
                pdfDocument.finishPage(page);
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
            }

            String type = item.isIncome ? "Income" : "Expense";
            String text = item.title + "   " + item.category + "   " + type + "   Rs " + item.amount;

            canvas.drawText(text, 20, y, paint);
            y += 20;
        }

        pdfDocument.finishPage(page);

        String fileName = "Statement_" + System.currentTimeMillis() + ".pdf";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            savePdfScoped(pdfDocument, fileName);
        } else {
            savePdfLegacy(pdfDocument, fileName);
        }

        pdfDocument.close();
    }

    private void savePdfScoped(PdfDocument pdfDocument, String fileName) {

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        try {
            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

            OutputStream out = getContentResolver().openOutputStream(uri);
            pdfDocument.writeTo(out);
            out.close();

            Toast.makeText(this, "Saved to Downloads", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void savePdfLegacy(PdfDocument pdfDocument, String fileName) {

        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), fileName);

            FileOutputStream out = new FileOutputStream(file);
            pdfDocument.writeTo(out);
            out.close();

            Toast.makeText(this, "Saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error saving PDF", Toast.LENGTH_SHORT).show();
        }
    }
}