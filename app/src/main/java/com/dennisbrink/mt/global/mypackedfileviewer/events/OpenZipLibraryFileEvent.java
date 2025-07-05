package com.dennisbrink.mt.global.mypackedfileviewer.events;

import com.dennisbrink.mt.global.mypackedfileviewer.EFileTypes;

public class OpenZipLibraryFileEvent {
    public final int position;
    public final String source, target, zipkey, fileName;
    public final EFileTypes eFileType;
    public OpenZipLibraryFileEvent(int position, String source, String target, String zipkey, EFileTypes fileType, String fileName) {
        this.position = position;
        this.source = source;
        this.target = target;
        this.zipkey = zipkey;
        this.eFileType = fileType;
        this.fileName = fileName;
    }
}
