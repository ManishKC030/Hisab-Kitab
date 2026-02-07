package com.example.hisab_kitab;

import android.app.Activity;
import android.os.*;
import android.R;


public class MainActivity  extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // THIS line connects your XML to the screen
        setContentView(R.layout.login);}
}
