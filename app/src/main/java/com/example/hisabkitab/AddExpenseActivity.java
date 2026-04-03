package com.example.hisabkitab;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddExpenseActivity extends AppCompatActivity {

    EditText edtAmount, edtDate, edtTitle, edtDescription, edtNewCategory;
    Button btnAddExpense;
    GridLayout gridCategory;

    String selectedCategory = "";

    DatabaseHandler db;
    FirebaseAuth auth;
    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_expense);

        db = new DatabaseHandler(this);
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        edtAmount = findViewById(R.id.edtAmount);
        edtDate = findViewById(R.id.edtDate);
        edtTitle = findViewById(R.id.edtTitle);
        edtDescription = findViewById(R.id.edtDescription);
        edtNewCategory = findViewById(R.id.edtNewCategory);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        gridCategory = findViewById(R.id.gridCategory);

        setupDatePicker();
        setupCategorySelection();

        btnAddExpense.setOnClickListener(v -> saveExpense());
    }

    // ✅ Internet check
    private boolean isNetworkAvailable() {

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }

        return false;
    }

    private void setupDatePicker() {
        edtDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, year, month, day) ->
                            edtDate.setText(day + "/" + (month + 1) + "/" + year),
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            // 🚫 Disable future dates (tomorrow and beyond)
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

            datePickerDialog.show();
        });
    }

    private void setupCategorySelection() {

        for (int i = 0; i < gridCategory.getChildCount(); i++) {

            View view = gridCategory.getChildAt(i);

            if (view instanceof TextView) {

                view.setOnClickListener(v -> {

                    for (int j = 0; j < gridCategory.getChildCount(); j++) {
                        gridCategory.getChildAt(j)
                                .setBackgroundResource(R.drawable.bg_category_unselected);
                    }

                    v.setBackgroundResource(R.drawable.bg_category_selected);

                    selectedCategory = ((TextView) v).getText().toString();

                    if (selectedCategory.equals("Other")) {
                        edtNewCategory.setVisibility(View.VISIBLE);
                    } else {
                        edtNewCategory.setVisibility(View.GONE);
                    }
                });
            }
        }
    }
    private void saveExpense() {

        String amountStr = edtAmount.getText().toString().trim();
        String title = edtTitle.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();
        String date = edtDate.getText().toString().trim();

        // 1️⃣ Amount Validation
        if (amountStr.isEmpty()) {
            edtAmount.setError("Amount is required");
            edtAmount.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                edtAmount.setError("Enter valid amount");
                edtAmount.requestFocus();
                return;
            }
        } catch (Exception e) {
            edtAmount.setError("Invalid number");
            edtAmount.requestFocus();
            return;
        }

        // 2️⃣ Title Validation
        if (title.isEmpty()) {
            edtTitle.setError("Title is required");
            edtTitle.requestFocus();
            return;
        }

        if (title.length() < 3) {
            edtTitle.setError("Title too short");
            edtTitle.requestFocus();
            return;
        }

        // 3️⃣ Date Validation
        if (date.isEmpty()) {
            edtDate.setError("Select date");
            edtDate.requestFocus();
            return;
        }

        // 4️⃣ Category Validation
        if (selectedCategory.equals("Other")) {
            selectedCategory = edtNewCategory.getText().toString().trim();

            if (selectedCategory.isEmpty()) {
                edtNewCategory.setError("Enter category name");
                edtNewCategory.requestFocus();
                return;
            }
        }

        if (selectedCategory.isEmpty()) {
            Toast.makeText(this, "Please select category", Toast.LENGTH_SHORT).show();
            return;
        }

        // 5️⃣ Description (Optional but limited)
        if (description.length() > 100) {
            edtDescription.setError("Max 100 characters");
            edtDescription.requestFocus();
            return;
        }

        // 6️⃣ User Check
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userUid = auth.getCurrentUser().getUid();

        // 7️⃣ Save locally (Offline-first)
        long localId = db.insertExpense(
                "",
                userUid,
                title,
                amount,
                selectedCategory,
                description,
                date,
                0 // not synced
        );

        if (localId == -1) {
            Toast.makeText(this, "Error saving locally", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Expense saved locally", Toast.LENGTH_SHORT).show();

        // 8️⃣ Sync
        SyncManager.syncData(this);

        finish();
    }