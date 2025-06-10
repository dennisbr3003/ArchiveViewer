package com.dennisbrink.mt.global.mypackedfileviewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ThumbnailCache implements IZipApplication {

    public ThumbnailCache() {
        // create all folder if needed
        List<ZipLibrary> libraries = ZipApplication.getLibraries();
        for(ZipLibrary library : libraries) {
            File cacheDir = new File(ZipApplication.getAppContext().getFilesDir(), CACHE_DIR + library.getTarget());
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
        }
    }

    public void saveThumbnail(String folder, String fileName, Bitmap thumbnail) throws IOException {
        File cacheDir = new File(ZipApplication.getAppContext().getFilesDir(), CACHE_DIR + folder);
        File file = new File(cacheDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            thumbnail.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
    }

    public Bitmap loadThumbnail(String folder, String fileName) {
        File cacheDir = new File(ZipApplication.getAppContext().getFilesDir(), CACHE_DIR + folder);
        File file = new File(cacheDir, fileName);
        if (file.exists()) {
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        }
        return null;
    }

    public boolean isThumbnailCached(String folder, String fileName) {
        File cacheDir = new File(ZipApplication.getAppContext().getFilesDir(), CACHE_DIR + folder);
        File file = new File(cacheDir, fileName);
        return file.exists();
    }

    public void clearThumbNailCacheFolder(String folder) {

        File cacheDir = new File(ZipApplication.getAppContext().getFilesDir(), CACHE_DIR + folder);
        if(cacheDir.exists() && cacheDir.isDirectory()){
            Log.d("DB1", "Cache folder thumbnails/" + folder + " is being cleared");
            String[] children = cacheDir.list();
            if (children != null) {
                for (String child : children) new File(cacheDir, child).delete();
            }
        }
    }

    public void clearCacheFolder(String folder) {
        Log.d("DB1", "Clear folder " + folder);
        File cacheDir = new File(ZipApplication.getAppContext().getFilesDir(), folder);
        if (cacheDir.exists() && cacheDir.isDirectory()) {
            deleteDirectoryContents(cacheDir);
        }
    }

    public void clearAll() {
        Log.d("DB1", "Clear all app data");
        File cacheDir = new File(ZipApplication.getAppContext().getFilesDir(), "");
        if (cacheDir.exists() && cacheDir.isDirectory()) {
            deleteDirectoryContents(cacheDir);
        }
    }

    private void deleteDirectoryContents(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectoryContents(file);
                }
                file.delete();
            }
        }
    }

}

