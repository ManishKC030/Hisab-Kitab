package com.example.hisabkitab;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AccountActivity extends Activity {

    TextView txtName, txtEmail;
    Button btnLogout;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.account);

        txtName = findViewById(R.id.txtName);
        txtEmail = findViewById(R.id.txtEmail);
        btnLogout = findViewById(R.id.btnLogout);

        mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            txtEmail.setText(user.getEmail());

            if (user.getDisplayName() != null) {
                txtName.setText(user.getDisplayName());
            } else {
                txtName.setText("Welcome User");
            }
        }

        btnLogout.setOnClickListener(v -> {

            mAuth.signOut();

            Intent intent = new Intent(AccountActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}

