package com.allinone.capstoneproject;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class CabRedirectActivity extends AppCompatActivity {
    final String TAG = getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cab_redirect);

//        Intent intent = getIntent();
//        String auth_code = intent.getStringExtra("auth_code");
//        Log.d(TAG, auth_code);
    }
}
