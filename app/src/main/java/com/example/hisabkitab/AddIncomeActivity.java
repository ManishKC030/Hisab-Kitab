package com.example.hisabkitab;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddIncomeActivity extends AppCompatActivity {

    EditText edtAmount, edtDate, edtTitle, edtDescription, edtNewCategory;
    Button btnAddIncome;
    GridLayout gridCategory;

    String selectedCategory = "";

    DatabaseHandler db;
    FirebaseAuth auth;
    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_income);

        db = new DatabaseHandler(this);
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

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

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userUid = auth.getCurrentUser().getUid();

        if (amountStr.isEmpty() || title.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);

        if (selectedCategory.equals("Other")) {
            selectedCategory = edtNewCategory.getText().toString().trim();
        }

        if (selectedCategory.isEmpty()) {
            Toast.makeText(this, "Please select category", Toast.LENGTH_SHORT).show();
            return;
        }

        long timestamp = System.currentTimeMillis();

        // 1️⃣ Save to SQLite first (unsynced)
        long localId = db.insertIncome(
                "", // firestoreId empty initially
                userUid,
                title,
                amount,
                selectedCategory,
                description,
                date,
                0 // 0 = not synced
        );

        // 2️⃣ Save to Firestore
        Map<String, Object> incomeMap = new HashMap<>();
        incomeMap.put("userId", userUid);
        incomeMap.put("title", title);
        incomeMap.put("amount", amount);
        incomeMap.put("category", selectedCategory);
        incomeMap.put("description", description);
        incomeMap.put("date", date);
        incomeMap.put("type", "income");
        incomeMap.put("timestamp", timestamp);

        firestore.collection("users")
                .document(userUid)
                .collection("transactions")
                .add(incomeMap)
                .addOnSuccessListener(documentReference -> {

                    String firestoreId = documentReference.getId();

                    // 3️⃣ Update SQLite with Firestore ID & mark synced
                    db.markIncomeAsSynced((int) localId, firestoreId);

                    Toast.makeText(this, "Income Added & Synced", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Saved locally. Will sync later.", Toast.LENGTH_LONG).show();
                });
    }
}