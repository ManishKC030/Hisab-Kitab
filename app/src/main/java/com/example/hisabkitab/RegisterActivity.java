package com.example.hisabkitab;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.*;
import android.text.TextUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends Activity {

    TextView txtGoToLogin;
    EditText edtName, edtEmail, edtPassword;
    Button btnCreateAccount;

    FirebaseAuth auth;
    FirebaseFirestore db;
    DatabaseHandler dbHandler; // SQLite handler

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.register);

        // Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // SQLite
        dbHandler = new DatabaseHandler(this);

        // Views
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        txtGoToLogin = findViewById(R.id.txtGoToLogin);

        btnCreateAccount.setOnClickListener(v -> registerUser());

        txtGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    // 🔹 Check internet connectivity
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    // 🔹 Register user
    private void registerUser() {

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection! Please connect to Wi-Fi or mobile data.", Toast.LENGTH_LONG).show();
            return; // stop registration
        }

        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this,
                    "All fields are required",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnCreateAccount.setEnabled(false);

        // 🔹 Firebase registration
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    btnCreateAccount.setEnabled(true);

                    if (task.isSuccessful()) {

                        FirebaseUser user = auth.getCurrentUser();

                        if (user != null) {

                            // 🔹 Send email verification
                            user.sendEmailVerification()
                                    .addOnSuccessListener(unused -> {

                                        String userId = user.getUid();

                                        // 🔹 Store user in Firestore
                                        Map<String, Object> userMap = new HashMap<>();
                                        userMap.put("name", name);
                                        userMap.put("email", email);

                                        db.collection("users")
                                                .document(userId)
                                                .set(userMap);

                                        // 🔹 Store user in SQLite for offline login
                                        dbHandler.insertUser(userId, email, password, name);

                                        Toast.makeText(this,
                                                "Account created! Please verify your email before login.",
                                                Toast.LENGTH_LONG).show();

                                        // 🔹 Sign out user to force email verification
                                        auth.signOut();

                                        // 🔹 Go to Login page
                                        startActivity(new Intent(RegisterActivity.this,
                                                LoginActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this,
                                                "Verification email failed: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    });

                        }

                    } else {
                        Toast.makeText(this,
                                "Registration Failed: " +
                                        task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
