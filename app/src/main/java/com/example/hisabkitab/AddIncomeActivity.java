package com.example.hisabkitab;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

public class AddIncomeActivity extends AppCompatActivity {

    EditText edtAmount, edtDate, edtTitle, edtDescription, edtNewCategory;
    Button btnAddIncome;
    GridLayout gridCategory;

    String selectedCategory = "";
    DatabaseHandler db;
    FirebaseAuth auth;
    DatabaseReference firebaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_income);

        db = new DatabaseHandler(this);
        auth = FirebaseAuth.getInstance();

        edtAmount = findViewById(R.id.edtIncomeAmount);
        edtDate = findViewById(R.id.edtIncomeDate);
        edtTitle = findViewById(R.id.edtIncomeTitle);
        edtDescription = findViewById(R.id.edtIncomeDescription);
        edtNewCategory = findViewById(R.id.edtNewIncomeCategory);
        btnAddIncome = findViewById(R.id.btnAddIncome);
        gridCategory = findViewById(R.id.gridIncomeCategory);

        setupDatePicker();
        setupCategorySelection();

        btnAddIncome.setOnClickListener(v -> saveIncome());
    }

    private void setupDatePicker() {
        edtDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this,
                    (view, year, month, day) ->
                            edtDate.setText(day + "/" + (month + 1) + "/" + year),
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)).show();
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

    private void saveIncome() {

        String amountStr = edtAmount.getText().toString().trim();
        String title = edtTitle.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();
        String date = edtDate.getText().toString().trim();
        String userUid = auth.getCurrentUser().getUid();

        if (amountStr.isEmpty() || title.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);

        if (selectedCategory.equals("Other")) {
            selectedCategory = edtNewCategory.getText().toString().trim();
        }

        // 1️⃣ Save to SQLite first
        long localId = db.insertIncome(
                "",
                userUid,
                title,
                amount,
                selectedCategory,
                description,
                date,
                0
        );

        // 2️⃣ Save to Firebase
        firebaseRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(userUid)
                .child("income");

        String firebaseId = firebaseRef.push().getKey();

        IncomeModel income = new IncomeModel(
                firebaseId, userUid, title, amount,
                selectedCategory, description, date
        );

        firebaseRef.child(firebaseId).setValue(income)
                .addOnSuccessListener(unused -> {
                    db.markIncomeAsSynced((int) localId, firebaseId);
                    Toast.makeText(this, "Income Added & Synced", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}