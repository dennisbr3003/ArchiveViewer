package com.dennisbrink.mt.global.mypackedfileviewer.events;

public class UpdateLibraryLockState {
    public String target;
    public int position;
    public UpdateLibraryLockState(String target, int position) {
        this.position = position;
        this.target = target;
    }
}
