package com.example.hisabkitab;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.*;
import java.util.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;


public class RegisterActivity extends  Activity{
    TextView txtGoToLogin;
    EditText edtName, edtEmail, edtPassword;
    Button btnCreateAccount;

    FirebaseAuth auth;
    FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.register);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        txtGoToLogin = findViewById(R.id.txtGoToLogin);

        // Create Account Button
        btnCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String name = edtName.getText().toString().trim();
                String email = edtEmail.getText().toString().trim();
                String password = edtPassword.getText().toString().trim();

                if(name.isEmpty() || email.isEmpty() || password.isEmpty()){
                    Toast.makeText(RegisterActivity.this,
                            "All fields are required", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create user in Firebase Auth
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {

                            if(task.isSuccessful()) {

                                String userId = auth.getCurrentUser().getUid();

                                // Create user data map
                                Map<String, Object> user = new HashMap<>();
                                user.put("name", name);
                                user.put("email", email);

                                // Store in Firestore
                                db.collection("users")
                                        .document(userId)
                                        .set(user)
                                        .addOnSuccessListener(unused -> {

                                            Toast.makeText(RegisterActivity.this,
                                                    "Account Created Successfully!",
                                                    Toast.LENGTH_SHORT).show();

                                            // Go to Login Page
                                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                            startActivity(intent);
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(RegisterActivity.this,
                                                    "Database Error: " + e.getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        });

                            } else {
                                Toast.makeText(RegisterActivity.this,
                                        "Error: " + task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

        txtGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); //closes register page
            }
        });
    }
}
