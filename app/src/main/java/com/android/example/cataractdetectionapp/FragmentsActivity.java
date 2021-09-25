package com.android.example.cataractdetectionapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.MANAGE_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class FragmentsActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final String PATH = Environment.getExternalStorageDirectory().getAbsolutePath();

    public Module module;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragments);

        if(!checkPermission()){
            requestPermission();
        }

        createDirectory();

        load();

        InitialFragment initialFragmentInstance = new InitialFragment();
        FragmentManager initialFragmentManager = getSupportFragmentManager();
        FragmentTransaction initialFragmentTransaction = initialFragmentManager.beginTransaction();
        initialFragmentTransaction.replace(R.id.fragment_act, initialFragmentInstance, "Initial Fragment")
                .addToBackStack(null).commit();

    }

    private void createDirectory(){
        new Thread(() -> {
            File file = new File(PATH, File.separator+"Cataract Grading");
            if(!file.exists()){
                boolean mkdir = file.mkdir();
            }
        }).start();
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
        new AlertDialog.Builder(FragmentsActivity.this)
                .setMessage("You need to allow access to all the permissions")
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    private void load(){
        loadModule();
    }

    @WorkerThread
    private void loadModule(){
        try {
            module = LiteModuleLoader.load(assetFilePath(getApplicationContext(), "Model_PyTorchCNN_forMobile_mine.ptl"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}