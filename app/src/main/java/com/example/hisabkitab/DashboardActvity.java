package com.example.hisabkitab;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.widget.*;

public class DashboardActvity extends Activity {
    LinearLayout navBtnAccount;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.dashboard);
        navBtnAccount = findViewById(R.id.navBtnAccount);

        navBtnAccount.setOnClickListener(v -> {

            Intent intent = new Intent(DashboardActvity.this, AccountActivity.class);
            startActivity(intent);

        });

    }
}
