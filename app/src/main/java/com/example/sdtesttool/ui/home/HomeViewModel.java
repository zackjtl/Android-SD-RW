package com.example.sdtesttool.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class HomeViewModel extends ViewModel {

    private MutableLiveData<String> mText;
    private MutableLiveData<List<String>> mRootList;
    private MutableLiveData<List<String>> mRootDispList;

    public HomeViewModel() {
        InitialMutableLiveData();
    }
    public void InitialMutableLiveData()
    {
        mText = new MutableLiveData<>();
        ////mText.setValue("This is home fragment");
        mRootList = new MutableLiveData<>();
        mRootDispList = new MutableLiveData<>();
    }

    public void setRootList(List<String> Value)
    {
       mRootList.setValue(Value);
    }

    public void setRootDispList(List<String> Value)
    {
        mRootDispList.setValue(Value);
    }

    public LiveData<String> getText() {
        return mText;
    }
    public LiveData<List<String>> getRootList() {return mRootList;}
    public LiveData<List<String>> getRootDispList() {return mRootDispList;}

    public String getRootListItem(int id) {
        return mRootList.getValue().get(id);
    }
    ////public List<String> getRootListValue() {return mRootList.getValue();}
}