package com.example.myapplication.ui.test;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TestViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    private int mSizeIdx;
    private int mUnitIdx;

    MutableLiveData<Integer> progress;
    MutableLiveData<Integer> progressMax;
    MutableLiveData<String> textMsg;

    public TestViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is dashboard fragment");
        progress = new MutableLiveData<>();
        progressMax = new MutableLiveData<>();
        textMsg = new MutableLiveData<>();
    }
    public void setText(String text)
    {
        mText.setValue(text);
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

    public int getSizeIdx() {
        return mSizeIdx;
    }
    public int getUnitIdx() {
        return mUnitIdx;
    }

    public LiveData<String> getText() {
        return mText;
    }
    public LiveData<Integer> getProgress() {return progress;}
    public LiveData<Integer> getProgressMax() {return progressMax;}
    public LiveData<String> getTextMsg() {return textMsg;}
}