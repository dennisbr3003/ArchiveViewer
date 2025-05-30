package com.dennisbrink.mt.global.mypackedfileviewer;

public class ZipLibraryExtraData {

    private String fileSize, fileDate, numFiles, errorMessage, warningMessage;
    private Boolean isCopied = true;
    private Boolean inAssets = true;

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    // Getter and Setter for fileSize
    public String getFileSize() {
        return fileSize;
    }

    public Boolean getInAssets() {
        return inAssets;
    }

    public void setInAssets(Boolean inAssets) {
        this.inAssets = inAssets;
    }

    public void setCopied(Boolean copied) {
        isCopied = copied;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = ZipUtilities.convertBytesToKilobytes(fileSize);
    }

    // Getter and Setter for fileDate
    public String getFileDate() {
        return fileDate;
    }

    public void setFileDate(Long fileDate) {
        this.fileDate = ZipUtilities.convertTimestampToReadableFormat(fileDate);
    }

    // Getter and Setter for numFiles
    public String getNumFiles() {
        return numFiles;
    }

    public Boolean getIsCopied() {
        return this.isCopied;
    }

    public void setNumFiles(int numFiles) {
        this.numFiles = ZipApplication.getAppContext().getString(R.string.files) + numFiles;
    }
}