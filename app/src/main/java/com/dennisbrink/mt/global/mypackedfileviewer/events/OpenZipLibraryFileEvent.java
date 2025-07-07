package com.dennisbrink.mt.global.mypackedfileviewer.events;

import com.dennisbrink.mt.global.mypackedfileviewer.structures.ZipEntryData;

public class OpenZipLibraryFileEvent {
    public final int position;
    public final String source, target, zipkey;
    public final ZipEntryData entryData;
    public OpenZipLibraryFileEvent(int position, String source, String target, String zipkey, ZipEntryData entryData ) {
        this.position = position;
        this.source = source;
        this.target = target;
        this.zipkey = zipkey;
        this.entryData = entryData;
    }
}
