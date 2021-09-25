package com.android.example.cataractdetectionapp;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Arrays;

///**
// * A simple {@link Fragment} subclass.
// * Use the {@link InitialFragment#newInstance} factory method to
// * create an instance of this fragment.
// */
public class InitialFragment extends Fragment {
    private static final int PERMISSION_REQUEST_CODE = 200;

    private AutoCompleteTextView acuityType;
    private EditText visionInput1;
    private EditText visionInput2;

    private static final String PATH = Environment.getExternalStorageDirectory().getAbsolutePath();

//    // TODO: Rename parameter arguments, choose names that match
//    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//    private static final String ARG_PARAM1 = "param1";
//    private static final String ARG_PARAM2 = "param2";
//
//    // TODO: Rename and change types of parameters
//    private String mParam1;
//    private String mParam2;

    public InitialFragment() {
        // Required empty public constructor
    }

//    /**
//     * Use this factory method to create a new instance of
//     * this fragment using the provided parameters.
//     *
//     * @param param1 Parameter 1.
//     * @param param2 Parameter 2.
//     * @return A new instance of fragment InitialFragment.
//     */
    // TODO: Rename and change types and number of parameters
//    public static InitialFragment newInstance(String param1, String param2) {
//        InitialFragment fragment = new InitialFragment();
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
       View view = inflater.inflate(R.layout.fragment_initial, container, false);

//        InferenceFragment inferenceFragment = new InferenceFragment();
//        Button submit = view.findViewById(R.id.button);
//        TextView textView = view.findViewById(R.id.textView);
//        textView.setText("Initial Fragment");
//        submit.setOnClickListener(view1 -> requireActivity().getSupportFragmentManager().beginTransaction()
//                .replace(R.id.fragment_act, new InferenceFragment())
//                .addToBackStack(null).commit());s

        Button submit = view.findViewById(R.id.submit);
        EditText name = view.findViewById(R.id.name);
        EditText age = view.findViewById(R.id.age);
        visionInput1 = view.findViewById(R.id.vision_1);
        visionInput2 = view.findViewById(R.id.vision_2);

        AutoCompleteTextView genderView = view.findViewById(R.id.gender);
        AutoCompleteTextView eyeView = view.findViewById(R.id.eye);
        acuityType = view.findViewById(R.id.vision_type);

        ArrayList<String> genders = getGenders();
        ArrayList<String> eyes = getEyePositions();
        ArrayList<String> types = getTestTypes();
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(getContext(), R.layout.support_simple_spinner_dropdown_item, genders);
        ArrayAdapter<String> eyeAdapter = new ArrayAdapter<>(getContext(), R.layout.support_simple_spinner_dropdown_item, eyes);
        ArrayAdapter<String> testTypeAdapter = new ArrayAdapter<>(getContext(), R.layout.support_simple_spinner_dropdown_item, types);

        genderView.setAdapter(genderAdapter);
        eyeView.setAdapter(eyeAdapter);
        acuityType.setAdapter(testTypeAdapter);

        acuityType.setText(testTypeAdapter.getItem(0), false);
        acuityType.setFreezesText(false);

        submit.setOnClickListener(v -> submitInputs(name.getText().toString(), age.getText().toString(), genderView.getText().toString(), eyeView.getText().toString()));


        return view;
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
        // TODO: Intent to fragment
//        Intent intent = new Intent(MainActivity.this, InferenceActivity.class);
//        intent.putExtra("name", name);
//        intent.putExtra("age", age);
//        intent.putExtra("gender", gender);
//        intent.putExtra("eye", eye);
//
//        intent.putExtra("acuity_type", acuityType.getText().toString());
//        if(!visionInput1.getText().toString().trim().equals("")) intent.putExtra("acuity_1", visionInput1.getText().toString());
//        if(!visionInput2.getText().toString().trim().equals("")) intent.putExtra("acuity_2", visionInput2.getText().toString());
//
//        startActivity(intent);

        InferenceFragment inferenceFragment = new InferenceFragment();
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_act, new InferenceFragment())
                .addToBackStack(null).commit();

    }
}