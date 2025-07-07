package com.dennisbrink.mt.global.mypackedfileviewer.events;

public class VideoThumbnailFinalEvent {
    public final int position;
    public final String target;
    public final String source;
    public final String zipkey;
    public VideoThumbnailFinalEvent(int position, String target, String source, String zipkey) {

        this.position = position;
        this.target = target;
        this.source = source;
        this.zipkey = zipkey;
    }
}
