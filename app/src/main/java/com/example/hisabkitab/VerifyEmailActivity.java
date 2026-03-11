package com.example.hisabkitab;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class VerifyEmailActivity extends Activity {

    TextView txtMessage;
    Button btnRefresh;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.verify_email);

        txtMessage = findViewById(R.id.txtMessage);
        btnRefresh = findViewById(R.id.btnRefresh);

        auth = FirebaseAuth.getInstance();

        txtMessage.setText("A verification email has been sent to your email. Please check your inbox or spam.");

        btnRefresh.setOnClickListener(v -> checkEmailVerification());
    }

    private void checkEmailVerification() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // Reload to get latest verification status
            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (user.isEmailVerified()) {
                        Toast.makeText(this, "Email verified! You can now login.", Toast.LENGTH_LONG).show();
                        auth.signOut(); // sign out only now
                        startActivity(new Intent(VerifyEmailActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Email not verified yet. Please check inbox/spam.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, "Failed to check verification. Try again.", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // This should rarely happen now
            Toast.makeText(this, "User session expired. Please login again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}