package com.example.hisabkitab;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends Activity {
    TextView txtGoToRegister, txtForgotPassword;;
    EditText edtEmail, edtPassword;
    Button btnLogin;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.login);


        // Initialize Firebase
        auth = FirebaseAuth.getInstance();

        //if user is logged in already then no need to need to login
        if(auth.getCurrentUser() != null){
            Intent intent = new Intent(LoginActivity.this, DashboardActvity.class);
            startActivity(intent);
            finish();
        }


        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtGoToRegister = findViewById(R.id.txtGoToRegister);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);

        // LOGIN BUTTON
        btnLogin.setOnClickListener(v -> {

            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if(email.isEmpty() || password.isEmpty()){
                Toast.makeText(LoginActivity.this,
                        "All fields required", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {

                        if(task.isSuccessful()) {

                            Toast.makeText(LoginActivity.this,
                                    "Login Successful!",
                                    Toast.LENGTH_SHORT).show();

                            // Go to MainActivity
                            Intent intent = new Intent(LoginActivity.this, DashboardActvity.class);
                            startActivity(intent);
                            finish();

                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "Login Failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });

        });

        txtGoToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                finish(); //closes login page

            }
        });

        // FORGOT PASSWORD
        txtForgotPassword.setOnClickListener(v -> {

            String email = edtEmail.getText().toString().trim();

            if(email.isEmpty()){
                Toast.makeText(LoginActivity.this,
                        "Enter your email first", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(LoginActivity.this,
                                "Password reset email sent!",
                                Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(LoginActivity.this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        });
    }
}
