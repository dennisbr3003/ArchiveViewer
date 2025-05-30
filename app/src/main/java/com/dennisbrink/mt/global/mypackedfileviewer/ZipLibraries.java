package com.dennisbrink.mt.global.mypackedfileviewer;

import androidx.annotation.NonNull;

import java.util.List;

public class ZipLibraries {

    private List<ZipLibrary> libraries;

    // Getter and Setter
    public List<ZipLibrary> getLibraries() {
        return libraries;
    }

    public String getTotalFileSize(){
        long totalSize = 0;
        for (ZipLibrary library : libraries) {
            totalSize += ZipUtilities.getZipLibrarySize(library.getTarget(), library.getSource());
        }
        return ZipUtilities.convertBytesToKilobytes(totalSize);
    }

}
