package com.example.myapplication;

import android.content.Context;

import androidx.fragment.app.Fragment;

public class LogOwner extends Fragment {
    public ILogListener mLogListener;

    public void addLog(String text)
    {
        if (mLogListener != null)
            mLogListener.onLog(text);
    }
    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        MainActivity main = (MainActivity) getActivity();
        setLogListener(main);
    }
    public void setLogListener(ILogListener LogListener)
    {
        mLogListener = LogListener;
    }
}
