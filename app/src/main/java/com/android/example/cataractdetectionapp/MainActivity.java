package com.android.example.cataractdetectionapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.MANAGE_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;

    private AutoCompleteTextView acuityType;
    private EditText visionInput1;
    private EditText visionInput2;

    private static final String PATH = Environment.getExternalStorageDirectory().getAbsolutePath();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createDirectory();

        Button submit = findViewById(R.id.submit);
        EditText name = findViewById(R.id.name);
        EditText age = findViewById(R.id.age);
        visionInput1 = findViewById(R.id.vision_1);
        visionInput2 = findViewById(R.id.vision_2);

        AutoCompleteTextView genderView = findViewById(R.id.gender);
        AutoCompleteTextView eyeView = findViewById(R.id.eye);
        acuityType = findViewById(R.id.vision_type);

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

    private void createDirectory(){
        new Thread(() -> {
            File file = new File(PATH, File.separator+"Cataract Grading");
            if(!file.exists()){
                boolean mkdir = file.mkdir();
            }
        }).start();
    }

    private ArrayList<String> getGenders(){
        return new ArrayList<>(Arrays.asList("Male", "Female"));
    }

    private ArrayList<String> getEyePositions(){
        return new ArrayList<>(Arrays.asList("OS", "OD"));
    }

    private ArrayList<String> getTestTypes(){
        return new ArrayList<>(Arrays.asList("Standard", "CF1", "CF2", "CF3", "HM", "PL/PR"));
    }

    private void submitInputs(String name, String age, String gender, String eye){
        Intent intent = new Intent(MainActivity.this, InferenceActivity.class);
        intent.putExtra("name", name);
        intent.putExtra("age", age);
        intent.putExtra("gender", gender);
        intent.putExtra("eye", eye);

        intent.putExtra("acuity_type", acuityType.getText().toString());
        if(!visionInput1.getText().toString().trim().equals("")) intent.putExtra("acuity_1", visionInput1.getText().toString());
        if(!visionInput2.getText().toString().trim().equals("")) intent.putExtra("acuity_2", visionInput2.getText().toString());

        startActivity(intent);
    }
}