package com.example.myapplication.ui.test;

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
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.myapplication.LogOwner;
import com.example.myapplication.MainActivity;
import com.example.myapplication.R;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestFragment extends LogOwner {

    private TestViewModel testViewModel;
    public Handler msgHandler;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        testViewModel = ViewModelProviders.of(getActivity()).get(TestViewModel.class);

        View root = inflater.inflate(R.layout.fragment_test, container, false);

        final TextView textView = root.findViewById(R.id.text_test);

        testViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });

        final ProgressBar progressBar = (ProgressBar)root.findViewById(R.id.progressBar);
        final TextView textMsg = (TextView)root.findViewById(R.id.textMsg);

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

        // Create size selection items
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

        Button.OnClickListener goBtnClick = new onGoBtnClickListener();
        final Button goBtn = (Button)root.findViewById(R.id.doTestBtn);
        goBtn.setOnClickListener(goBtnClick);




        msgHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if(msg.what==300){
                    ////images.get(msg.arg1).setImageBitmap((Bitmap) msg.obj);
                    switch (msg.arg1) {
                        case (0):
                            addLog((String) msg.obj);
                            break;
                        case (2): // set progress max
                            ////progressBar.setMax(msg.arg2);
                            testViewModel.setProgressMax(msg.arg2);
                            break;
                        case (3): // set progress
                            ////progressBar.setProgress(msg.arg2);
                            testViewModel.setProgress(msg.arg2);
                            break;
                        case (4): // set text message
                            ////textMsg.setText((String)msg.obj);
                            testViewModel.setTextMsg((String)msg.obj);
                            break;
                        case (9): // almost done
                            goBtn.setEnabled(true);
                            goBtn.setText("GO");
                            testViewModel.setProgress(progressBar.getMax());
                            break;
                        default:
                            break;
                    }
                }
                super.handleMessage(msg);
            }
        };

        return root;
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
            testViewModel.setText("You selected: " + size + " " + unit);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }
    public int getTestFileSizeKB()
    {
        Spinner sizeSpinner = (Spinner)getActivity().findViewById(R.id.sizeSpinner);
        Spinner unitSpinner = (Spinner)getActivity().findViewById(R.id.unitSpinner);

        int size = Integer.parseInt((String)sizeSpinner.getSelectedItem());
        int unitShift = unitSpinner.getSelectedItemId() == 0 ? (0) : (10);

        return size << unitShift;
    }

    public class onGoBtnClickListener implements Button.OnClickListener {
        @Override
        public void onClick(View v) {
            try {
                MainActivity main = (MainActivity) getActivity();
                String rootPath = main.getRootDir();
                String writePath = main.getTargetDir();
                StatFs stat1 = new StatFs(rootPath);
                long totalBytes = (long) stat1.getBlockSize() * (long) stat1.getFreeBlocks();
                long totalKB = totalBytes / 1024;

                addLog("Size to test: " + totalKB + " KB");

                File rootDir = new File(writePath);

                boolean success = rootDir.setExecutable(true);

                if (!success)
                    throw new Exception("Can't set test root directory writable");

                File testRootDir = new File(writePath, "000");

                if (!testRootDir.exists()) {
                    testRootDir.mkdirs();

                    if (!testRootDir.exists()) {
                        addLog("Can't mkdir test directory");
                    }
                }
                addLog("Test with file size " + getTestFileSizeKB() + " KB");

                Button goBtn = (Button)v.findViewById(R.id.doTestBtn);
                goBtn.setText("Testing..");
                goBtn.setEnabled(false);

                WriteDirectoryThread th = new WriteDirectoryThread(writePath + "/000", (int)totalKB, getTestFileSizeKB());
                th.start();
            }
            catch (Exception e) {
                addLog("[Error] " + e.getMessage());
            }
        }
    }


    public class WriteDirectoryThread extends Thread {
        public String rootDir;
        public int totalSizeKB;
        public int fileSizeKB;

        WriteDirectoryThread(String rootDir, int totalSizeKB, int fileSizeKB) {
            this.rootDir = rootDir;
            this.totalSizeKB = totalSizeKB;
            this.fileSizeKB = fileSizeKB;
        }

        private void addLog(String s)
        {
            sendMessage(0, 0, s);
        }
        private void showText(String s)
        {
            sendMessage(4, 0, s);
        }
        private void almostDone()
        {
            sendMessage(9, 0, null);
        }
        private void setProgressMax(int progressMax)
        {
            sendMessage(2, progressMax, null);
        }
        private  void setProgress(int progress)
        {
            sendMessage(3, progress, null);
        }

        private void sendMessage(int arg1, int arg2, Object obj)
        {
            Message msg = msgHandler.obtainMessage();
            msg.what = 300;
            msg.arg1 = arg1;
            msg.arg2 = arg2;
            msg.obj = obj;
            msgHandler.sendMessage(msg);
        }

        @Override
        public void run() {
            try {
                String fName;

                Random rand = new Random(0x12345678);

                setProgressMax(50);

                addLog("Star to write files on directory " + rootDir);

                for (int idx = 0; idx < 50; ++idx) {
                    fName = String.format("%03d.bin", idx);
                    showText("Try to create file " + fName);

                    File newFile = new File(rootDir, fName);
                    newFile.createNewFile();

                    if (!newFile.exists()) {
                        throw new Exception("File not exists!");
                    }
                    showText("Try to write file " + fName);
                    addLog(fName);

                    FileOutputStream fs = new FileOutputStream(newFile);

                    byte[] buf = new byte[4096 * 1024];
                    rand.nextBytes(buf);

                    buf[0] = (byte) 0x55;
                    buf[1] = (byte) 0xaa;
                    buf[buf.length - 2] = (byte) 0xaa;
                    buf[buf.length - 1] = (byte) 0x55;

                    fs.write(buf);
                    fs.close();
                    setProgress(idx);
                }
                showText("Done");
                addLog("Done");
            }
            catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
            almostDone();
        }
    }

}
