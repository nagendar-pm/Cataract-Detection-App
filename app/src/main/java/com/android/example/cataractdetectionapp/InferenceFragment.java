package com.android.example.cataractdetectionapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.util.Arrays;

///**
// * A simple {@link Fragment} subclass.
// * Use the {@link InferenceFragment#newInstance} factory method to
// * create an instance of this fragment.
// */
public class InferenceFragment extends Fragment {

    public FragmentsActivity fragmentsActivity;
    private Module module;

//    // TODO: Rename parameter arguments, choose names that match
//    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//    private static final String ARG_PARAM1 = "param1";
//    private static final String ARG_PARAM2 = "param2";
//
//    // TODO: Rename and change types of parameters
//    private String mParam1;
//    private String mParam2;

    public InferenceFragment() {
        // Required empty public constructor
    }

//    /**
//     * Use this factory method to create a new instance of
//     * this fragment using the provided parameters.
//     *
//     * @param param1 Parameter 1.
//     * @param param2 Parameter 2.
//     * @return A new instance of fragment InferenceFragment.
//     */
//    // TODO: Rename and change types and number of parameters
//    public static InferenceFragment newInstance(String param1, String param2) {
//        InferenceFragment fragment = new InferenceFragment();
//        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
//        fragment.setArguments(args);
//        return fragment;
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inference, container, false);

        fragmentsActivity = (FragmentsActivity) getActivity();
        assert fragmentsActivity != null;
        module = fragmentsActivity.module;

        InitialFragment initialFragment = new InitialFragment();
        Button submit = view.findViewById(R.id.button);
        TextView textView = view.findViewById(R.id.textView);
        textView.setText("Inference Fragment");
        submit.setOnClickListener(view1 -> {
            requireActivity().getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_act, new InitialFragment())
                    .addToBackStack(null).commit();
        });

        try {
            @SuppressLint("WrongThread") int[] inf = inference(null, null, null);
            Toast.makeText(getContext(), Arrays.toString(inf), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return view;
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

    @WorkerThread
    private int[] inference(String diffusedPath, String obliquePath, String retroPath) throws IOException {
        Bitmap bmp1, bmp2, bmp3;

        bmp1 = BitmapFactory.decodeFile(diffusedPath!=null && new File(diffusedPath).exists()?diffusedPath:assetFilePath(requireContext(), "black_img.jpeg"));
        bmp2 = BitmapFactory.decodeFile(obliquePath!=null && new File(obliquePath).exists()?obliquePath:assetFilePath(requireContext(), "black_img.jpeg"));
        bmp3 = BitmapFactory.decodeFile(retroPath!=null && new File(retroPath).exists()?retroPath:assetFilePath(requireContext(), "black_img.jpeg"));

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
}