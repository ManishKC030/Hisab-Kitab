package com.example.hisabkitab;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.*;
import android.text.TextUtils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends Activity {

    TextView txtGoToRegister, txtForgotPassword;
    EditText edtEmail, edtPassword;
    Button btnLogin;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.login);

        auth = FirebaseAuth.getInstance();

        // 🔹 Auto Login if already logged in AND email verified
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            startActivity(new Intent(LoginActivity.this, DashboardActvity.class));
            finish();
        }

        // Initialize views
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtGoToRegister = findViewById(R.id.txtGoToRegister);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);

        // 🔹 LOGIN BUTTON
        btnLogin.setOnClickListener(v -> loginUser());

        // 🔹 Go To Register
        txtGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });

        // 🔹 Forgot Password
        txtForgotPassword.setOnClickListener(v -> resetPassword());
    }

    private void loginUser() {

        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    btnLogin.setEnabled(true);

                    if (task.isSuccessful()) {

                        FirebaseUser user = auth.getCurrentUser();

                        if (user != null) {

                            if (user.isEmailVerified()) {

                                Toast.makeText(this,
                                        "Login Successful!",
                                        Toast.LENGTH_SHORT).show();

                                startActivity(new Intent(LoginActivity.this,
                                        DashboardActvity.class));
                                finish();

                            } else {

                                user.sendEmailVerification();

                                Toast.makeText(this,
                                        "Email not verified. Verification email sent again.",
                                        Toast.LENGTH_LONG).show();

                                auth.signOut();
                            }
                        }

                    } else {

                        Toast.makeText(this,
                                "Login Failed: " +
                                        task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void resetPassword() {

        String email = edtEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this,
                    "Enter your registered email first",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this,
                                "Password reset email sent!",
                                Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
}
