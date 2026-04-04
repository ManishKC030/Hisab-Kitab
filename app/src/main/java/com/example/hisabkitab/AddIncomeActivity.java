package com.example.hisabkitab;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import android.text.Editable;
import android.text.TextWatcher;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Calendar;

public class AddIncomeActivity extends AppCompatActivity {

    EditText edtAmount, edtDate, edtTitle, edtDescription, edtNewCategory;
    Button btnAddIncome;
    GridLayout gridCategory;

    String selectedCategory = "";
    private String currentAmount = "";

    DatabaseHandler db;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_income);

        db = new DatabaseHandler(this);
        auth = FirebaseAuth.getInstance();

        edtAmount = findViewById(R.id.edtIncomeAmount);
        edtDate = findViewById(R.id.edtIncomeDate);
        edtTitle = findViewById(R.id.edtIncomeTitle);
        edtDescription = findViewById(R.id.edtIncomeDescription);
        edtNewCategory = findViewById(R.id.edtNewIncomeCategory);
        btnAddIncome = findViewById(R.id.btnAddIncome);
        gridCategory = findViewById(R.id.gridIncomeCategory);

        edtDate.setFocusable(false); // prevent manual typing

        setupDatePicker();
        setupCategorySelection();
        setupCurrencyFormatting();
        setupRealtimeValidation();

        btnAddIncome.setOnClickListener(v -> saveIncome());
    }

    // 📅 Date Picker (no future)
    private void setupDatePicker() {
        edtDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    String selectedDate = day + "/" + (month + 1) + "/" + year;
                    edtDate.setText(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

            dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            dialog.show();
        });
    }

    // 🧩 Category Selection
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
                        edtNewCategory.setError(null);
                    }
                });
            }
        }
    }

    // 💰 Currency Formatting
    private void setupCurrencyFormatting() {
        edtAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

                if (!s.toString().equals(currentAmount)) {

                    edtAmount.removeTextChangedListener(this);

                    String clean = s.toString().replaceAll("[Rs,.\\s]", "");

                    if (!clean.isEmpty()) {
                        double parsed = Double.parseDouble(clean);

                        NumberFormat format = NumberFormat.getInstance(new Locale("en", "IN"));
                        String formatted = "Rs " + format.format(parsed);

                        currentAmount = formatted;
                        edtAmount.setText(formatted);
                        edtAmount.setSelection(formatted.length());
                    }

                    edtAmount.addTextChangedListener(this);
                }
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    // ⚡ Real-time Validation
    private void setupRealtimeValidation() {

        // Amount
        edtAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().replaceAll("[Rs,.\\s]", "");

                if (value.isEmpty()) {
                    edtAmount.setError("Amount required");
                } else {
                    try {
                        double val = Double.parseDouble(value);
                        if (val <= 0) {
                            edtAmount.setError("Invalid amount");
                        } else {
                            edtAmount.setError(null);
                        }
                    } catch (Exception e) {
                        edtAmount.setError("Invalid input");
                    }
                }
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        // Title
        edtTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() < 3) {
                    edtTitle.setError("Minimum 3 characters");
                } else {
                    edtTitle.setError(null);
                }
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        // Category "Other"
        edtNewCategory.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (selectedCategory.equals("Other") && s.toString().trim().isEmpty()) {
                    edtNewCategory.setError("Enter category");
                } else {
                    edtNewCategory.setError(null);
                }
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    // 💾 Save Income
    private void saveIncome() {

        String amountStr = edtAmount.getText().toString().replaceAll("[Rs,.\\s]", "");
        String title = edtTitle.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();
        String date = edtDate.getText().toString().trim();

        if (amountStr.isEmpty()) {
            edtAmount.setError("Amount required");
            return;
        }

        double amount = Double.parseDouble(amountStr);

        if (title.isEmpty() || title.length() < 3) {
            edtTitle.setError("Enter valid title");
            return;
        }

        if (date.isEmpty()) {
            edtDate.setError("Select date");
            return;
        }

        String finalCategory = selectedCategory;

        if (selectedCategory.equals("Other")) {
            finalCategory = edtNewCategory.getText().toString().trim();

            if (finalCategory.isEmpty()) {
                edtNewCategory.setError("Enter category");
                return;
            }
        }

        if (finalCategory.isEmpty()) {
            Toast.makeText(this, "Select category", Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.length() > 100) {
            edtDescription.setError("Max 100 characters");
            return;
        }

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userUid = auth.getCurrentUser().getUid();

        long localId = db.insertIncome(
                "",
                userUid,
                title,
                amount,
                finalCategory,
                description,
                date,
                0
        );

        if (localId == -1) {
            Toast.makeText(this, "Error saving", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Income saved", Toast.LENGTH_SHORT).show();

        SyncManager.syncData(this);

        finish();
    }
}