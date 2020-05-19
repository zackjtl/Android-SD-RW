package com.example.myapplication.ui.home;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel {

    private MutableLiveData<String> mText;
    private MutableLiveData<List<String>> mRootList;

    public HomeViewModel() {
        InitialMutableLiveData();
    }
    public void InitialMutableLiveData()
    {
        mText = new MutableLiveData<>();
        ////mText.setValue("This is home fragment");
        mRootList = new MutableLiveData<>();
    }

    public void setRootList(List<String> Value)
    {
       mRootList.setValue(Value);
    }

    public LiveData<String> getText() {
        return mText;
    }
    public LiveData<List<String>> getRootList() {return mRootList;}

    public String getRootListItem(int id) {
        return mRootList.getValue().get(id);
    }
    ////public List<String> getRootListValue() {return mRootList.getValue();}
}