package com.dennisbrink.mt.global.mypackedfileviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ThumbnailCache {

    private final File cacheDir;

    public ThumbnailCache() {
        cacheDir = new File(ZipApplication.getAppContext().getFilesDir(), "thumbnails");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
    }

    public void saveThumbnail(String fileName, Bitmap thumbnail) throws IOException {
        File file = new File(cacheDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            thumbnail.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
    }

    public Bitmap loadThumbnail(String fileName) {
        File file = new File(cacheDir, fileName);
        if (file.exists()) {
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        }
        return null;
    }

    public boolean isThumbnailCached(String fileName) {
        File file = new File(cacheDir, fileName);
        return file.exists();
    }

    public void clearThumbNailCache() {
        File cacheDir = new File(ZipApplication.getAppContext().getFilesDir(), "thumbnails");
        if(cacheDir.exists() && cacheDir.isDirectory()){
            Log.d("DB1", "Cache folder found");
            String[] children = cacheDir.list();
            if (children != null) {
                for (String child : children) new File(cacheDir, child).delete();
            }
        }
    }

}

