package com.android.example.cataractdetectionapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class InferenceActivity extends AppCompatActivity {

    private AutoCompleteTextView nuclear;
    private AutoCompleteTextView cortical;
    private AutoCompleteTextView posterior;
    private AutoCompleteTextView senile;

    private static final String PATH = Environment.getExternalStorageDirectory().getAbsolutePath();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inference);

        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        String age = intent.getStringExtra("age");
        String gender = intent.getStringExtra("gender");
        String eye = intent.getStringExtra("eye");
        String acuity1 = intent.getStringExtra("acuity_1");
        String acuity2 = "";
        if(intent.hasExtra("acuity_2")) acuity2 = intent.getStringExtra("acuity_2");
        String retroPath = intent.getStringExtra("retro");
        String diffusedPath = intent.getStringExtra("diffused");
        String obliquePath = intent.getStringExtra("oblique");
        String camInputs = intent.getStringExtra("camInputs");
        String[] split = camInputs.split("#");
        int[] rotate = new int[3];
        for(int i=0; i<3; i++){
            rotate[i] = Integer.parseInt(split[i]);
        }

        nuclear = findViewById(R.id.nuclear);
        cortical = findViewById(R.id.cortical);
        posterior = findViewById(R.id.posterior);
        senile = findViewById(R.id.senile);

        ArrayList<String> nuclearGrades = getNuclear();
        ArrayList<String> corticalGrades = getCortical();
        ArrayList<String> posteriorGrades = getPosterior();
        ArrayList<String> senileGrades = getSenile();

        ArrayAdapter<String> nuclearAdapter = new ArrayAdapter<>(InferenceActivity.this, R.layout.support_simple_spinner_dropdown_item, nuclearGrades);
        ArrayAdapter<String> corticalAdapter = new ArrayAdapter<>(InferenceActivity.this, R.layout.support_simple_spinner_dropdown_item, corticalGrades);
        ArrayAdapter<String> posteriorAdapter = new ArrayAdapter<>(InferenceActivity.this, R.layout.support_simple_spinner_dropdown_item, posteriorGrades);
        ArrayAdapter<String> senileAdapter = new ArrayAdapter<>(InferenceActivity.this, R.layout.support_simple_spinner_dropdown_item, senileGrades);

        nuclear.setAdapter(nuclearAdapter);
        cortical.setAdapter(corticalAdapter);
        posterior.setAdapter(posteriorAdapter);
        senile.setAdapter(senileAdapter);

        nuclear.setText(nuclearAdapter.getItem(0), false);
        nuclear.setFreezesText(false);
        cortical.setText(corticalAdapter.getItem(0), false);
        cortical.setFreezesText(false);
        posterior.setText(posteriorAdapter.getItem(0), false);
        posterior.setFreezesText(false);

        Button saveData = findViewById(R.id.save_data);
        Button next = findViewById(R.id.next);

        String finalAcuity = acuity2;
        AtomicBoolean val = new AtomicBoolean(false);
        saveData.setOnClickListener(view -> {
            val.set(saveImageData(retroPath, diffusedPath, obliquePath, acuity1, finalAcuity, eye, rotate));
            Toast.makeText(this, val.get() ?"Image data saved successfully!":"Check the 4 input fields and images before saving!", Toast.LENGTH_SHORT).show();
        });

        next.setOnClickListener(view -> startInitialActivity(val, retroPath, diffusedPath, obliquePath));
    }

    private ArrayList<String> getNuclear(){
        ArrayList<String> nuclearGrades = new ArrayList<>();
        for(int i=0; i<=6; i++){
            nuclearGrades.add("NO"+i);
        }
        return nuclearGrades;
    }

    private ArrayList<String> getCortical(){
        ArrayList<String> corticalGrades = new ArrayList<>();
        for(int i=0; i<=5; i++){
            corticalGrades.add("C"+i);
        }
        return corticalGrades;
    }

    private ArrayList<String> getPosterior(){
        ArrayList<String> posteriorGrades = new ArrayList<>();
        for(int i=0; i<=5; i++){
            posteriorGrades.add("P"+i);
        }
        return posteriorGrades;
    }

    private ArrayList<String> getSenile(){
        return new ArrayList<>(Arrays.asList("None", "MSC", "HMSC", "PPC"));
    }

    private boolean saveImageData(String retroPath, String diffusedPath, String obliquePath, String vision1, String vision2, String eye, int[] rotate){
        if(areImages(retroPath, diffusedPath, obliquePath)) return false;
        String ns = nuclear.getText().toString();
        String c = cortical.getText().toString();
        String p = posterior.getText().toString();
        String msc = senile.getText().toString();
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("dd-MM-yyyy_HHmmss_").format(new Date());

        String currPath = "";
        if(!ns.trim().equals("")) currPath += ns+"_";
        if(!c.trim().equals("")) currPath += c+"_";
        if(!p.trim().equals("")) currPath += p+"_";
        if(!msc.trim().equals("")) currPath += msc+"_";
        if(vision1!=null && vision2!=null) currPath += (vision1.equals("Normal") ? vision2 : vision1) + "_";
        currPath += eye;

        File dir = new File(PATH+"/Pictures", currPath+"_"+timeStamp);
        if(!dir.exists()) {
            boolean mkdir = dir.mkdir();
        }

        boolean r = false, d = false, o = false;
        if(!retroPath.trim().equals("")) r = saveImageUtil(retroPath, dir.getPath() + "/retro", rotate[0]);
        if(!diffusedPath.trim().equals("")) d = saveImageUtil(diffusedPath, dir.getPath()+"/diffused", rotate[1]);
        if(!obliquePath.trim().equals("")) o = saveImageUtil(obliquePath, dir.getPath()+"/oblique", rotate[2]);

        return r || d || o;
    }

    private boolean saveImageUtil(String srcPath, String destPath, int toRotate){
        final int[] signal = {-1};
        new Thread(() -> {
            Bitmap bmp = BitmapFactory.decodeFile(srcPath);
            int width = bmp.getWidth();
            int height = bmp.getHeight();
            if(toRotate==1){
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
            }
            File file = new File(destPath+".jpg");
            if (file.exists ()) {
                boolean delete = file.delete();
            }
            try {
                FileOutputStream out = new FileOutputStream(file);
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
                out.close();
            } catch (Exception e) {
                signal[0] = 1;
                e.printStackTrace();
            }
        }).start();
        return signal[0]==-1;
    }

    private boolean areImages(String retroPath, String diffusedPath, String obliquePath){
        return retroPath.trim().equals("") && diffusedPath.trim().equals("") && obliquePath.trim().equals("");
    }

    private void startInitialActivity(AtomicBoolean val, String retroPath, String diffusedPath, String obliquePath){
        if(val.get()) deleteTempFiles(retroPath, diffusedPath, obliquePath);
        Intent intent = new Intent(InferenceActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void deleteTempFiles(String retroPath, String diffusedPath, String obliquePath){
        new Thread(() -> {
            if(!retroPath.trim().equals("")){
                boolean isDeleted = new File(retroPath).getAbsoluteFile().delete();
            }
            if(!diffusedPath.trim().equals("")){
                boolean isDeleted = new File(diffusedPath).getAbsoluteFile().delete();
            }
            if(!obliquePath.trim().equals("")){
                boolean isDeleted = new File(obliquePath).getAbsoluteFile().delete();
            }
        }).start();
    }
}