package com.example.sdtesttool.ui.test;

import android.Manifest;
import android.os.Build;
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
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.sdtesttool.LogOwner;
import com.example.sdtesttool.MainActivity;
import com.example.sdtesttool.R;
import com.example.sdtesttool.ui.ButtonState;
import com.example.sdtesttool.ui.IThreadListener;
import com.example.sdtesttool.ui.RWTestThread;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestFragment extends LogOwner
{
    private TestViewModel testViewModel;
    private Handler msgHandler;
    private View root = null;
    private String goButtonCaption = "TEST";
    private String verifyButtonCaption = "Read to verify";
    private String stopButtonCaption = "STOP";
    private static long currRootTotalSizeKB = 0;

    private TestThreadListener testThreadListener = new TestThreadListener();

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

        root = inflater.inflate(R.layout.fragment_test, container, false);

        final Switch deleteFilesSwitch = (Switch)root.findViewById(R.id.switchDelFiles);
        final ProgressBar progressBar = (ProgressBar)root.findViewById(R.id.progressBar);
        final TextView textMsg = (TextView)root.findViewById(R.id.textMsg);
        final Button goBtn = (Button)root.findViewById(R.id.goButton);
        final Button verifyBtn = (Button)root.findViewById(R.id.verifyBtn);
        final Button clearBtn = (Button)root.findViewById(R.id.clearFilesBtn);
        final TextView textWriteSpeed = (TextView)root.findViewById(R.id.textWriteSpeed);
        final TextView textReadSpeed = (TextView)root.findViewById(R.id.textReadSpeed);
        final TextView sizeRatioText = (TextView)root.findViewById(R.id.sizeRatioText);
        final SeekBar sizeRatioSeekBar = (SeekBar)root.findViewById(R.id.sizeRatioSeekBar);

        MainActivity main = (MainActivity)getActivity();
        testViewModel.setRootDir(main.getRootDir());
        testViewModel.setTargetDir(main.getTargetDir());

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

        Observer<ButtonState> buttonStateObserver = new Observer<ButtonState>() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void onChanged(ButtonState buttonState) {
                Button btn = null;
                if (buttonState.tag == 0) btn = goBtn;
                if (buttonState.tag == 1) btn = verifyBtn;
                if (buttonState.tag == 2) btn = clearBtn;

                btn.setText(buttonState.caption);
                btn.setEnabled(buttonState.enabled);

                if (buttonState.tag != 2) {
                    if (buttonState.enabled) {
                        btn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                    } else {
                        btn.setBackgroundColor(getResources().getColor(R.color.colorDisabled));
                    }
                    if (!buttonState.running) {
                        btn.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(android.R.drawable.ic_media_play), null, null, null);
                    } else {
                        btn.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(android.R.drawable.ic_media_pause), null, null, null);
                    }
                }
            }
        };

        testViewModel.getGoButtonState().observe(getViewLifecycleOwner(), buttonStateObserver);
        testViewModel.getVerifyButtonState().observe(getViewLifecycleOwner(), buttonStateObserver);
        testViewModel.getClearFilesButtonState().observe(getViewLifecycleOwner(), buttonStateObserver);

        testViewModel.getWritePerformance().observe(getViewLifecycleOwner(), new Observer<Double>() {
            @Override
            public void onChanged(Double writePerformance) {
                if (writePerformance == 0) {
                    textWriteSpeed.setText("Write performance: --");
                }
                else {
                    String unitStr = "KB/s";

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

                    if (readPerformance >= 1024) {
                        readPerformance /= 1024;
                        unitStr = "MB/s";
                    }
                    textReadSpeed.setText(String.format("Read performance: %.2f %s", readPerformance, unitStr));
                }
            }
        });

        if (testViewModel.isFirstCreate()) {
            try {
                loadSettingFromCache();
                currRootTotalSizeKB = getRootTotalSizeKB();
                ////testViewModel.setGoButtonState(goButtonCaption, true);
            }
            catch (Exception e){}
        }

        updateTestSizeTextView((TextView)root.findViewById(R.id.sizeRatioText));

        sizeRatioSeekBar.setProgress(testViewModel.getTestSizeRatio().getValue());
        sizeRatioSeekBar.setOnSeekBarChangeListener(new onSizeRatioSeekBarChangeListener());

        deleteFilesSwitch.setChecked(testViewModel.getDeleteFiles().getValue());
        deleteFilesSwitch.setOnCheckedChangeListener(new onDeleteFileCheckedChangeListener());

        // Create size selection items
        List<String> sizeStrings = new ArrayList<>();
        List<String> unitStrings = new ArrayList<>();
        List<String> verifyTypeStrings = new ArrayList<>();

        for (int i = 1; i < 1024; i*=2) {
            sizeStrings.add(Integer.toString(i));
        }
        unitStrings.add("KB");
        unitStrings.add("MB");

        verifyTypeStrings.add("No Verify");
        verifyTypeStrings.add("Read immediately");
        verifyTypeStrings.add("Read after all write");

        Spinner sizeSpinner = (Spinner)root.findViewById(R.id.sizeSpinner);
        Spinner unitSpinner = (Spinner)root.findViewById(R.id.unitSpinner);
        Spinner verifyTypeSpinner = (Spinner)root.findViewById(R.id.verifyTypeSpinner);

        adpatSpinnerStrings(sizeSpinner, sizeStrings);
        adpatSpinnerStrings(unitSpinner, unitStrings);
        adpatSpinnerStrings(verifyTypeSpinner, verifyTypeStrings);

        sizeSpinner.setSelection(testViewModel.getSizeIdx());
        unitSpinner.setSelection(testViewModel.getUnitIdx());
        verifyTypeSpinner.setSelection(testViewModel.getVerifyTypeIdx());

        // Set item select listener
        Spinner.OnItemSelectedListener sizeSelListener = new onSizeUnitSelectedListener();
        sizeSpinner.setOnItemSelectedListener(sizeSelListener);
        unitSpinner.setOnItemSelectedListener(sizeSelListener);
        verifyTypeSpinner.setOnItemSelectedListener(sizeSelListener);

        goBtn.setOnClickListener(new onGoBtnClickListener());
        verifyBtn.setOnClickListener(new onVerifyBtnListener());
        clearBtn.setOnClickListener(new onClearTestFilesBtnListener());

        msgHandler = new messageHandler();
        testViewModel.updateFirstCreate();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //saveSettingToCache();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    @Override
    public void onDetach()
    {
        super.onDetach();
    }

    private void loadSettingFromCache() throws IOException {
        File cacheDir = getContext().getCacheDir();
        File setting = new File(getContext().getCacheDir(), "setting.bin");

        if (setting.exists()) {
            byte[] buff = new byte[5];

            FileInputStream fis = new FileInputStream(setting);
            try {
                fis.read(buff);

                testViewModel.setSizeIdx(buff[0]);
                testViewModel.setUnitIdx(buff[1]);
                testViewModel.setVerifyTypeIdx(buff[2]);
                testViewModel.setTestSizeRatioe((int)buff[3]);
                testViewModel.setDeleteFiles(buff[4] == 0 ? false : true);
            }
            catch (Exception e) {
                ;
            }
            finally {
                fis.close();
            }
        }
    }

    private void saveSettingToCache() throws IOException {
        File setting = new File(getContext().getCacheDir(), "setting.bin");
        FileOutputStream fos = new FileOutputStream(setting);
        try {
            if (!setting.exists()) {
                setting.createNewFile();
            }
            byte[] buff = new byte[5];

            buff[0] = (byte)testViewModel.getSizeIdx();
            buff[1] = (byte)testViewModel.getUnitIdx();
            buff[2] = (byte)testViewModel.getVerifyTypeIdx();
            buff[3] = (byte)testViewModel.getTestSizeRatio().getValue().byteValue();
            buff[4] = (byte)(testViewModel.getDeleteFiles().getValue() ? 1 : 0);

            fos.write(buff);
            fos.flush();
        }
        catch (Exception e) {
            ;
        }
        finally {
            fos.close();
        }
    }

    private void adpatSpinnerStrings(AdapterView view, List<String> strings)
    {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                strings);

        view.setAdapter(adapter);
    }

    private long getRootTotalSizeKB()
    {
        String rootPath = testViewModel.getRootDir();
        String writePath = testViewModel.getTargetDir();
        StatFs stat1 = new StatFs(rootPath);
        long totalBytes = (long) stat1.getBlockSize() * (long) stat1.getFreeBlocks();
        long totalKB = totalBytes / 1024;

        if (totalKB > 1024)
            totalKB -= 1024;

        return totalKB;
    }
    private void updateTestSizeTextView(TextView tv)
    {
        long size = (long)(currRootTotalSizeKB * ((double)testViewModel.getTestSizeRatio().getValue() / 100.0));
        String unit = "KB";

        if (size >= 1024) {
            size = size >> 10;
            unit = "MB";
        }
        tv.setText(String.format("Test size ratio: %d%% (%d %s)", testViewModel.getTestSizeRatio().getValue(), size, unit));
    }

    /*
     *  Callback (listener) functions
     */

    public class onSizeUnitSelectedListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Spinner sizeSpinner = (Spinner)getActivity().findViewById(R.id.sizeSpinner);
            Spinner unitSpinner = (Spinner)getActivity().findViewById(R.id.unitSpinner);
            Spinner verifyTypeSpinner = (Spinner)getActivity().findViewById(R.id.verifyTypeSpinner);

            testViewModel.setSizeIdx(sizeSpinner.getSelectedItemPosition());
            testViewModel.setUnitIdx(unitSpinner.getSelectedItemPosition());
            testViewModel.setVerifyTypeIdx(verifyTypeSpinner.getSelectedItemPosition());

            try {
                saveSettingToCache();
            }
            catch (Exception e) { }
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
            updateTestSizeTextView((TextView)getActivity().findViewById(R.id.sizeRatioText));
            try {
                saveSettingToCache();
            }
            catch (Exception e) { }
        }
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    }
    public class onDeleteFileCheckedChangeListener implements RadioButton.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            testViewModel.setDeleteFiles(isChecked);
            try {
                saveSettingToCache();
            }
            catch (Exception e) { }
        }
    }

    public interface IThreadCreator {
        public void createRun();
    }

    public class onGoBtnClickListener implements Button.OnClickListener, IThreadCreator
    {
        @Override
        public void onClick(View v) {
            if (!testViewModel.getRunning()) {
                createRun();
            }
            else {
                testViewModel.stopThread();
            }
        }

        @Override
        public void createRun()
        {
            try {
                addLog("Test with file size " + getTestFileSizeKB() + " KB");

                testViewModel.setGoButtonState(stopButtonCaption, true, true);
                testViewModel.setVerifyButtonState(verifyButtonCaption, false, false);
                testViewModel.startThread(testThreadListener , msgHandler, createTestArgs(getVerifyType()));
            }
            catch (Exception e) {
                addLog("[Error] " + e.getMessage());
            }
        }
    }
    public class onVerifyBtnListener extends onGoBtnClickListener {
        private Thread runThread = null;

        public void createRun()
        {
            try {
                addLog("Read to verify with file size " + getTestFileSizeKB() + " KB");

                testViewModel.setGoButtonState(goButtonCaption, false, false);
                testViewModel.setVerifyButtonState(stopButtonCaption, true, true);
                testViewModel.startThread(testThreadListener, msgHandler, createTestArgs(RWTestThread.VerifyType.verify_only));
            }
            catch (Exception e) {
                addLog("[Error] " + e.getMessage());
            }
        }
    }
    public class onClearTestFilesBtnListener extends onGoBtnClickListener {
        private Thread runThread = null;

        public void createRun()
        {
            try {
                addLog("Read to verify with file size " + getTestFileSizeKB() + " KB");

                testViewModel.setGoButtonState(goButtonCaption, false, false);
                testViewModel.setVerifyButtonState(verifyButtonCaption, false, false);
                testViewModel.startThread(testThreadListener, msgHandler, createTestArgs(RWTestThread.VerifyType.clear_files));
            }
            catch (Exception e) {
                addLog("[Error] " + e.getMessage());
            }
        }
    }
    private RWTestThread.TestArgs createTestArgs(final RWTestThread.VerifyType verifyTypeInput)
    {
        final MainActivity main = (MainActivity) getActivity();
        assert main != null;

        return new RWTestThread.TestArgs() {
            {
                verifyType = verifyTypeInput;
                rootSizeKB = (int)getRootTotalSizeKB();
                rootDir = main.getTargetDir();
                fileSizeKB = getTestFileSizeKB();
                testSizeRatio = testViewModel.getTestSizeRatio().getValue();
                deleteFiles = testViewModel.getDeleteFiles().getValue();

                if (verifyType == RWTestThread.VerifyType.clear_files)
                    deleteFiles = true;
            };
        };
    }

    private RWTestThread.VerifyType getVerifyType()
    {
        if (testViewModel.getVerifyTypeIdx() == 0) return RWTestThread.VerifyType.none;
        else if (testViewModel.getVerifyTypeIdx() == 1) return RWTestThread.VerifyType.immediately;
        else return RWTestThread.VerifyType.after_all;
    }


    private int getTestFileSizeKB()
    {
        Spinner sizeSpinner = (Spinner)getActivity().findViewById(R.id.sizeSpinner);
        Spinner unitSpinner = (Spinner)getActivity().findViewById(R.id.unitSpinner);

        int size = Integer.parseInt((String)sizeSpinner.getSelectedItem());
        int unitShift = unitSpinner.getSelectedItemId() == 0 ? (0) : (10);

        return size << unitShift;
    }

    private void onAlmostDone()
    {
        testViewModel.setGoButtonState(goButtonCaption, true, false);
        testViewModel.setVerifyButtonState(verifyButtonCaption, true, false);
        assert testViewModel.getProgressMax().getValue() != null;
        currRootTotalSizeKB = getRootTotalSizeKB();
        updateTestSizeTextView((TextView)root.findViewById(R.id.sizeRatioText));
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

    public class TestThreadListener implements  IThreadListener {

        @Override
        public void onAlmostDone(int Tag) {

        }
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
                    onAlmostDone();
                }
                else {
                    ;
                }
            }
            super.handleMessage(msg);
        }
    }

}
