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
import java.util.Date;
import java.util.Locale;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.components.XAxis;
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

import java.util.*;

public class StatementActivity extends AppCompatActivity {

    LineChart lineChart;

    RecyclerView recycler;
    Spinner spinnerDate, spinnerCategory;
    Button btnFilterAll, btnFilterIncome, btnFilterExpense;

    DatabaseHandler db;
    List<TransactionItem> allTransactions = new ArrayList<>();
    List<TransactionItem> filteredTransactions = new ArrayList<>();
    TransactionAdapter adapter;

    String selectedType = "All"; // "All", "Income", "Expense"
    String selectedDateRange = "All Time";
    String selectedCategory = "All";

    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    String userUid;

    LinearLayout navBtnHome, navBtnAnalytics, navBtnStatement, navBtnAccount;

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

        // Navigation
        navBtnHome = findViewById(R.id.navBtnHome);
        navBtnAnalytics = findViewById(R.id.navBtnAnalytics);
        navBtnStatement = findViewById(R.id.navBtnStatement);
        navBtnAccount = findViewById(R.id.navBtnAccount);

        // Set active tab color
        int activeColor = androidx.core.content.ContextCompat.getColor(this, R.color.colorPrimary);
        ((ImageView)findViewById(R.id.navIconStatement)).setColorFilter(activeColor);
        ((TextView)findViewById(R.id.navTextStatement)).setTextColor(activeColor);

        navBtnHome.setOnClickListener(v ->
                startActivity(new Intent(this, DashboardActivity.class)));
        navBtnAnalytics.setOnClickListener(v ->
                startActivity(new Intent(this, AnalyticsActivity.class)));
        navBtnAccount.setOnClickListener(v ->
                startActivity(new Intent(this, AccountActivity.class)));

        db = new DatabaseHandler(this);

        lineChart = findViewById(R.id.lineChart);
        recycler = findViewById(R.id.recyclerTransactions);

        spinnerDate = findViewById(R.id.spinnerDate);
        spinnerCategory = findViewById(R.id.spinnerCategory);

        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterIncome = findViewById(R.id.btnFilterIncome);
        btnFilterExpense = findViewById(R.id.btnFilterExpense);

        // Setup RecyclerView
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(filteredTransactions);
        recycler.setAdapter(adapter);

        setupSpinners();
        setupFilterListeners();

        Button btnExportPDF = findViewById(R.id.btnExportPDF);
        btnExportPDF.setOnClickListener(v -> exportStatementAsPDF());
        
        spinnerDate.setSelection(0);
        spinnerCategory.setSelection(0);
        updateFilterUI();
        loadData();
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
                selectedDateRange = parent.getItemAtPosition(position).toString();
                applyFilters();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
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

        applyFilters();
    }

    void applyFilters() {
        filteredTransactions.clear();
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.US);
        Calendar cal = Calendar.getInstance();
        Date now = new Date();

        for (TransactionItem item : allTransactions) {
            // Type Filter
            if (!selectedType.equals("All")) {
                boolean isIncome = selectedType.equals("Income");
                if (item.isIncome != isIncome) continue;
            }

            // Category Filter
            if (!selectedCategory.equals("All")) {
                if (!item.category.equalsIgnoreCase(selectedCategory)) continue;
            }

            // Date Filter
            if (!selectedDateRange.equals("All Time")) {
                try {
                    Date itemDate = sdf.parse(item.date);
                    if (itemDate == null) continue;

                    long diff = now.getTime() - itemDate.getTime();
                    long days = diff / (24 * 60 * 60 * 1000);

                    if (selectedDateRange.equals("Last 7 Days") && days > 7) continue;
                    if (selectedDateRange.equals("Last 30 Days") && days > 30) continue;
                } catch (Exception e) {
                    continue;
                }
            }

            filteredTransactions.add(item);
        }

        // Sort
        Collections.sort(filteredTransactions, (a, b) -> {
            try {
                Date d1 = sdf.parse(a.date);
                Date d2 = sdf.parse(b.date);
                return d2.compareTo(d1);
            } catch (Exception e) { return 0; }
        });

        adapter.notifyDataSetChanged();
        updateGraph(filteredTransactions);
    }

    void updateGraph(List<TransactionItem> list) {
        List<Entry> incomeEntries = new ArrayList<>();
        List<Entry> expenseEntries = new ArrayList<>();

        // Group by date for line chart might be complex, let's keep it simple as a trend
        float incIdx = 1, expIdx = 1;
        
        // Reverse list to show chronologically on graph
        List<TransactionItem> sortedList = new ArrayList<>(list);
        Collections.sort(sortedList, (a, b) -> {
            try {
                SimpleDateFormat parser = new SimpleDateFormat("d/M/yyyy", Locale.US);
                return parser.parse(a.date).compareTo(parser.parse(b.date));
            } catch (Exception e) { return 0; }
        });

        for (TransactionItem item : sortedList) {
            if (item.isIncome) {
                incomeEntries.add(new Entry(incIdx++, (float) item.amount));
            } else {
                expenseEntries.add(new Entry(expIdx++, (float) item.amount));
            }
        }

        LineDataSet incomeSet = new LineDataSet(incomeEntries, "Income");
        incomeSet.setColor(androidx.core.content.ContextCompat.getColor(this, R.color.income_green));
        incomeSet.setCircleColor(androidx.core.content.ContextCompat.getColor(this, R.color.income_green));
        incomeSet.setDrawFilled(true);
        incomeSet.setFillColor(androidx.core.content.ContextCompat.getColor(this, R.color.income_green));
        incomeSet.setFillAlpha(30);

        LineDataSet expenseSet = new LineDataSet(expenseEntries, "Expense");
        expenseSet.setColor(androidx.core.content.ContextCompat.getColor(this, R.color.expense_red));
        expenseSet.setCircleColor(androidx.core.content.ContextCompat.getColor(this, R.color.expense_red));
        expenseSet.setDrawFilled(true);
        expenseSet.setFillColor(androidx.core.content.ContextCompat.getColor(this, R.color.expense_red));
        expenseSet.setFillAlpha(30);

        LineData data = new LineData(incomeSet, expenseSet);
        lineChart.setData(data);
        lineChart.invalidate();
    }

    private static final int PERMISSION_REQUEST_CODE = 100;

    //export as pdf
    private void exportStatementAsPDF() {
        if (allTransactions.isEmpty()) {
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generateAndSavePDF();
            } else {
                Toast.makeText(this, "Permission denied to write to storage", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void generateAndSavePDF() {
        PdfDocument pdfDocument = new PdfDocument();
        try {
            Paint paint = new Paint();

            int pageWidth = 595;
            int pageHeight = 842;
            int y = 50;

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            paint.setTextSize(20f);
            paint.setFakeBoldText(true);
            canvas.drawText("Complete Statement Report", 150, y, paint);
            y += 40;

            paint.setTextSize(14f);
            paint.setFakeBoldText(false);

            canvas.drawText("Date: " + new SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(new Date()), 20, y, paint);
            y += 30;

            canvas.drawText("Title           Category        Type       Amount", 20, y, paint);
            y += 20;
            canvas.drawLine(20, y, pageWidth - 20, y, paint);
            y += 20;

            // Sort allTransactions by date before exporting
            List<TransactionItem> exportList = new ArrayList<>(allTransactions);
            SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.US);
            Collections.sort(exportList, (a, b) -> {
                try {
                    Date d1 = sdf.parse(a.date);
                    Date d2 = sdf.parse(b.date);
                    return d2.compareTo(d1);
                } catch (Exception e) {
                    return 0;
                }
            });

            for (TransactionItem item : exportList) {
                if (y > pageHeight - 50) {
                    pdfDocument.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdfDocument.getPages().size() + 1).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 50;
                }

                String type = item.isIncome ? "Income" : "Expense";
                String text = String.format(Locale.getDefault(), "%-15s %-12s %-8s Rs %.2f",
                        item.title, item.category, type, item.amount);

                canvas.drawText(text, 20, y, paint);
                y += 20;
            }

            pdfDocument.finishPage(page);

            String fileName = "StatementReport_" + System.currentTimeMillis() + ".pdf";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ScopedStorageHelper.savePdfToDownloads(this, pdfDocument, fileName);
            } else {
                savePdfLegacy(pdfDocument, fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            pdfDocument.close();
        }
    }

    private void savePdfLegacy(PdfDocument pdfDocument, String fileName) {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(downloadsDir, fileName);

        try {
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }
            FileOutputStream out = new FileOutputStream(file);
            pdfDocument.writeTo(out);
            out.close();
            Toast.makeText(this, "PDF saved to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

            // Scan the file so it appears in file managers
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(file));
            sendBroadcast(intent);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Helper class for Scoped Storage to avoid NoClassDefFoundError on older APIs
    private static class ScopedStorageHelper {
        static void savePdfToDownloads(android.content.Context context, PdfDocument pdfDocument, String fileName) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            try {
                Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    OutputStream out = context.getContentResolver().openOutputStream(uri);
                    pdfDocument.writeTo(out);
                    out.close();
                    Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "Error creating PDF file", Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(context, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    void setupSpinners() {
        String[] dateFilters = {"All Time", "Last 7 Days", "Last 30 Days"};
        String[] categories = {"All", "Food", "Transport", "Shopping", "Salary", "Bills"};

        spinnerDate.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, dateFilters));

        spinnerCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, categories));
    }
}