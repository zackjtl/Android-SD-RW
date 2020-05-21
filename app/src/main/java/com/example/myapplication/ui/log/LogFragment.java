package com.example.myapplication.ui.log;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;

public class LogFragment extends Fragment {

    private LogViewModel logViewModel;

    View rootView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        logViewModel = ViewModelProviders.of(getActivity()).get(LogViewModel.class);
        rootView = inflater.inflate(R.layout.fragment_log, container, false);

        final TextView tv = rootView.findViewById(R.id.logText);
        tv.setMovementMethod(new ScrollingMovementMethod());

        MainActivity main = (MainActivity)getActivity();

        main.getLogData().observe(getActivity(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                tv.setText(s);
            }
        });


        //UpdateLogFromMain();

        return rootView;
    }

    public void UpdateLogFromMain()
    {
        //EditText edit = getActivity().findViewById(R.id.logEdit);
        //edit.setText(((MainActivity)getActivity()).getLogText());

        /*
        ArrayAdapter adapter = new ArrayAdapter(getContext(),
                android.R.layout.simple_list_item_1,
                ((MainActivity)getActivity()).getLogList());

        ListView lv = rootView.findViewById(R.id.logListView);

        if (lv != null)
            lv.setAdapter(adapter);
        */
    }


}


