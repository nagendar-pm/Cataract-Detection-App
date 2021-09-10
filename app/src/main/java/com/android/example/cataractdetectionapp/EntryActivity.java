package com.android.example.cataractdetectionapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.MANAGE_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class EntryActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 200;
    private ActivityResultLauncher<Intent> camLaunch;
    private ActivityResultLauncher<Intent> galleryLaunch;
    private ActivityResultLauncher<Intent> mainActivityLaunch;
    private ImageView mRetroImage;
    private ImageView mDiffusedImage;
    private ImageView mObliqueImage;
    private EditText visionInput;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        View mRetroView = findViewById(R.id.retro_input);
        View mDiffusedView = findViewById(R.id.diffused_input);
        View mObliqueView = findViewById(R.id.oblique_input);

        TextView mRetroText = mRetroView.findViewById(R.id.image_name);
        TextView mDiffusedText = mDiffusedView.findViewById(R.id.image_name);
        TextView mObliqueText = mObliqueView.findViewById(R.id.image_name);

        mRetroImage = mRetroView.findViewById(R.id.image_input);
        mDiffusedImage = mDiffusedView.findViewById(R.id.image_input);
        mObliqueImage = mObliqueView.findViewById(R.id.image_input);

        mRetroText.setText(R.string.retro);
        mDiffusedText.setText(R.string.diffused);
        mObliqueText.setText(R.string.oblique);

        Button submit = findViewById(R.id.submit);
        visionInput = new EditText(this);
        visionInput.setInputType(InputType.TYPE_CLASS_TEXT);

        if(!checkPermission()){
            requestPermission();
        }
        final int[] camInput = {-1};
        final int[] galleryInput = {-1};

        camLaunch = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    int resultCode = result.getResultCode();
                    Intent data = result.getData();
                    if (resultCode == RESULT_OK && data != null) {
                        Bitmap selectedImage = (Bitmap) data.getExtras().get("data");
                        saveImage(selectedImage, camInput[0]);
                        switch (camInput[0]){
                            case 1:
                                mRetroImage.setImageBitmap(selectedImage);
                                break;
                            case 2:
                                mDiffusedImage.setImageBitmap(selectedImage);
                                break;
                            case 3:
                                mObliqueImage.setImageBitmap(selectedImage);
                                break;
                        }
                    }
                }
        );

        galleryLaunch = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    int resultCode = result.getResultCode();
                    Intent data = result.getData();
                    if (resultCode == RESULT_OK && data != null) {
                        Uri selectedImage = data.getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        if (selectedImage != null) {
                            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                            if (cursor != null) {
                                cursor.moveToFirst();
                                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                                String picturePath = cursor.getString(columnIndex);
                                switch (galleryInput[0]){
                                    case 1:
                                        mRetroImage.setImageBitmap(BitmapFactory.decodeFile(picturePath));
                                        break;
                                    case 2:
                                        mDiffusedImage.setImageBitmap(BitmapFactory.decodeFile(picturePath));
                                        break;
                                    case 3:
                                        mObliqueImage.setImageBitmap(BitmapFactory.decodeFile(picturePath));
                                        break;
                                }
                                cursor.close();
                            }
                        }
                    }
                }
        );

        mRetroImage.setOnClickListener(view -> {
            camInput[0] = 1;
            galleryInput[0] = 1;
            chooseImagePicker();
        });

        mDiffusedImage.setOnClickListener(view -> {
            camInput[0] = 2;
            galleryInput[0] = 2;
            chooseImagePicker();
        });

        mObliqueImage.setOnClickListener(view -> {
            camInput[0] = 3;
            galleryInput[0] = 3;
            chooseImagePicker();
        });

        submit.setOnClickListener(view -> getVisionTestSubmit());
    }

    private boolean checkPermission() {
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result2 = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);
        int result4 = ContextCompat.checkSelfPermission(getApplicationContext(), INTERNET);
        int result5 = ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA);
        boolean result = result1==PackageManager.PERMISSION_GRANTED && result2==PackageManager.PERMISSION_GRANTED
                && result4==PackageManager.PERMISSION_GRANTED && result5==PackageManager.PERMISSION_GRANTED;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            int result3 = ContextCompat.checkSelfPermission(getApplicationContext(), MANAGE_EXTERNAL_STORAGE);
            return result && result3==PackageManager.PERMISSION_GRANTED;
        }
        return result;
    }

    private void requestPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, INTERNET, CAMERA, MANAGE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
        else{
            ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, INTERNET,CAMERA}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean writeAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean readAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                boolean internetAccepted = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                boolean cameraAccepted = grantResults[3] == PackageManager.PERMISSION_GRANTED;
                boolean isR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
                boolean manageAccepted = false;
                if (isR)
                    manageAccepted = grantResults[4] == PackageManager.PERMISSION_GRANTED;
                if (writeAccepted && readAccepted && internetAccepted && cameraAccepted && isR == manageAccepted)
                    Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show();
                else {
                    Toast.makeText(this, "Permissions Denied, App cannot access storage & camera", Toast.LENGTH_SHORT).show();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (shouldShowRequestPermissionRationale(MANAGE_EXTERNAL_STORAGE)) {
                            showMessageOKCancel(
                                    (dialog, which) -> requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, INTERNET, CAMERA, MANAGE_EXTERNAL_STORAGE},
                                            PERMISSION_REQUEST_CODE));
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)) {
                                showMessageOKCancel(
                                        (dialog, which) -> requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, CAMERA, INTERNET},
                                                PERMISSION_REQUEST_CODE));
                            }
                        }
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showMessageOKCancel(DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(EntryActivity.this)
                .setMessage("You need to allow access to all the permissions")
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void chooseImagePicker(){
        final CharSequence[] optionsMenu = {"Capture from Camera", "Choose from Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(EntryActivity.this);
        builder.setItems(optionsMenu, (dialogInterface, i) -> {
            if(optionsMenu[i].equals("Capture from Camera")){
                Intent takePicture = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                takePicture.putExtra("REQUEST_CODE", 0);
                camLaunch.launch(takePicture);
            }
            else if(optionsMenu[i].equals("Choose from Gallery")){
                Intent choosePicture = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                choosePicture.putExtra("REQUEST_CODE", 0);
                galleryLaunch.launch(choosePicture);
            }
            else{
                dialogInterface.dismiss();
            }
        }).create();
        builder.show();
    }

    private void saveImage(Bitmap bmp, int type){
        @SuppressLint("SimpleDateFormat") String fileName = new SimpleDateFormat("dd-MM-yyyy_HHmmss_").format(new Date());
        switch (type){
            case 1:
                fileName = "retroImage_"+fileName;
                break;
            case 2:
                fileName = "diffusedImage_"+fileName;
                break;
            case 3:
                fileName = "ObliqueImage_"+fileName;
                break;
        }
        String basePath = Environment.getExternalStorageDirectory().getPath();
        System.out.println("base path "+basePath);
        File file = new File(basePath+"/Pictures/"+fileName+".jpg");
        if (file.exists ()) {
            boolean delete = file.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getVisionTestSubmit(){
        AlertDialog.Builder builder = new AlertDialog.Builder(EntryActivity.this);

        if(visionInput.getParent()!=null) ((ViewGroup)visionInput.getParent()).removeView(visionInput);

        builder.setMessage("Please provide Vision Test results")
                .setView(visionInput)
                .setPositiveButton("Submit", (dialog, id) -> {
                    String input = visionInput.getText().toString();
                    try {
                        submit(input);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("Cancel", (dialog, id) -> {
                    dialog.cancel();
                    Toast.makeText(getApplicationContext(),"Cancelled",
                            Toast.LENGTH_SHORT).show();
                });
        AlertDialog alert = builder.create();
        alert.setTitle("Vision Test Input");
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }

    private void submit(String input) throws IOException {
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        mainActivityIntent.putExtra("vision_test_input", input);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        Bitmap retroBmp = ((BitmapDrawable) mRetroImage.getDrawable()).getBitmap();
//        Bitmap retroBmp = BitmapFactory.decodeResource(getResources(), R.id.retro_input);
        retroBmp.compress(Bitmap.CompressFormat.PNG, 0, stream);
        byte[] retroByteArray = stream.toByteArray();

        stream.reset();

        Bitmap diffusedBmp = ((BitmapDrawable) mDiffusedImage.getDrawable()).getBitmap();
//        Bitmap diffusedBmp = BitmapFactory.decodeResource(getResources(), R.id.retro_input);
        diffusedBmp.compress(Bitmap.CompressFormat.PNG, 0, stream);
        byte[] diffusedByteArray = stream.toByteArray();

        stream.reset();

        Bitmap obliqueBmp = ((BitmapDrawable) mObliqueImage.getDrawable()).getBitmap();
//        Bitmap obliqueBmp = BitmapFactory.decodeResource(getResources(), R.id.retro_input);
        obliqueBmp.compress(Bitmap.CompressFormat.PNG, 0, stream);
        byte[] obliqueByteArray = stream.toByteArray();

        stream.close();

        mainActivityIntent.putExtra("retro_input", retroByteArray);
        mainActivityIntent.putExtra("diffused_input", diffusedByteArray);
        mainActivityIntent.putExtra("oblique_input", obliqueByteArray);

        startActivity(mainActivityIntent);
    }
}