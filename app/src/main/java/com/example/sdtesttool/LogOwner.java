package com.example.sdtesttool;

import android.content.Context;

import androidx.fragment.app.Fragment;

public class LogOwner extends Fragment {
    public ILogListener logListener;

    public void addLog(String text)
    {
        if (logListener != null)
            logListener.onLog(text);
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
        logListener = LogListener;
    }
}
