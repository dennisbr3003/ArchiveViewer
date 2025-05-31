package com.dennisbrink.mt.global.mypackedfileviewer;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.util.Log;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ZipUtilities {

    static AssetManager assetManager = ZipApplication.getAppContext().getAssets();
    static String[] assets;
    static ZipFile zipFile;

    public static void initAssetManager() throws IOException {
        assets = assetManager.list("");
    }

    private static Boolean checkIfFileExistsInAssetsFolder(String target) {
        for (String asset : assets) {
            if (asset.equals(target)) {
                return true;
            }
        }
        return false;
    }

    public static ZipLibraryExtraData getFileData(String target, String source, String zipkey) {

        File tempFile = new File(ZipApplication.getAppContext().getFilesDir(), target);
        ZipLibraryExtraData zipLibraryExtraData = new ZipLibraryExtraData();

        if(!tempFile.exists()){
            zipLibraryExtraData.setCopied(false);
            zipLibraryExtraData.setWarningMessage(ZipApplication.getAppContext().getString(R.string.archive_is_not_copied));
        }
        if(!checkIfFileExistsInAssetsFolder(source)) {
            zipLibraryExtraData.setInAssets(false);
            zipLibraryExtraData.setErrorMessage(ZipApplication.getAppContext().getString(R.string.archive_not_found));
        }

        if(tempFile.exists()) {
            // first check the file without opening it
            try (ZipFile zipFile = new ZipFile(tempFile)) {
                zipLibraryExtraData.setValidZip(zipFile.isValidZipFile());
                if (zipFile.isValidZipFile()) {
                    if (!zipkey.isEmpty() && zipFile.isEncrypted()) {
                        zipLibraryExtraData.setLockState(LockStatus.LOCKED_PASSWORD);
                    }
                    if (zipkey.isEmpty() && zipFile.isEncrypted()) {
                        zipLibraryExtraData.setLockState(LockStatus.LOCKED_NO_PASSWORD);
                    }
                    if (!zipFile.isEncrypted()) {
                        zipLibraryExtraData.setLockState(LockStatus.NOT_LOCKED);
                    }
                }
            } catch (IOException e) {
                zipLibraryExtraData.setValidZip(false);
                zipLibraryExtraData.setErrorMessage(ZipApplication.getAppContext().getString(R.string.invalid_zip_archive));
                zipLibraryExtraData.setLockState(LockStatus.UNKNOWN);
            }

            
            try (ZipFile zipFile = new ZipFile(tempFile, zipkey.toCharArray())) {
                zipLibraryExtraData.setFileDate(tempFile.lastModified());
                zipLibraryExtraData.setFileSize(tempFile.length());
                zipLibraryExtraData.setNumFiles(zipFile.getFileHeaders().size());
            } catch (IOException e) {
                zipLibraryExtraData.setValidZip(false);
                zipLibraryExtraData.setErrorMessage(ZipApplication.getAppContext().getString(R.string.invalid_password));
                zipLibraryExtraData.setLockState(LockStatus.LOCKED_CORRUPTED);
            }
        } else {
            zipLibraryExtraData.setLockState(LockStatus.UNKNOWN);
            if(checkIfFileExistsInAssetsFolder(source)){
                try {
                    zipLibraryExtraData.setFileSize(assetManager.open(source).available());
                } catch (IOException e) {
                    zipLibraryExtraData.setFileSize(0);
                }
            } else {
                zipLibraryExtraData.setFileSize(0);
            }
        }

        return zipLibraryExtraData;

    }

    public static long getZipLibrarySize(String target, String source) {
        long size;
        File tempFile = new File(ZipApplication.getAppContext().getFilesDir(), target);
        if(tempFile.exists()){
            size = tempFile.length();
        } else {
            try {
                size = assetManager.open(source).available();
            } catch (IOException e) {
                size = 0;
            }
        }
        return size;
    }

    public static String convertBytesToKilobytes(long bytes) {

        // Convert bytes to kilobytes or megabytes
        double units = bytes;
        String unit = "B";
        Locale locale = ZipApplication.getAppContext().getResources().getConfiguration().getLocales().get(0);

        if(bytes > 1024 && bytes <= (1024 * 1024)) {
            units = bytes / 1024.0;
            unit = "KB";
        }
        if(bytes > (1024 * 1024)) {
            units = bytes / (1024.0 * 1024);
            unit = "MB";
        }

        // Format the units with thousand separators
        NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
        numberFormat.setMaximumFractionDigits(1);
        return numberFormat.format(units) + " " + unit;
    }

    public static String convertTimestampToReadableFormat(long timestamp) {

        Locale locale = ZipApplication.getAppContext().getResources().getConfiguration().getLocales().get(0);

        // Convert epoch to LocalDateTime using system's default time zone
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());

        // Define the desired format in 24-hour time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy, HH:mm", locale);
        return dateTime.format(formatter);
    }

    public static Bitmap createThumbnail(InputStream inputStream, int width, int height, Bitmap placeholder) {
        long startTime = System.currentTimeMillis();
        Bitmap thumbnail = null;

        try {
            // Attempt to decode the input stream into a bitmap
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);

            // Check if the decoding failed, possibly due to unsupported format
            if (originalBitmap != null) {
                // Create the thumbnail
                thumbnail = ThumbnailUtils.extractThumbnail(originalBitmap, width, height);
            } else {
                Log.d("DB1", "Decoding failed: Unsupported file format or corrupt image.");
            }
        } catch (Exception e) {
            Log.d("DB1", "Thumbnail generation failed: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        Log.d("DB1", "Execution time thumbnail generation: " + executionTime + " ms");

        return thumbnail != null ? thumbnail : placeholder;

    }

    public static InputStream getImageInputStream(String fileName) {

        // ZipFile zipFile = new ZipFile(new File(ZipApplication.getAppContext().getFilesDir(), target), zipkey.toCharArray());

        FileHeader fileHeader = null;

        // Get the file header for the file you want
        try {
            fileHeader = zipFile.getFileHeader(fileName);
        } catch(Exception e) {
            Log.d("DB1", Objects.requireNonNull(e.getMessage()));
        }

        assert fileHeader != null;
        Log.d("DB1", fileHeader.getFileName());

        // Create an InputStream from the zip entry
        try {
            return zipFile.getInputStream(fileHeader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setZipFile(ZipFile zFile) {
        // Perform any necessary validation or setup here
        zipFile = zFile;
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "UND"; // No extension found
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toUpperCase();
    }

    public static List<ZipEntryData> getZipContentsFromAsset(String source, String target, String zipkey) {
        List<ZipEntryData> entryDataList = new ArrayList<>();

        Context context = ZipApplication.getAppContext();

        // Copy ZIP file from assets to a file if it does not already exist
        File tempFile = new File(context.getFilesDir(), target);
        if (!tempFile.exists()) {
            try (InputStream inputStream = context.getAssets().open(source);
                FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
            catch (Exception e){
                Log.d("DB1", "Zip file could not be copied to files dir: " + Objects.requireNonNull(e.getMessage()));
            }
        }

        // file is copied form assets to files dir where zip4j can reach it. Zip4j cannot read from stream
        // We must now read the contents from the library, assuming we have access for now
        if (tempFile.exists()) {
            try (ZipFile zipFile = new ZipFile(tempFile, zipkey.toCharArray())) {
                Log.d("DB1", "zipFile is zip file? " + zipFile.isValidZipFile());
                Log.d("DB1", "zipFile is encrypted zip file? " + zipFile.isEncrypted());
                Log.d("DB1", "zipFile is there a password? " + !zipkey.isEmpty());

                setZipFile(zipFile);

                for (FileHeader fileHeader : zipFile.getFileHeaders()) {
                    ZipEntryData data = new ZipEntryData(
                            fileHeader.getFileName(),
                            fileHeader.getUncompressedSize(),
                            fileHeader.getLastModifiedTimeEpoch()
                    );
                    data.setCacheName(target, String.valueOf(fileHeader.getFileName().hashCode()));
                    entryDataList.add(data);
                    Log.d("DB1", data.toString());
                }
            } catch (Exception e) {
                Log.d("DB1", "Zip file " + source + " could not be read: " + Objects.requireNonNull(e.getMessage()));
            }
        }
        return entryDataList;
    }

}
