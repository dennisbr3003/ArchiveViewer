package com.dennisbrink.mt.global.mypackedfileviewer;

public enum EVideoExtensions {
    WMV, AVI, MP4, FLV;

    // Method to check if a given extension is in the video list
    public static boolean contains(String extension) {
        for (EVideoExtensions ext : EVideoExtensions.values()) {
            if (ext.name().equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }
}
