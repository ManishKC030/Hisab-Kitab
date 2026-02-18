package com.example.hisabkitab;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.*;
import android.text.TextUtils;

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

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.register);

        //firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        //SQLite
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

    private void registerUser() {

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

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser user = auth.getCurrentUser();

                        if (user != null) {

                            // 🔹 Send Email Verification
                            user.sendEmailVerification()
                                    .addOnSuccessListener(unused -> {

                                        String userId = user.getUid();

                                        // Store user data in Firestore
                                        Map<String, Object> userMap = new HashMap<>();
                                        userMap.put("name", name);
                                        userMap.put("email", email);

                                        db.collection("users")
                                                .document(userId)
                                                .set(userMap);

                                        Toast.makeText(this,
                                                "Account created! Please verify your email before login.",
                                                Toast.LENGTH_LONG).show();

                                        // 🔹 Sign out user after registration
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
                        btnCreateAccount.setEnabled(true);
                        Toast.makeText(this,
                                "Registration Failed: " +
                                        task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
