package com.dennisbrink.mt.global.mypackedfileviewer.structures;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.dennisbrink.mt.global.mypackedfileviewer.EFileTypes;
import com.dennisbrink.mt.global.mypackedfileviewer.EVideoExtensions;
import com.dennisbrink.mt.global.mypackedfileviewer.libraries.ZipUtilities;

public class ZipEntryData {
    private final String fileName, displayDateTime, displaySize;
    private final EFileTypes eFileType;
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
        this.eFileType = setFileType(this.fileName);
    }

    private EFileTypes setFileType(String fileName) {
        return EVideoExtensions.contains(ZipUtilities.getFileExtension(fileName))? EFileTypes.VIDEO : EFileTypes.IMAGE;
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

    public EFileTypes getFileType() { return eFileType; }

    public String getCacheFolder() {
        return cacheFolder;
    }

    public void setCacheFolder(String cacheFolder) {
        this.cacheFolder = cacheFolder;
    }

    public String getCacheName() {
        return this.cacheName;
    }

}
