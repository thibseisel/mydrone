package fr.telecomlille.mydrone;

import android.app.Application;

import com.parrot.arsdk.ARSDK;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ARSDK.loadSDKLibs();
    }
}
