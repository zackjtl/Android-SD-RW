package com.example.myapplication.ui.log;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;

import java.util.List;

public class LogFragment extends Fragment {

    private LogViewModel logViewModel;

    View rootView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        logViewModel = ViewModelProviders.of(this).get(LogViewModel.class);
        rootView = inflater.inflate(R.layout.fragment_log, container, false);

        final TextView textView = rootView.findViewById(R.id.text_log);

        logViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        UpdateLogFromMain();

        return rootView;
    }

    public void UpdateLogFromMain()
    {
        ArrayAdapter adapter = new ArrayAdapter(getContext(),
                android.R.layout.simple_list_item_1,
                ((MainActivity)getActivity()).getLogList());

        ListView lv = rootView.findViewById(R.id.logListView);

        if (lv != null)
            lv.setAdapter(adapter);

    }


}


