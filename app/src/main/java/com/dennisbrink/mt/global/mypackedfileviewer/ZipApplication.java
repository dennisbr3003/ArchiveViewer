package com.dennisbrink.mt.global.mypackedfileviewer;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class ZipApplication extends Application {
    private static ZipApplication instance;

    static ZipLibraries zipLibraries = new ZipLibraries();

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        zipLibraries = loadLibrariesFromAssets("libraries-dev.json");
    }

    public static Context getAppContext() {
        return instance.getApplicationContext();
    }

    public static List<ZipLibrary> getLibraries(){
        return zipLibraries.getLibraries();
    }

    public static ZipLibraries getZipLibraries(){
        return zipLibraries;
    }

    private ZipLibraries loadLibrariesFromAssets(String fileName) {
        AssetManager assetManager = ZipApplication.getAppContext().getAssets();

        try (InputStream inputStream = assetManager.open(fileName);
             InputStreamReader reader = new InputStreamReader(inputStream)) {

            Gson gson = new Gson();
            return gson.fromJson(reader, ZipLibraries.class);

        } catch (IOException e) {
            return null;
        }
    }

}
