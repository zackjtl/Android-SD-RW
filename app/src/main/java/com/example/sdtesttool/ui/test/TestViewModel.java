package com.example.sdtesttool.ui.test;

import android.content.Context;
import android.os.Handler;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sdtesttool.ui.ButtonState;
import com.example.sdtesttool.ui.IThreadListener;
import com.example.sdtesttool.ui.RWTestThread;
import com.example.sdtesttool.ui.TestThreadBase;

public class TestViewModel extends ViewModel {

    private int mSizeIdx;
    private int mUnitIdx;
    private int verifyTypeIdx;

    private boolean running;
    private boolean firstCreate;

    MutableLiveData<Boolean> deleteFiles;
    MutableLiveData<Integer> progress;
    MutableLiveData<Integer> progressMax;
    MutableLiveData<String> textMsg;
    MutableLiveData<ButtonState> goButtonState;
    MutableLiveData<ButtonState> verifyButtonState;
    MutableLiveData<ButtonState> clearFilesButtonState;

    MutableLiveData<Double> writePerformance;
    MutableLiveData<Double> readPerformance;

    MutableLiveData<Integer> testSizeRatio;

    private String rootDir;
    private String targetDir;

    private TestThreadBase runThraed;

    public TestViewModel() {
        deleteFiles = new MutableLiveData<>();
        progress = new MutableLiveData<>();
        progressMax = new MutableLiveData<>();
        textMsg = new MutableLiveData<>();
        goButtonState = new MutableLiveData<>();
        verifyButtonState = new MutableLiveData<>();
        clearFilesButtonState = new MutableLiveData<>();
        writePerformance = new MutableLiveData<>();
        readPerformance = new MutableLiveData<>();
        testSizeRatio = new MutableLiveData<>();

        deleteFiles.setValue(false);
        testSizeRatio.setValue(100);
        mUnitIdx = 0;
        mSizeIdx = 2;
        verifyTypeIdx = 2;

        running = false;
        firstCreate = true;
    }
    Thread obtainThread() {return runThraed;}

    void startThread(IThreadListener listener, Handler msgHandler, RWTestThread.TestArgs testArgs) {
        if (runThraed != null)
            runThraed.interrupt();
        runThraed = new RWTestThread(msgHandler, testArgs);
        runThraed.setListener(listener);
        runThraed.start();
        running = true;
    }

    void stopThread() {
        if (runThraed != null) {
            ((RWTestThread)runThraed).terminate();
            running = false;
        }
    }

    public void updateFirstCreate()
    {
        firstCreate = false;
    }
    public boolean isFirstCreate()
    {
        return firstCreate;
    }

    public void setDeleteFiles(boolean value) {this.deleteFiles.setValue(value);}
    public void setSizeIdx(int mSizeIdx) {
        this.mSizeIdx = mSizeIdx;
    }

    public void setUnitIdx(int mUnitIdx) {
        this.mUnitIdx = mUnitIdx;
    }
    public void setVerifyTypeIdx(int value)
    {
        this.verifyTypeIdx = value;
    }

    public void setProgressMax(int progressMax) {this.progressMax.setValue(progressMax);}
    public void setProgress(int progress) {this.progress.setValue(progress);}
    public void setTextMsg(String textMsg) {this.textMsg.setValue(textMsg);}
    public void setGoButtonState(String caption, boolean enabled, boolean running)
    {
        try {
            ButtonState btnState = new ButtonState();
            btnState.tag = 0;
            btnState.caption = caption;
            btnState.enabled = enabled;
            btnState.running = running;
            goButtonState.setValue(btnState);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void setVerifyButtonState(String caption, boolean enabled, boolean running)
    {
        ButtonState btnState = new ButtonState();
        btnState.tag = 1;
        btnState.caption = caption;
        btnState.enabled = enabled;
        btnState.running = running;
        verifyButtonState.setValue(btnState);
    }
    public void setClearFilesButtonState(String caption, boolean enabled, boolean running)
    {
        ButtonState btnState = new ButtonState();
        btnState.tag = 2;
        btnState.caption = caption;
        btnState.enabled = enabled;
        btnState.running = running;
        clearFilesButtonState.setValue(btnState);
    }
    public void setWritePerformance(double value) {writePerformance.setValue(value);}
    public void setReadPerformance(double value) {readPerformance.setValue(value);}
    public void setTestSizeRatioe(Integer value) {testSizeRatio.setValue(value);}
    public void setRunning(boolean value) {running = value;}

    public void setRootDir(String value) {rootDir = value;}
    public String getRootDir() {return rootDir;}
    public void setTargetDir(String value) {targetDir = value;}
    public String getTargetDir() {return targetDir;}

    public int getSizeIdx() {
        return mSizeIdx;
    }
    public int getUnitIdx() {
        return mUnitIdx;
    }
    public int getVerifyTypeIdx() {
        return verifyTypeIdx;
    }

    public LiveData<Integer> getProgress() {return progress;}
    public LiveData<Integer> getProgressMax() {return progressMax;}
    public LiveData<Boolean> getDeleteFiles() {return deleteFiles;}
    public LiveData<String> getTextMsg() {return textMsg;}
    public LiveData<ButtonState> getGoButtonState() {return goButtonState;}
    public LiveData<ButtonState> getVerifyButtonState() {return verifyButtonState;}
    public LiveData<ButtonState> getClearFilesButtonState() {return clearFilesButtonState;}
    public LiveData<Double> getWritePerformance() {return writePerformance;}
    public LiveData<Double> getReadPerformance() {return readPerformance;}
    public LiveData<Integer> getTestSizeRatio() {return testSizeRatio;}
    public boolean getRunning() {return running;}
}