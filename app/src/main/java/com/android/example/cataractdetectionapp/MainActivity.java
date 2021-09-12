package com.android.example.cataractdetectionapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.MANAGE_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;
    private ActivityResultLauncher<Intent> camLaunch;
    private ActivityResultLauncher<Intent> galleryLaunch;

    private AutoCompleteTextView acuityType;
    private EditText visionInput2;

    private ImageView mRetroImage;
    private ImageView mDiffusedImage;
    private ImageView mObliqueImage;

    private File photoFile = null;
    private String retroPath = "";
    private String diffusedPath = "";
    private String obliquePath = "";

    final int[] camInput = {-1};
    final int[] galleryInput = {-1};
    int decision = 0;
    String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        EditText name = findViewById(R.id.name);
        EditText age = findViewById(R.id.age);
//        visionInput1 = findViewById(R.id.vision_1);
        visionInput2 = findViewById(R.id.vision_2);

        AutoCompleteTextView genderView = findViewById(R.id.gender);
        AutoCompleteTextView eyeView = findViewById(R.id.eye);
        acuityType = findViewById(R.id.vision_1);

        ArrayList<String> genders = getGenders();
        ArrayList<String> eyes = getEyePositions();
        ArrayList<String> types = getTestTypes();
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.support_simple_spinner_dropdown_item, genders);
        ArrayAdapter<String> eyeAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.support_simple_spinner_dropdown_item, eyes);
        ArrayAdapter<String> testTypeAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.support_simple_spinner_dropdown_item, types);

        genderView.setAdapter(genderAdapter);
        eyeView.setAdapter(eyeAdapter);
        acuityType.setAdapter(testTypeAdapter);

        acuityType.setText(testTypeAdapter.getItem(0), false);
        acuityType.setFreezesText(false);

        if(!checkPermission()){
            requestPermission();
        }

        camLaunch = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    int resultCode = result.getResultCode();
                    System.out.println(result.toString());
                    if (resultCode == RESULT_OK) {
                        String currImagePath = photoFile.getAbsolutePath();
                        Bitmap selectedImage = BitmapFactory.decodeFile(currImagePath);
//                        saveImage(selectedImage, camInput[0]);
                        switch (camInput[0]){
                            case 1:
                                retroPath = currImagePath;
                                mRetroImage.setImageBitmap(selectedImage);
                                break;
                            case 2:
                                diffusedPath = currImagePath;
                                mDiffusedImage.setImageBitmap(selectedImage);
                                break;
                            case 3:
                                obliquePath = currImagePath;
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
                                        retroPath = picturePath;
                                        mRetroImage.setImageBitmap(BitmapFactory.decodeFile(picturePath));
                                        break;
                                    case 2:
                                        diffusedPath = picturePath;
                                        mDiffusedImage.setImageBitmap(BitmapFactory.decodeFile(picturePath));
                                        break;
                                    case 3:
                                        obliquePath = picturePath;
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

        submit.setOnClickListener(view -> submitInputs(name.getText().toString(), age.getText().toString(), genderView.getText().toString(), eyeView.getText().toString()));

    }

    private boolean checkPermission() {
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result2 = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);
        int result4 = ContextCompat.checkSelfPermission(getApplicationContext(), INTERNET);
        int result5 = ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA);
        boolean result = result1== PackageManager.PERMISSION_GRANTED && result2==PackageManager.PERMISSION_GRANTED
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
        new AlertDialog.Builder(MainActivity.this)
                .setMessage("You need to allow access to all the permissions")
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void chooseImagePicker(){
        final CharSequence[] optionsMenu = {"Capture from Camera", "Choose from Gallery", "Preview", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setItems(optionsMenu, (dialogInterface, i) -> {
            if(optionsMenu[i].equals("Capture from Camera")){
                decision = 0;
                Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                takePicture.putExtra("REQUEST_CODE", 0);

                // Ensure that there's a camera activity to handle the intent
                if (takePicture.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        Log.d("ImagePicker ", "chooseImagePicker: "+ex);
                        // Error occurred while creating the File
                    }
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        new Thread(() -> {
                            Uri photoURI = FileProvider.getUriForFile(Objects.requireNonNull(getApplicationContext()),
                                    BuildConfig.APPLICATION_ID + ".provider", photoFile);
                            takePicture.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                            camLaunch.launch(takePicture);
                        }).start();

                    }
                }
            }
            else if(optionsMenu[i].equals("Choose from Gallery")){
                decision = 1;
                Intent choosePicture = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                choosePicture.putExtra("REQUEST_CODE", 0);
                galleryLaunch.launch(choosePicture);
            }
            else if(optionsMenu[i].equals("Preview")){
                Intent displayImg = new Intent(this, ImagePreviewActivity.class);
                String path;
                switch (camInput[0]){
                    case 2:
                        path = diffusedPath;
                        break;
                    case 3:
                        path = obliquePath;
                        break;
                    case 1:
                    default:
                        path = retroPath;
                        break;
                }
                if(!path.trim().equals("")){
                    displayImg.putExtra("imagePath", path);
                    displayImg.putExtra("rotation", decision);
                    startActivity(displayImg);
                }
            }
            else{
                dialogInterface.dismiss();
            }
        }).create();
        builder.show();
    }

    private void submitInputs(String name, String age, String gender, String eye){
        Intent intent = new Intent(MainActivity.this, InferenceActivity.class);
        intent.putExtra("name", name);
        intent.putExtra("age", age);
        intent.putExtra("gender", gender);
        intent.putExtra("eye", eye);

        intent.putExtra("acuity_1", acuityType.getText().toString());
        System.out.println("acuity "+acuityType.getText()+" h "+visionInput2.getText()+" e "+eye);
        if(!visionInput2.getText().toString().trim().equals("")) intent.putExtra("acuity_2", visionInput2.getText().toString());

        intent.putExtra("retro", retroPath);
        intent.putExtra("diffused", diffusedPath);
        intent.putExtra("oblique", obliquePath);

        startActivity(intent);
        finish();
    }

    private ArrayList<String> getGenders(){
        return new ArrayList<>(Arrays.asList("Male", "Female"));
    }

    private ArrayList<String> getEyePositions(){
        return new ArrayList<>(Arrays.asList("OS", "OD"));
    }

    private ArrayList<String> getTestTypes(){
        return new ArrayList<>(Arrays.asList("Normal", "CF1", "CF2", "CF3", "HM", "PL/PR"));
    }

    private File createImageFile() throws IOException {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("dd-MM-yyyy_HHmmss_").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);

        if(!retroPath.trim().equals("")) outState.putParcelable("retro", Uri.parse(retroPath));
        if(!diffusedPath.trim().equals("")) outState.putParcelable("diffused", Uri.parse(diffusedPath));
        if(!obliquePath.trim().equals("")) outState.putParcelable("oblique", Uri.parse(obliquePath));
    }

    @Override
    public void onRestoreInstanceState(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);

        assert savedInstanceState != null;
        retroPath = savedInstanceState.getParcelable("retro");
        diffusedPath = savedInstanceState.getParcelable("diffused");
        obliquePath = savedInstanceState.getParcelable("oblique");
    }
}