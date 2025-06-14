package com.dennisbrink.mt.global.mypackedfileviewer.structures;

public class ZipLibrary {

    private String source, target, name, zipkey;
    private int sequence;

    // Getters and setters
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getZipkey() {
        return zipkey;
    }

    public void setZipkey(String zipkey) {
        this.zipkey = zipkey;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
