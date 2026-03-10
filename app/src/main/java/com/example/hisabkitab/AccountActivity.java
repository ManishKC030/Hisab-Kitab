package com.example.hisabkitab;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountActivity extends Activity {

    TextView txtName, txtEmail;
    Button btnLogout, btnDeleteAccount;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    DatabaseHandler dbHandler; // SQLite handler

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.account);

        // Views
        txtName = findViewById(R.id.txtName);
        txtEmail = findViewById(R.id.txtEmail);
        btnLogout = findViewById(R.id.btnLogout);

        // NEW DELETE BUTTON
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // SQLite
        dbHandler = new DatabaseHandler(this);

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            txtEmail.setText(user.getEmail());

            if (user.getDisplayName() != null) {
                txtName.setText(user.getDisplayName());
            } else {
                txtName.setText("Welcome User");
            }
        }

        // LOGOUT
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(AccountActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // DELETE ACCOUNT
        btnDeleteAccount.setOnClickListener(v -> deleteAccount(user));
    }

    private void deleteAccount(FirebaseUser user) {
        if (user == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        String email = user.getEmail();

        // 1️⃣ Delete from Firestore
        db.collection("users")
                .document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // 2️⃣ Delete from Firebase Auth
                    user.delete()
                            .addOnSuccessListener(aVoid1 -> {
                                // 3️⃣ Delete from SQLite
                                dbHandler.deleteUser(email);

                                Toast.makeText(this, "Account deleted successfully!", Toast.LENGTH_LONG).show();

                                // 4️⃣ Go to LoginActivity
                                Intent intent = new Intent(AccountActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to delete Firebase user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete Firestore data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}