package com.android.example.cataractdetectionapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.MANAGE_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;

    private AutoCompleteTextView acuityType;
    private AutoCompleteTextView visionInput1;
    private AutoCompleteTextView visionInput2;

    private static String PATH = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!checkPermission()){
            requestPermission();
        }

        setPath();
        System.out.println("path "+PATH);
        createDirectory();

        Button submit = findViewById(R.id.submit);
        EditText age = findViewById(R.id.age);
        visionInput1 = findViewById(R.id.vision_1);
        visionInput2 = findViewById(R.id.vision_2);

        AutoCompleteTextView genderView = findViewById(R.id.gender);
        AutoCompleteTextView eyeView = findViewById(R.id.eye);
        acuityType = findViewById(R.id.vision_type);

        ArrayList<String> genders = getGenders();
        ArrayList<String> eyes = getEyePositions();
        ArrayList<String> types = getTestTypes();
        ArrayList<String> acuityResult1 = getAcuityResult1();
        ArrayList<String> acuityResult2 = getAcuityResult2();

        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.support_simple_spinner_dropdown_item, genders);
        ArrayAdapter<String> eyeAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.support_simple_spinner_dropdown_item, eyes);
        ArrayAdapter<String> testTypeAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.support_simple_spinner_dropdown_item, types);
        ArrayAdapter<String> result1Adapter = new ArrayAdapter<>(MainActivity.this, R.layout.support_simple_spinner_dropdown_item, acuityResult1);
        ArrayAdapter<String> result2Adapter = new ArrayAdapter<>(MainActivity.this, R.layout.support_simple_spinner_dropdown_item, acuityResult2);

        genderView.setAdapter(genderAdapter);
        eyeView.setAdapter(eyeAdapter);
        acuityType.setAdapter(testTypeAdapter);
        visionInput1.setAdapter(result1Adapter);
        visionInput2.setAdapter(result2Adapter);

        genderView.setText(genderAdapter.getItem(0), false);
        genderView.setFreezesText(false);

        eyeView.setText(eyeAdapter.getItem(0), false);
        eyeView.setFreezesText(false);

        acuityType.setText(testTypeAdapter.getItem(0), false);
        acuityType.setFreezesText(false);

        visionInput1.setFreezesText(false);
        visionInput2.setFreezesText(false);

        acuityType.setOnItemClickListener((adapterView, view, i, l) -> {
            if(i==0){
                runOnUiThread(() -> {
                    visionInput1.setEnabled(true);
                    visionInput1.setAdapter(result1Adapter);

                    visionInput2.setEnabled(true);
                    visionInput2.setAdapter(result2Adapter);
                });
            }
            else if(i>=1 && i<=7){
                runOnUiThread(() -> {
                    visionInput1.setText("");
                    visionInput1.dismissDropDown();
                    visionInput1.setAdapter(null);

                    visionInput2.setText("");
                    visionInput2.dismissDropDown();
                    visionInput2.setAdapter(null);
                });
            }
        });

        submit.setOnClickListener(view -> {
            if(TextUtils.isEmpty(age.getText())){
                age.setError("Required");
                return;
            }
            if(TextUtils.equals(acuityType.getText(), "Snellen Chart")){
                if(TextUtils.isEmpty(visionInput1.getText())){
                    visionInput1.setError("Required!");
                    return;
                }
                if(TextUtils.isEmpty(visionInput2.getText())){
                    visionInput2.setError("Required!");
                    return;
                }
            }
            submitInputs(age.getText().toString(), genderView.getText().toString(), eyeView.getText().toString());
        });

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

    @RequiresApi(api = Build.VERSION_CODES.R)
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
                        if (shouldShowRequestPermissionRationale(CAMERA)) {
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
                            if (shouldShowRequestPermissionRationale(CAMERA)) {
                                showMessageOKCancel(
                                        (dialog, which) -> requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, INTERNET, CAMERA, MANAGE_EXTERNAL_STORAGE},
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
        new MaterialAlertDialogBuilder(MainActivity.this)
                .setMessage("You need to allow access to all the permissions")
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void setPath(){
        File[] files = ContextCompat.getExternalFilesDirs(getApplicationContext(), null);
        Log.d("", "setPath: "+ Environment.getExternalStorageDirectory().getAbsolutePath());
        if(files.length!=0){
            for(File file : files){
                Log.d("", "setPath: "+file.getAbsolutePath());
            }
            PATH = files.length>=2?files[1].getAbsolutePath():Environment.getExternalStorageDirectory().getAbsolutePath();
        }
    }

    private void createDirectory(){
        new Thread(() -> {
            if(PATH==null) setPath();
            File file = new File(PATH, File.separator+"Cataract Grading");
            if(!file.exists()){
                boolean mkdir = file.mkdir();
            }
        }).start();
    }

    private ArrayList<String> getGenders(){
        return new ArrayList<>(Arrays.asList("Male", "Female", "Other"));
    }

    private ArrayList<String> getEyePositions(){
        return new ArrayList<>(Arrays.asList("Right", "Left"));
    }

    private ArrayList<String> getTestTypes(){
        return new ArrayList<>(Arrays.asList("Snellen Chart", "HM", "CFCF", "CF 1mts", "CF 2mts", "CF 3mts", "CF 4mts", "CF 5mts"));
    }

    private ArrayList<String> getAcuityResult1(){
        return new ArrayList<>(Collections.singletonList("6"));
    }

    private ArrayList<String> getAcuityResult2(){
        return new ArrayList<>(Arrays.asList("6", "9", "12", "18", "24", "36", "60"));
    }

    /**
     * @param age age of the given patient
     * @param gender gender of the given patient
     * @param eye whether left or right eye
     */
    private void submitInputs(String age, String gender, String eye){
        Intent intent = new Intent(MainActivity.this, InferenceActivity.class);
        intent.putExtra("age", age);
        intent.putExtra("gender", gender);
        intent.putExtra("eye", eye.equals("Right")?"OD":"OS");

        intent.putExtra("acuity_type", acuityType.getText().toString());
        if(!visionInput1.getText().toString().trim().equals("")) intent.putExtra("acuity_1", visionInput1.getText().toString());
        if(!visionInput2.getText().toString().trim().equals("")) intent.putExtra("acuity_2", visionInput2.getText().toString());

        startActivity(intent);
    }
}