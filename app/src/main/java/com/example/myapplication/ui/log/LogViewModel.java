package com.example.myapplication.ui.log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class LogViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    private MutableLiveData<List<String>> mLogList;

    public LogViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is notifications fragment");

        mLogList = new MutableLiveData<>();
    }

    public LiveData<String> getText() {
        return mText;
    }

    public void setLogList(List<String> Value)
    {
        mLogList.setValue(Value);
    }
}