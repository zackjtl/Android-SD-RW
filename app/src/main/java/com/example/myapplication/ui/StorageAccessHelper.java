package com.example.myapplication.ui;

import android.content.Context;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StorageAccessHelper {
    public class WritableDirectory {
        public String Path;
        public String Root;
    };

    public static String[] GetWritableDirectories(Context Context, String RootPath)
    {
        File[] dirs = ContextCompat.getExternalFilesDirs(Context, null);

        List<String> dirList = new ArrayList<String>();
        String[] str = new String[dirs.length];

        for (File f : dirs)
        {
            if (RootPath != null) {
                if (f.getPath().contains(RootPath)) {
                    dirList.add(f.getPath());
                }
            }
            else {
                dirList.add(f.getPath());
            }
        }
        dirList.toArray(str);

        return str;
    }

    public static String[] GetWritableDirectories(Context Context)
    {
        return GetWritableDirectories(Context, null);
    }


}
