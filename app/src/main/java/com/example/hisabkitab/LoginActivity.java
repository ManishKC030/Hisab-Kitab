package com.example.hisabkitab;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.*;
import android.text.TextUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends Activity {

    EditText edtEmail, edtPassword;
    Button btnLogin;
    TextView txtGoToRegister, txtForgotPassword;

    FirebaseAuth auth;
    DatabaseHandler dbHandler; // SQLite

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Firebase
        auth = FirebaseAuth.getInstance();

        // SQLite
        dbHandler = new DatabaseHandler(this);

        // Views
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtGoToRegister = findViewById(R.id.txtGoToRegister);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);

        // If already logged in online
        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, DashboardActvity.class));
            finish();
        }

        btnLogin.setOnClickListener(v -> loginUser());

        txtGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });

        txtForgotPassword.setOnClickListener(v -> forgotPassword());
    }

    // 🔹 Check internet
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    // 🔹 Login user
    private void loginUser() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);

        if (isNetworkAvailable()) {
            // Online login using Firebase
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        btnLogin.setEnabled(true);
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null && user.isEmailVerified()) {
                                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, DashboardActvity.class));
                                finish();
                            } else {
                                Toast.makeText(this, "Please verify your email first!", Toast.LENGTH_LONG).show();
                                auth.signOut();
                            }
                        } else {
                            Toast.makeText(this, "Firebase login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            // Offline login using SQLite
            boolean exists = dbHandler.checkUser(email, password);
            btnLogin.setEnabled(true);
            if (exists) {
                String name = dbHandler.getUsername(email, password);
                Toast.makeText(this, "Offline login successful! Welcome " + name, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, DashboardActvity.class));
                finish();
            } else {
                Toast.makeText(this, "No internet and offline login failed!", Toast.LENGTH_LONG).show();
            }
        }
    }

    // 🔹 Forgot password
    private void forgotPassword() {
        String email = edtEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Enter your email first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isNetworkAvailable()) {
            auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(unused -> Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_LONG).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            Toast.makeText(this, "No internet! Cannot reset password offline.", Toast.LENGTH_LONG).show();
        }
    }
}
