package com.example.sdtesttool.ui.test;

import android.Manifest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.sdtesttool.LogOwner;
import com.example.sdtesttool.MainActivity;
import com.example.sdtesttool.R;
import com.example.sdtesttool.ui.ButtonState;

import java.util.ArrayList;
import java.util.List;

public class TestFragment extends LogOwner {

    private TestViewModel testViewModel;
    private Handler msgHandler;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        assert getActivity() != null;
        assert getContext() != null;

        ActivityCompat.requestPermissions(getActivity(),
                new String[] {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                100);

        testViewModel = ViewModelProviders.of(getActivity()).get(TestViewModel.class);

        View root = inflater.inflate(R.layout.fragment_test, container, false);

        final ProgressBar progressBar = (ProgressBar)root.findViewById(R.id.progressBar);
        final TextView textMsg = (TextView)root.findViewById(R.id.textMsg);
        final Button goBtn = (Button)root.findViewById(R.id.gotButton);
        final TextView textWriteSpeed = (TextView)root.findViewById(R.id.textWriteSpeed);
        final TextView textReadSpeed = (TextView)root.findViewById(R.id.textReadSpeed);
        final TextView sizeRatioText = (TextView)root.findViewById(R.id.sizeRatioText);
        final SeekBar sizeRatioSeekBar = (SeekBar)root.findViewById(R.id.sizeRatioSeekBar);

        testViewModel.getProgress().observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                progressBar.setProgress(integer);
            }
        });
        testViewModel.getProgressMax().observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                progressBar.setMax(integer);
            }
        });
        testViewModel.getTextMsg().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                textMsg.setText(s);
            }
        });
        testViewModel.getGoButtonState().observe(getViewLifecycleOwner(), new Observer<ButtonState>() {
            @Override
            public void onChanged(ButtonState buttonState) {
                goBtn.setText(buttonState.caption);
                goBtn.setEnabled(buttonState.enabled);
            }
        });
        testViewModel.getWritePerformance().observe(getViewLifecycleOwner(), new Observer<Double>() {
            @Override
            public void onChanged(Double writePerformance) {
                if (writePerformance == 0) {
                    textWriteSpeed.setText("Write performance: --");
                }
                else {
                    String unitStr = "KB/s";

                    writePerformance /= 100;
                    if (writePerformance >= 1024) {
                        writePerformance /= 1024;
                        unitStr = "MB/s";
                    }
                    textWriteSpeed.setText(String.format("Write performance: %.2f %s", writePerformance, unitStr));
                }
            }
        });
        testViewModel.getReadPerformance().observe(getViewLifecycleOwner(), new Observer<Double>() {
            @Override
            public void onChanged(Double readPerformance) {
                if (readPerformance == 0) {
                    textReadSpeed.setText("Read performance: --");
                }
                else {
                    String unitStr = "KB/s";

                    readPerformance /= 100;
                    if (readPerformance >= 1024) {
                        readPerformance /= 1024;
                        unitStr = "MB/s";
                    }
                    textReadSpeed.setText(String.format("Read performance: %.2f %s", readPerformance, unitStr));
                }
            }
        });

        setTestSizeTextView((TextView)root.findViewById(R.id.sizeRatioText));

        sizeRatioSeekBar.setProgress(testViewModel.getTestSizeRatio().getValue());
        sizeRatioSeekBar.setOnSeekBarChangeListener(new onSizeRatioSeekBarChangeListener());

        // Create size selection items
        List<String> sizeStrings = new ArrayList<>();
        List<String> unitStrings = new ArrayList<>();

        for (int i = 1; i < 1024; i*=2) {
            sizeStrings.add(Integer.toString(i));
        }
        unitStrings.add("KB");
        unitStrings.add("MB");

        ArrayAdapter<String> sizeApapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                sizeStrings);
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                unitStrings);

        Spinner sizeSpinner = (Spinner)root.findViewById(R.id.sizeSpinner);
        Spinner unitSpinner = (Spinner)root.findViewById(R.id.unitSpinner);
        sizeSpinner.setAdapter(sizeApapter);
        unitSpinner.setAdapter(unitAdapter);

        sizeSpinner.setSelection(testViewModel.getSizeIdx());
        unitSpinner.setSelection(testViewModel.getUnitIdx());

        // Set item select listener
        Spinner.OnItemSelectedListener sizeSelListener = new onSizeUnitSelectedListener();
        sizeSpinner.setOnItemSelectedListener(sizeSelListener);
        unitSpinner.setOnItemSelectedListener(sizeSelListener);

        goBtn.setOnClickListener(new onGoBtnClickListener());

        msgHandler = new messageHandler();

        return root;
    }
    private long getRootTotalSizeKB()
    {
        MainActivity main = (MainActivity) getActivity();

        String rootPath = main.getRootDir();
        String writePath = main.getTargetDir();
        StatFs stat1 = new StatFs(rootPath);
        long totalBytes = (long) stat1.getBlockSize() * (long) stat1.getFreeBlocks();
        long totalKB = totalBytes / 1024;

        if (totalKB > 1024)
            totalKB -= 1024;

        return totalKB;
    }
    private void setTestSizeTextView(TextView tv)
    {
        long size = (long)(getRootTotalSizeKB() * ((double)testViewModel.getTestSizeRatio().getValue() / 100.0));
        String unit = "KB";

        if (size >= 1024) {
            size = size >> 10;
            unit = "MB";
        }
        tv.setText(String.format("Test size ratio: %d%% (%d %s)", testViewModel.getTestSizeRatio().getValue(), size, unit));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ///msgHandler
    }

    public class onSizeUnitSelectedListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Spinner sizeSpinner = (Spinner)getActivity().findViewById(R.id.sizeSpinner);
            Spinner unitSpinner = (Spinner)getActivity().findViewById(R.id.unitSpinner);

            String size = (String)sizeSpinner.getSelectedItem();
            String unit = (String)unitSpinner.getSelectedItem();

            testViewModel.setSizeIdx(sizeSpinner.getSelectedItemPosition());
            testViewModel.setUnitIdx(unitSpinner.getSelectedItemPosition());
            ////testViewModel.setTextMsg("You selected: " + size + " " + unit);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }
    public class onSizeRatioSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            testViewModel.setTestSizeRatioe(progress);
            setTestSizeTextView((TextView)getActivity().findViewById(R.id.sizeRatioText));
        }
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    }
    public class onGoBtnClickListener implements Button.OnClickListener {
        private Thread runThread = null;
        @Override
        public void onClick(View v) {
            if (!testViewModel.getRunning()) {
                createRun();
            }
            else {
                testViewModel.stopThread();
            }
        }
        private void createRun()
        {
            try {
                MainActivity main = (MainActivity) getActivity();
                assert main != null;
                String writePath = main.getTargetDir();
                long totalKB = getRootTotalSizeKB();

                addLog("Test with file size " + getTestFileSizeKB() + " KB");

                testViewModel.setGoButtonState("STOP", true);
                testViewModel.startThread(msgHandler, writePath, (int)totalKB, getTestFileSizeKB(), testViewModel.getTestSizeRatio().getValue());
            }
            catch (Exception e) {
                addLog("[Error] " + e.getMessage());
            }
        }
    }
    private int getTestFileSizeKB()
    {
        Spinner sizeSpinner = (Spinner)getActivity().findViewById(R.id.sizeSpinner);
        Spinner unitSpinner = (Spinner)getActivity().findViewById(R.id.unitSpinner);

        int size = Integer.parseInt((String)sizeSpinner.getSelectedItem());
        int unitShift = unitSpinner.getSelectedItemId() == 0 ? (0) : (10);

        return size << unitShift;
    }


    public enum messageId {
        add_log,
        set_progress_max,
        set_progress,
        set_text_msg,
        set_write_speed,
        set_read_speed,
        almost_done;

        public int value = ordinal() + 1;
    }

    // Handle messages received from thread
    public class messageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what==300){
                if (msg.arg1 == messageId.add_log.value) {
                    addLog((String) msg.obj);
                }
                else if (msg.arg1 == messageId.set_text_msg.value) {
                    testViewModel.setTextMsg((String)msg.obj);
                }
                else if (msg.arg1 == messageId.set_progress_max.value) {
                    testViewModel.setProgressMax(msg.arg2);
                }
                else if (msg.arg1 == messageId.set_progress.value) {
                    testViewModel.setProgress(msg.arg2);
                }
                else if (msg.arg1 == messageId.set_write_speed.value) {
                    testViewModel.setWritePerformance(msg.arg2 / 100);
                }
                else if (msg.arg1 == messageId.set_read_speed.value) {
                    testViewModel.setReadPerformance(msg.arg2 / 100);
                }
                else if (msg.arg1 == messageId.almost_done.value) {
                    testViewModel.setGoButtonState("GO", true);
                    assert testViewModel.getProgressMax().getValue() != null;
                    testViewModel.setProgress(testViewModel.getProgressMax().getValue());
                }
                else {
                    ;
                }
            }
            super.handleMessage(msg);
        }
    }

}
