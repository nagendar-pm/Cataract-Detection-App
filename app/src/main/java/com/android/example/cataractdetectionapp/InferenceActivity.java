package com.android.example.cataractdetectionapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
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

    private Interpreter tflite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inference);

        createDirectory();

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
            layout1.setVisibility(View.VISIBLE);
            layout2.setVisibility(View.VISIBLE);
            saveData.setVisibility(View.VISIBLE);
            next.setVisibility(View.VISIBLE);
            load();
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

    @WorkerThread
    public void load(){
        loadModel();
        tflite.allocateTensors();
        int[] shape = tflite.getInputTensor(0).shape();
        int WIDTH = shape[1];
        int HEIGHT = shape[2];
        Bitmap bmp1 = BitmapFactory.decodeFile(retroPath);
        Bitmap bmp2 = BitmapFactory.decodeFile(diffusedPath);
        Bitmap bmp3 = BitmapFactory.decodeFile(obliquePath);
        Bitmap resized1 = Bitmap.createScaledBitmap(bmp1, WIDTH, HEIGHT, true);
        Bitmap resized2 = Bitmap.createScaledBitmap(bmp2, WIDTH, HEIGHT, true);
        Bitmap resized3 = Bitmap.createScaledBitmap(bmp3, WIDTH, HEIGHT, true);

//        convertBitmapToByteBuffer();
//
        ByteBuffer b1 = ByteBuffer.allocate(bmp1.getRowBytes()*bmp1.getHeight());
        ByteBuffer b2 = ByteBuffer.allocate(bmp2.getRowBytes()*bmp2.getHeight());
        ByteBuffer b3 = ByteBuffer.allocate(bmp3.getRowBytes()*bmp3.getHeight());

        bmp1.copyPixelsToBuffer(b1);
        bmp2.copyPixelsToBuffer(b2);
        bmp3.copyPixelsToBuffer(b3);

        double[] op = new double[4];
        op[0] = op[1] = op[2] = op[3] = 0.0;
        tflite.run(new ByteBuffer[]{b1, b2, b3}, op);
        Log.d("output ", "load: "+Arrays.toString(op));
    }


    @WorkerThread
    private synchronized void loadModel() {
        try {
            ByteBuffer buffer = loadModelFile(mContext.getAssets(), "AIVision_model1.tflite");
            tflite = new Interpreter(buffer);
            Log.v("model ", "TFLite model loaded.");
        } catch (IOException ex) {
            Log.e("model ", ex.getMessage());
        }
    }

    private ByteBuffer convertBitmapToByteBuffer_float(Bitmap bitmap, int BATCH_SIZE, int inputSize, int PIXEL_SIZE) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * BATCH_SIZE *
                inputSize * inputSize * PIXEL_SIZE); //float_size = 4 bytes
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[inputSize * inputSize];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0,
                bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                final int val = intValues[pixel++];


                byteBuffer.putFloat( ((val >> 16) & 0xFF)* (1.f/255.f));
                byteBuffer.putFloat( ((val >> 8) & 0xFF)* (1.f/255.f));
                byteBuffer.putFloat( (val & 0xFF)* (1.f/255.f));
            }
        }
        return byteBuffer;
    }

    public static MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath)
            throws IOException {
        try (AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
             FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor())) {
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
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
        System.out.println("src "+srcPath);
        System.out.println("dest "+destPath);
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