package com.example.myapplication.ui.home;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.StatFs;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.myapplication.ILogListener;
import com.example.myapplication.LogOwner;
import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.ui.StorageAccessHelper;
import com.example.myapplication.ui.StorageHelper;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends LogOwner {

    private HomeViewModel homeViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState)
    {
        // Create a viewmodel instance
        homeViewModel = ViewModelProviders.of(getActivity()).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        final TextView textView = root.findViewById(R.id.text_home);
        final ListView rootListView = root.findViewById(R.id.rootListView);

        // Bundle livedata observe
        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });


        homeViewModel.getRootDispList().observe(getViewLifecycleOwner(), new Observer<List<String>>() {
            @Override
            public void onChanged(List<String> strings) {
                ArrayAdapter adapter = new ArrayAdapter(getContext(),
                        android.R.layout.simple_list_item_1,
                        strings);

                rootListView.setAdapter(adapter);
                rootListView.setOnItemClickListener(new ListView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String rootDir = homeViewModel.getRootListItem(position);
                        String[] dirArray = StorageAccessHelper.GetWritableDirectories(getActivity(), rootDir);
                        String writableDir = null;
                        if (dirArray.length > 0) {
                            writableDir = dirArray[0];
                        }
                        addLog("Root selected: " + rootDir);
                        addLog("Target: " + writableDir);
                        ((MainActivity)getActivity()).setRootDir(rootDir);
                        ((MainActivity)getActivity()).setTargetDir(writableDir);
                        ((MainActivity)getActivity()).JumpToFragment(R.id.navigation_test);
                    }
                });
            };
        });

        Button refreshBtn = (Button)root.findViewById(R.id.refreshButton);

        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
            @Override
            public void onClick(View v) {
                List<StorageHelper.StorageVolume> storages = StorageHelper.getStorages(false);
                List<String> rootList = new ArrayList<String>();
                List<String> rootDispList = new ArrayList<String>();

                for (StorageHelper.StorageVolume storage : storages) {
                    String rootPath = storage.file.getAbsolutePath();
                    StatFs stat1 = new StatFs(rootPath);

                    long megaBytesFree = (stat1.getFreeBytes() >> 20);
                    long megaBytesTotal = (stat1.getTotalBytes() >> 20);

                    String freeStr = megaBytesFree >= 1024 ? String.format("%.2f GB", (double)megaBytesFree / 1024.0) : megaBytesFree + " GB";
                    String totalStr = megaBytesTotal >= 1024 ? String.format("%.2f GB", (double)megaBytesTotal / 1024.0) : megaBytesTotal + " GB";
                    String sizeStr = String.format(" [%s]", totalStr);

                    rootList.add(storage.file.getAbsolutePath());
                    rootDispList.add(storage.file.getAbsolutePath() + sizeStr);
                }
                homeViewModel.setRootList(rootList);
                homeViewModel.setRootDispList(rootDispList);
            }
        });

        return root;
    }
}
