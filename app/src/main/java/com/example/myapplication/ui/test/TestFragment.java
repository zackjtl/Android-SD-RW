package com.example.myapplication.ui.test;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.myapplication.ILogListener;
import com.example.myapplication.LogOwner;
import com.example.myapplication.R;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class TestFragment extends LogOwner {

    private TestViewModel testViewModel;
    private ILogListener mLogListener = null;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        testViewModel = ViewModelProviders.of(this).get(TestViewModel.class);
        View root = inflater.inflate(R.layout.fragment_test, container, false);
        final TextView textView = root.findViewById(R.id.text_test);
        testViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });

        List<String> sizeStrings = new ArrayList<>();
        List<String> unitStrings = new ArrayList<>();

        for (int i = 1; i < 1024; i*=2) {
            sizeStrings.add(Integer.toString(i));
        }
        unitStrings.add("KB");
        unitStrings.add("MB");

        ArrayAdapter sizeApapter = new ArrayAdapter(getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                sizeStrings);
        ArrayAdapter unitAdapter = new ArrayAdapter(getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                unitStrings);

        final Spinner sizeSpinner = (Spinner)root.findViewById(R.id.sizeSpinner);
        final Spinner unitSpinner = (Spinner)root.findViewById(R.id.unitSpinner);
        sizeSpinner.setAdapter(sizeApapter);
        unitSpinner.setAdapter(unitAdapter);

        Spinner.OnItemSelectedListener listener = new onSizeUnitSelectedListener();

        sizeSpinner.setOnItemSelectedListener(listener);
        unitSpinner.setOnItemSelectedListener(listener);


        return root;
    }

    public class onSizeUnitSelectedListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Spinner sizeSpinner = (Spinner)getActivity().findViewById(R.id.sizeSpinner);
            Spinner unitSpinner = (Spinner)getActivity().findViewById(R.id.unitSpinner);
            String size = (String)sizeSpinner.getSelectedItem();
            String unit = (String)unitSpinner.getSelectedItem();
            testViewModel.setText("You selected: " + size + " " + unit);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }
}
