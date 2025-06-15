package com.dennisbrink.mt.global.mypackedfileviewer.events;

public class OpenZipLibraryFileEvent {
    public final int position;
    public final String source, target, zipkey;
    public OpenZipLibraryFileEvent(int position, String source, String target, String zipkey) {
        this.position = position;
        this.source = source;
        this.target = target;
        this.zipkey = zipkey;
    }
}
