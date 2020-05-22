package com.example.sdtesttool.ui.test;

import android.os.Handler;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sdtesttool.ui.ButtonState;
import com.example.sdtesttool.ui.WriteDirectoryThread;

public class TestViewModel extends ViewModel {

    private int mSizeIdx;
    private int mUnitIdx;

    private boolean running;

    MutableLiveData<Integer> progress;
    MutableLiveData<Integer> progressMax;
    MutableLiveData<String> textMsg;
    MutableLiveData<ButtonState> goButtonState;

    MutableLiveData<Double> writePerformance;
    MutableLiveData<Double> readPerformance;

    MutableLiveData<Integer> testSizeRatio;

    private Thread runThraed;

    public TestViewModel() {
        progress = new MutableLiveData<>();
        progressMax = new MutableLiveData<>();
        textMsg = new MutableLiveData<>();
        goButtonState = new MutableLiveData<>();
        writePerformance = new MutableLiveData<>();
        readPerformance = new MutableLiveData<>();
        testSizeRatio = new MutableLiveData<>();

        testSizeRatio.setValue(100);
        mSizeIdx = 3;
        running = false;
    }
    Thread obtainThread() {return runThraed;}

    void startThread(Handler msgHandler, String rootDir, int rootSizeKB, int fileSizeKB, int testSizeRatio) {
        if (runThraed != null)
            runThraed.interrupt();
        runThraed = new WriteDirectoryThread(msgHandler, WriteDirectoryThread.PatternVerifyType.immediately, rootDir, rootSizeKB, fileSizeKB, testSizeRatio);
        runThraed.start();
        running = true;
    }

    void stopThread() {
        if (runThraed != null) {
            ((WriteDirectoryThread)runThraed).terminate();
            running = false;
        }
    }

    public void setSizeIdx(int mSizeIdx) {
        this.mSizeIdx = mSizeIdx;
    }

    public void setUnitIdx(int mUnitIdx) {
        this.mUnitIdx = mUnitIdx;
    }

    public void setProgressMax(int progressMax) {this.progressMax.setValue(progressMax);}
    public void setProgress(int progress) {this.progress.setValue(progress);}
    public void setTextMsg(String textMsg) {this.textMsg.setValue(textMsg);}
    public void setGoButtonState(String caption, boolean enabled)
    {
        ButtonState btnState = new ButtonState();
        btnState.caption = caption;
        btnState.enabled = enabled;
        goButtonState.setValue(btnState);
    }
    public void setWritePerformance(double value) {writePerformance.setValue(value);}
    public void setReadPerformance(double value) {readPerformance.setValue(value);}
    public void setTestSizeRatioe(Integer value) {testSizeRatio.setValue(value);}
    public void setRunning(boolean value) {running = value;}

    public int getSizeIdx() {
        return mSizeIdx;
    }
    public int getUnitIdx() {
        return mUnitIdx;
    }

    public LiveData<Integer> getProgress() {return progress;}
    public LiveData<Integer> getProgressMax() {return progressMax;}
    public LiveData<String> getTextMsg() {return textMsg;}
    public LiveData<ButtonState> getGoButtonState() {return goButtonState;}
    public LiveData<Double> getWritePerformance() {return writePerformance;}
    public LiveData<Double> getReadPerformance() {return readPerformance;}
    public LiveData<Integer> getTestSizeRatio() {return testSizeRatio;}
    public boolean getRunning() {return running;}
}