package com.android.example.cataractdetectionapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startActivity(new Intent(SplashScreenActivity.this, MainActivity.class));
        finish();

//        new Handler().postDelayed(() -> {
//            Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
//            startActivity(intent);
//            finish();
//        }, 2000);
    }
}