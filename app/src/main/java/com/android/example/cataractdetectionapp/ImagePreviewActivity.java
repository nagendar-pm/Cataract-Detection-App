package com.android.example.cataractdetectionapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.widget.ImageView;

public class ImagePreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        Intent intent = getIntent();
        ImageView imageView = findViewById(R.id.image);
        String imagePath = intent.getStringExtra("imagePath");
        int rotation = intent.getIntExtra("rotation",1);
        Bitmap bmp = BitmapFactory.decodeFile(imagePath);
        if(rotation==0){
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
        }
        imageView.setImageBitmap(bmp);
    }
}