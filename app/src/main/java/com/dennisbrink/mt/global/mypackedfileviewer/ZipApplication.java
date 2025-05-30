package com.dennisbrink.mt.global.mypackedfileviewer;

import android.app.Application;
import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

public class ZipApplication extends Application {
    private static ZipApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

    }

    public static Context getAppContext() {
        return instance.getApplicationContext();
    }
}
