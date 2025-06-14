package com.dennisbrink.mt.global.mypackedfileviewer.structures;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.dennisbrink.mt.global.mypackedfileviewer.libraries.ZipUtilities;

public class ZipEntryData {
    private final String fileName, displayDateTime, displaySize;
    private String cacheName, cacheFolder;
    private Bitmap thumbnail;

    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }

    public ZipEntryData(String fileName, long fileSize, long creationDate) {
        this.fileName = fileName;
        this.displayDateTime = ZipUtilities.convertTimestampToReadableFormat(creationDate);
        this.displaySize = ZipUtilities.convertBytesToKilobytes(fileSize);
    }

    public String getFileName() {
        return fileName;
    }

    public String getDisplayDateTime() {
        return displayDateTime;
    }

    public String getDisplaySize() {
        return displaySize;
    }

    public void setCacheName(String target, String filenameHash) {
        this.cacheName = "cache_" + target + filenameHash;
    }

    public String getCacheFolder() {
        return cacheFolder;
    }

    public void setCacheFolder(String cacheFolder) {
        this.cacheFolder = cacheFolder;
    }

    public String getCacheName() {
        return this.cacheName;
    }

    @NonNull
    @Override
    public String toString() {
        return "ZipEntryData{" +
                "cacheFolder='" + cacheFolder + '\'' +
                ", cacheName='" + cacheName + '\'' +
                ", displaySize='" + displaySize + '\'' +
                ", displayDateTime='" + displayDateTime + '\'' +
                ", fileName='" + fileName + '\'' +
                '}';
    }
}
