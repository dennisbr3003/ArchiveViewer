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
    private Boolean isFinal; // used for video's that get the thumbnail after the video is actually played
                             // initially each video will have the placeholder until it is played and this
                             // switch is set
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

    public void setFinal(Boolean aFinal) { isFinal = aFinal; }

    public Boolean getFinal() { return isFinal; }

    @NonNull
    @Override
    public String toString() {
        return "ZipEntryData{" +
                "fileName='" + fileName + '\'' +
                ", displayDateTime='" + displayDateTime + '\'' +
                ", displaySize='" + displaySize + '\'' +
                ", eFileType=" + eFileType +
                ", cacheName='" + cacheName + '\'' +
                ", cacheFolder='" + cacheFolder + '\'' +
                ", thumbnail=" + thumbnail +
                ", isFinal=" + isFinal +
                '}';
    }
}
