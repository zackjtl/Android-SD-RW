package com.example.myapplication.ui.test;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TestViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public TestViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is dashboard fragment");
    }
    public void setText(String text)
    {
        mText.setValue(text);
    }

    public LiveData<String> getText() {
        return mText;
    }
}