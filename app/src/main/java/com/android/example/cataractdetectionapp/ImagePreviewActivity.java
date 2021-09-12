package com.android.example.cataractdetectionapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;

public class ImagePreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        Intent intent = getIntent();
        PhotoView imageView = findViewById(R.id.image);
        String imagePath = intent.getStringExtra("imagePath");
        Glide.with(this).load(imagePath).into(imageView);
    }
}