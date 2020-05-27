package com.example.sdtesttool;

import android.Manifest;
import android.os.Bundle;

import com.example.sdtesttool.ui.home.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
    implements ILogListener
{

    private static final int REQUEST_WRITE_STORAGE = 112;

    HomeFragment mHomeFragment;
    NavHostFragment mHostFragment;

    String rootDir = null;
    String targetDir = null;

    List<String> mLogList;

    MutableLiveData<String> logData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_test, R.id.navigation_log)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        mHostFragment = (NavHostFragment)getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_WRITE_STORAGE);

        mLogList = new ArrayList<>();
        logData = new MutableLiveData<>();

        //JumpToFragment(2);
    }

    public void setRootDir(String value) {rootDir = value;}
    public void setTargetDir(String value) {targetDir = value;}
    public String getRootDir() {return rootDir;}
    public String getTargetDir() {return targetDir;}

    public void JumpToFragment(int id)
    {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        navController.navigate(id);
    }

    public List<String> getLogList()
    {
        return mLogList;
    }

    public LiveData<String> getLogData() {return logData;}

    public void onLog(String Text)
    {
        ////mLogList.add(Text);
        logData.setValue(logData.getValue() + Text + "\n");
        ////logString += Text + "\n";
    }

    @Override
    public void onAttachFragment(@NonNull Fragment fragment) {

    }


}
