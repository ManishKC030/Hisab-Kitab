package com.example.hisabkitab;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountActivity extends AppCompatActivity {

    TextView txtName, txtEmail;
    Button btnLogout, btnDeleteAccount;
    LinearLayout navBtnHome, navBtnAnalytics, navBtnStatement, navBtnAccount;

    FirebaseAuth mAuth;
    FirebaseFirestore firestore;
    DatabaseHandler dbHandler; // SQLite

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account);

        // Views
        txtName = findViewById(R.id.txtName);
        txtEmail = findViewById(R.id.txtEmail);
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);

        // Navigation
        navBtnHome = findViewById(R.id.navBtnHome);
        navBtnAnalytics = findViewById(R.id.navBtnAnalytics);
        navBtnStatement = findViewById(R.id.navBtnStatement);
        navBtnAccount = findViewById(R.id.navBtnAccount);

        // Set active tab color
        int activeColor = androidx.core.content.ContextCompat.getColor(this, R.color.colorPrimary);
        ((ImageView)findViewById(R.id.navIconAccount)).setColorFilter(activeColor);
        ((TextView)findViewById(R.id.navTextAccount)).setTextColor(activeColor);

        navBtnHome.setOnClickListener(v ->
                startActivity(new Intent(this, DashboardActivity.class)));
        navBtnStatement.setOnClickListener(v ->
                startActivity(new Intent(this, StatementActivity.class)));
        navBtnAnalytics.setOnClickListener(v ->
                startActivity(new Intent(this, AnalyticsActivity.class)));

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // SQLite
        dbHandler = new DatabaseHandler(this);

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            txtEmail.setText(user.getEmail());
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                txtName.setText(user.getDisplayName());
            } else {
                txtName.setText("Welcome User");
            }
        }

        // Logout
        btnLogout.setOnClickListener(v -> logoutUser());

        // Delete Account
        btnDeleteAccount.setOnClickListener(v -> deleteAccount(user));
    }

    // --------------------------
    // LOGOUT METHOD
    // --------------------------
    private void logoutUser() {
        // No internet required
        mAuth.signOut();
        new SessionManager(this).clearSession();
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(AccountActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // --------------------------
    // DELETE ACCOUNT METHOD
    // --------------------------
    private void deleteAccount(FirebaseUser user) {
        if (user == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = user.getEmail();

        // Ask user for password to re-authenticate
        EditText edtPassword = new EditText(this);
        edtPassword.setHint("Enter your password");

        new AlertDialog.Builder(this)
                .setTitle("Confirm Account Deletion")
                .setMessage("For security, please enter your password to delete your account.")
                .setView(edtPassword)
                .setPositiveButton("Delete", (dialog, which) -> {
                    String password = edtPassword.getText().toString().trim();
                    if (password.isEmpty()) {
                        Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Reauthenticate user
                    user.reauthenticate(EmailAuthProvider.getCredential(email, password))
                            .addOnSuccessListener(aVoid -> {

                                String uid = user.getUid();

                                // 1. Attempt Firestore delete (Fail-safe)
                                firestore.collection("users").document(uid).delete()
                                        .addOnCompleteListener(task -> {
                                            // Even if Firestore fails (e.g. permission denied or doc gone),
                                            // we proceed to delete the Auth account and local data.
                                            if (!task.isSuccessful()) {
                                                android.util.Log.e("AccountActivity", "Firestore delete failed: " + task.getException());
                                            }

                                            // 2. Delete the Firebase Auth account
                                            user.delete()
                                                    .addOnSuccessListener(aVoid2 -> {
                                                        // 3. Clear SQLite data
                                                        dbHandler.clearUserData(uid);
                                                        new SessionManager(this).clearSession();

                                                        Toast.makeText(this, "Account deleted successfully!", Toast.LENGTH_LONG).show();

                                                        Intent intent = new Intent(AccountActivity.this, LoginActivity.class);
                                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                        startActivity(intent);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(this, "Auth deletion failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                        android.util.Log.e("AccountActivity", "Auth Error: ", e);
                                                    });
                                        });

                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Reauthentication failed: " + e.getMessage(), Toast.LENGTH_LONG).show());

                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}