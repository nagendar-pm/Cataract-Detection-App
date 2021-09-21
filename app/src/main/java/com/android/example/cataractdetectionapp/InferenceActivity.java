package com.android.example.cataractdetectionapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class InferenceActivity extends AppCompatActivity {
    private Context mContext;

    private File dir;
    private static final String PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final String BASE = PATH+"/Cataract Grading";

    private AutoCompleteTextView nuclear;
    private AutoCompleteTextView cortical;
    private AutoCompleteTextView posterior;
    private AutoCompleteTextView senile;

    private ActivityResultLauncher<Intent> camLaunch;
    private ActivityResultLauncher<Intent> galleryLaunch;

    private ImageView mRetroImage;
    private ImageView mDiffusedImage;
    private ImageView mObliqueImage;
    
    private File photoFile = null;
    private String retroPath = "";
    private String diffusedPath = "";
    private String obliquePath = "";

    final int[] camera = {-1};
    final int[] gallery = {-1};
    int[] rotate;
    String currentPhotoPath;

    private Module module;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inference);

        createDirectory();

        load();

        View mRetroView = findViewById(R.id.retro_input);
        View mDiffusedView = findViewById(R.id.diffused_input);
        View mObliqueView = findViewById(R.id.oblique_input);

        ConstraintLayout layout1 = findViewById(R.id.constraintLayout1);
        ConstraintLayout layout2 = findViewById(R.id.constraintLayout4);

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

        mContext = getApplicationContext();
        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        String age = intent.getStringExtra("age");
        String gender = intent.getStringExtra("gender");
        String eye = intent.getStringExtra("eye");
        String acuity_type = intent.getStringExtra("acuity_type");
        String acuity1 = "";
        String acuity2 = "";
        if(intent.hasExtra("acuity_1")) acuity1 = intent.getStringExtra("acuity_1");
        if(intent.hasExtra("acuity_2")) acuity2 = intent.getStringExtra("acuity_2");
        rotate = new int[3];

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

        AtomicBoolean val = new AtomicBoolean(false);
        dir = null;
        String finalAcuity1 = acuity1;
        String finalAcuity2 = acuity2;

        submit.setOnClickListener(view -> {
            if(retroPath.equals("") && diffusedPath.equals("") && obliquePath.equals("")){
                Toast.makeText(this, "Please provide the image inputs!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            layout1.setVisibility(View.VISIBLE);
            layout2.setVisibility(View.VISIBLE);
            saveData.setVisibility(View.VISIBLE);
            next.setVisibility(View.VISIBLE);
            int[] labels;
            try {
                labels = inference();

                labels[0] = labels[0]<=0?0:(labels[0]>=nuclearAdapter.getCount()?nuclearAdapter.getCount()-1:labels[0]);
                labels[1] = labels[1]<=0?0:(labels[1]>=corticalAdapter.getCount()?corticalAdapter.getCount()-1:labels[1]);
                labels[2] = labels[2]<=0?0:(labels[2]>=posteriorAdapter.getCount()?posteriorAdapter.getCount()-1:labels[2]);
                labels[3] = labels[3]<=0?0:(labels[3]>=senileAdapter.getCount()?senileAdapter.getCount()-1:labels[3]);

                nuclear.setText(nuclearAdapter.getItem(labels[0]), false);
                cortical.setText(corticalAdapter.getItem(labels[1]), false);
                posterior.setText(posteriorAdapter.getItem(labels[2]), false);
                senile.setText(senileAdapter.getItem(labels[3]), false);

                nuclear.setFreezesText(false);
                cortical.setFreezesText(false);
                posterior.setFreezesText(false);
                senile.setFreezesText(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        saveData.setOnClickListener(view -> {
            if(areNonImages(retroPath, diffusedPath, obliquePath)){
                val.set(false);
                Toast.makeText(this, "Check the 4 input fields and images before saving!", Toast.LENGTH_SHORT).show();
            }
            else{
                val.set(true);
                saveImageData(retroPath, diffusedPath, obliquePath, acuity_type, finalAcuity1, finalAcuity2, eye);
            }
        });

        next.setOnClickListener(view -> startInitialActivity(val, retroPath, diffusedPath, obliquePath));

        camLaunch = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    int resultCode = result.getResultCode();
                    if (resultCode == RESULT_OK) {
                        String currImagePath = photoFile.getAbsolutePath();
                        switch (camera[0]){
                            case 1:
                                retroPath = currImagePath;
                                rotate[0] = 1;
                                Glide.with(this).load(currImagePath).into(mRetroImage);
                                break;
                            case 2:
                                diffusedPath = currImagePath;
                                rotate[1] = 1;
                                Glide.with(this).load(currImagePath).into(mDiffusedImage);
                                break;
                            case 3:
                                obliquePath = currImagePath;
                                rotate[2] = 1;
                                Glide.with(this).load(currImagePath).into(mObliqueImage);
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
                                switch (gallery[0]){
                                    case 1:
                                        retroPath = picturePath;
                                        Glide.with(this).load(picturePath).into(mRetroImage);
                                        break;
                                    case 2:
                                        diffusedPath = picturePath;
                                        Glide.with(this).load(picturePath).into(mDiffusedImage);
                                        break;
                                    case 3:
                                        obliquePath = picturePath;
                                        Glide.with(this).load(picturePath).into(mObliqueImage);
                                        break;
                                }
                                cursor.close();
                            }
                        }
                    }
                }
        );

        mRetroView.setOnClickListener(view -> {
            camera[0] = 1;
            gallery[0] = 1;
            chooseImagePicker();
        });

        mDiffusedView.setOnClickListener(view -> {
            camera[0] = 2;
            gallery[0] = 2;
            chooseImagePicker();
        });

        mObliqueView.setOnClickListener(view -> {
            camera[0] = 3;
            gallery[0] = 3;
            chooseImagePicker();
        });
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

    @WorkerThread
    private int[] inference() throws IOException {
        Bitmap bmp1, bmp2, bmp3;

        bmp1 = BitmapFactory.decodeFile(new File(diffusedPath).exists()?diffusedPath:assetFilePath(getApplicationContext(), "black_img.jpeg"));
        bmp2 = BitmapFactory.decodeFile(new File(obliquePath).exists()?obliquePath:assetFilePath(getApplicationContext(), "black_img.jpeg"));
        bmp3 = BitmapFactory.decodeFile(new File(retroPath).exists()?retroPath:assetFilePath(getApplicationContext(), "black_img.jpeg"));

        int WIDTH = 189;
        int HEIGHT = 336;
        Bitmap resized1 = Bitmap.createScaledBitmap(bmp1, WIDTH, HEIGHT, true);
        Bitmap resized2 = Bitmap.createScaledBitmap(bmp2, WIDTH, HEIGHT, true);
        Bitmap resized3 = Bitmap.createScaledBitmap(bmp3, WIDTH, HEIGHT, true);

        float[] f1 = readImageData(resized1, HEIGHT, WIDTH);
        float[] f2 = readImageData(resized2, HEIGHT, WIDTH);
        float[] f3 = readImageData(resized3, HEIGHT, WIDTH);


        FloatBuffer buffer = Tensor.allocateFloatBuffer(3*3* WIDTH * HEIGHT);
        buffer.put(f1);
        buffer.put(f2);
        buffer.put(f3);

        Tensor inputTensor = Tensor.fromBlob(buffer, new long[]{1, 9, HEIGHT, WIDTH});

        Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
        float[] scores = outputTensor.getDataAsFloatArray();
        int[] output = new int[scores.length];
        for (int i = 0; i < scores.length; i++) {
            if(scores[i]<0){
                output[i] = 0;
                continue;
            }
            int temp = (int) (scores[i]);
            float curr = scores[i]*10;
            if((int)curr-temp<=5) output[i] = temp;
            else output[i] = temp+1;
        }
//        System.out.println("output "+Arrays.toString(scores));
//        System.out.println("output "+Arrays.toString(output));
        return output;
    }

    private float[] readImageData(Bitmap bmp, int ROWS, int COLS){
        float[][][][] data = new float[1][3][ROWS][COLS];

        for(int i=0; i<ROWS; i++){
            for(int j=0; j<COLS; j++){
                int pixel = bmp.getPixel(j, i);
                data[0][0][i][j] = (Color.red(pixel));
                data[0][1][i][j] = (Color.green(pixel));
                data[0][2][i][j] = (Color.blue(pixel));
            }
        }

        float[] result = new float[3 * ROWS * COLS];
        int index = 0;
        for(int i=0; i<ROWS; i++){
            for(int j=0; j<COLS; j++){
                for(int k=0; k<3; k++){
                    result[index++] = data[0][k][i][j];
                }
            }
        }

        return result;
    }

    private void chooseImagePicker(){
        final CharSequence[] optionsMenu = {"Capture from Camera", "Choose from Gallery", "Preview", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(InferenceActivity.this);
        builder.setItems(optionsMenu, (dialogInterface, i) -> {
            if(optionsMenu[i].equals("Capture from Camera")){
                Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                takePicture.putExtra("REQUEST_CODE", 0);
                if (takePicture.resolveActivity(getPackageManager()) != null) {
                    photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        Log.d("ImagePicker ", "chooseImagePicker: "+ex);
                    }
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
                Intent choosePicture = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                choosePicture.putExtra("REQUEST_CODE", 0);
                galleryLaunch.launch(choosePicture);
            }
            else if(optionsMenu[i].equals("Preview")){
                Intent displayImg = new Intent(this, ImagePreviewActivity.class);
                String path;
                switch (camera[0]){
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
                    startActivity(displayImg);
                }
            }
            else{
                dialogInterface.dismiss();
            }
        }).create();
        builder.show();
    }

    private void createDirectory(){
        new Thread(() -> {
            File file = new File(BASE);
            if(!file.exists()){
                boolean mkdir = file.mkdir();
            }
        }).start();
    }

    private File createImageFile() throws IOException {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss_").format(new Date());
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

    private void saveImageData(String retroPath, String diffusedPath, String obliquePath, String vision_type, String vision1, String vision2, String eye){
        new Thread(() -> {
            String ns = nuclear.getText().toString();
            String c = cortical.getText().toString();
            String p = posterior.getText().toString();
            String msc = senile.getText().toString();
            msc = msc.equals("None")?"":msc;
            @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss").format(new Date());

            String currPath = "";
            if(!ns.trim().equals("")) currPath += ns;
            if(!c.trim().equals("")) currPath += "_"+c;
            if(!p.trim().equals("")) currPath += "_"+p;
            if(!msc.trim().equals("")) currPath += "_"+msc;
            if(vision_type!=null){
                if(vision_type.equals("Standard")) currPath += vision1+"-"+vision2;
                else currPath += vision_type;
            }
            currPath += eye;

            dir = new File(BASE, currPath+(!msc.trim().equals("")?"_":"")+"_"+timeStamp);
            if(!dir.exists()) {
                boolean mkdir = dir.mkdir();
            }

            if(!retroPath.trim().equals("")) saveImageUtil(retroPath, dir.getPath() + "/retro", rotate[0]);
            if(!diffusedPath.trim().equals("")) saveImageUtil(diffusedPath, dir.getPath()+"/diffused", rotate[1]);
            if(!obliquePath.trim().equals("")) saveImageUtil(obliquePath, dir.getPath()+"/oblique", rotate[2]);

        }).start();
    }

    private void saveImageUtil(String srcPath, String destPath, int toRotate){
        final int[] signal = {-1};
        Thread t1 = new Thread(() -> {
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
        });
        Thread t2 = new Thread(() -> notifyMediaStoreScanner(destPath+".jpg"));
        t1.start();
        runOnUiThread(() -> Toast.makeText(mContext, "Image data saved successfully!", Toast.LENGTH_SHORT).show());
        try {
            t1.join();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        t2.start();
    }

    private boolean areNonImages(String retroPath, String diffusedPath, String obliquePath){
        return retroPath.trim().equals("") && diffusedPath.trim().equals("") && obliquePath.trim().equals("");
    }

    private void startInitialActivity(AtomicBoolean val, String retroPath, String diffusedPath, String obliquePath){
        if(val.get()) deleteTempFiles(retroPath, diffusedPath, obliquePath);
        Intent intent = new Intent(InferenceActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
//        finish();
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

    public final void notifyMediaStoreScanner(String path) {
        File file = new File(path);
        try {
            MediaStore.Images.Media.insertImage(mContext.getContentResolver(),
                    file.getAbsolutePath(), file.getName(), null);
            mContext.sendBroadcast(new Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
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
        Uri uri1 = savedInstanceState.getParcelable("retro");
        retroPath = uri1.getPath();
        Uri uri2 = savedInstanceState.getParcelable("diffused");
        diffusedPath = uri2.getPath();
        Uri uri3 = savedInstanceState.getParcelable("oblique");
        obliquePath = uri3.getPath();

        if(retroPath!=null && !retroPath.equals("")) Glide.with(this).load(retroPath).into(mRetroImage);
        if(diffusedPath!=null && !diffusedPath.equals("")) Glide.with(this).load(diffusedPath).into(mDiffusedImage);
        if(obliquePath!=null && !obliquePath.equals("")) Glide.with(this).load(obliquePath).into(mObliqueImage);
    }
}