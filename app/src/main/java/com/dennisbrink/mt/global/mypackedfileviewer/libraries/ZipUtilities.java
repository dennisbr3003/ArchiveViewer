package com.dennisbrink.mt.global.mypackedfileviewer.libraries;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.dennisbrink.mt.global.mypackedfileviewer.IZipApplication;
import com.dennisbrink.mt.global.mypackedfileviewer.LockStatus;
import com.dennisbrink.mt.global.mypackedfileviewer.R;
import com.dennisbrink.mt.global.mypackedfileviewer.ZipApplication;
import com.dennisbrink.mt.global.mypackedfileviewer.EVideoExtensions;
import com.dennisbrink.mt.global.mypackedfileviewer.ZipLibraryActivity;
import com.dennisbrink.mt.global.mypackedfileviewer.events.UpdateLibraryLockState;
import com.dennisbrink.mt.global.mypackedfileviewer.structures.Coordinates;
import com.dennisbrink.mt.global.mypackedfileviewer.structures.ZipEntryData;
import com.dennisbrink.mt.global.mypackedfileviewer.structures.ZipLibraryExtraData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ZipUtilities implements IZipApplication {

    static AssetManager assetManager = ZipApplication.getAppContext().getAssets();
    static String[] assets;
   // static ZipFile zipFile;
   //private static ZipLibraryExtraData zipLibraryExtraData;
    public static void initAssetManager() throws IOException {
        assets = assetManager.list("");
    }
    ZipLibraryExtraData zipLibraryExtraData = new ZipLibraryExtraData();
    private static Boolean checkIfFileExistsInAssetsFolder(String target) {
        for (String asset : assets) {
            if (asset.equals(target)) {
                return true;
            }
        }
        return false;
    }

    public static boolean copyZipFromAsset(String target, String source) {

        File tempFile = new File(ZipApplication.getAppContext().getFilesDir(), target);

        if (!tempFile.exists()) {
            try (InputStream inputStream = ZipApplication.getAppContext().getAssets().open(source);
                 FileOutputStream outputStream = new FileOutputStream(tempFile)) {

                byte[] buffer = new byte[1024];
                int length;

                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

            }
            catch (Exception e){
                Log.d("DB1", "ZipUtilities.copyZipFromAssetZip: file could not be copied to files dir: " + Objects.requireNonNull(e.getMessage()));
                return false; // not copied
            }
        }
        return true; // file exists or is copied successfully
    }
    public static boolean isValidZipPassword(String target, String zipkey){
        // the file exist
        File tempFile = new File(ZipApplication.getAppContext().getFilesDir(), target);

        try (ZipFile zipFile = new ZipFile(tempFile, zipkey.toCharArray())) {
            for (FileHeader fileHeader : zipFile.getFileHeaders()) {
                // Attempt to extract
                InputStream is = zipFile.getInputStream(fileHeader);
                byte[] b = new byte[4 * 4096];
                while (is.read(b) != -1) {
                    // Do nothing as we just want to verify password
                }
                is.close();
                // Exit after the first successful read
                break;
            }
        } catch (Exception e) {
            return false; // password not correct
        }
        return true; // password correct

    }
    public static ZipLibraryExtraData getZipLibraryExtraData(String target, String source, String zipkey, int position) {

        // this is the data that can be gathered from the
        // asset folder without actually copying the file
        // we do not check for a valid password here because
        // that is done when the file is actually opened

        ZipLibraryExtraData zipLibraryExtraData;
        // check for a saved version if any
        zipLibraryExtraData = getZipLibraryExtraDataObjectFromFile(target);
        if (zipLibraryExtraData == null) {
            zipLibraryExtraData = new ZipLibraryExtraData();
        }
        File tempFile = new File(ZipApplication.getAppContext().getFilesDir(), target);

        if(!tempFile.exists()){
            zipLibraryExtraData.setCopied(false);
            zipLibraryExtraData.setWarningMessage(ZipApplication.getAppContext().getString(R.string.archive_is_not_copied));
            zipLibraryExtraData.setLockState(LockStatus.UNKNOWN);

            if(checkIfFileExistsInAssetsFolder(source)){
                try {
                    zipLibraryExtraData.setFileSize(assetManager.open(source).available());
                    zipLibraryExtraData.setInAssets(true);
                } catch (IOException e) {
                    zipLibraryExtraData.setFileSize(0);
                    zipLibraryExtraData.setInAssets(false);
                    zipLibraryExtraData.setErrorMessage(ZipApplication.getAppContext().getString(R.string.archive_not_found)); // override previous message if any
                }
            } else {
                zipLibraryExtraData.setInAssets(false);
                zipLibraryExtraData.setErrorMessage(ZipApplication.getAppContext().getString(R.string.archive_not_found)); // override previous message if any
                zipLibraryExtraData.setFileSize(0);
            }
        } else {
            try (ZipFile zipFile = new ZipFile(tempFile)) {

                zipLibraryExtraData.setValidZip(zipFile.isValidZipFile());
                zipLibraryExtraData.setCopied(true);
                zipLibraryExtraData.setInAssets(true);

                if (zipFile.isValidZipFile()) {
                    if(zipFile.isEncrypted()) {
                        if (zipkey.isEmpty()) {
                            zipLibraryExtraData.setLockState(LockStatus.LOCKED_NO_PASSWORD);
                        } else {
                            if(zipLibraryExtraData.getLockState() != LockStatus.LOCKED_CORRUPTED) {
                                zipLibraryExtraData.setLockState(LockStatus.LOCKED_PASSWORD);
                            }
                        }
                    } else {
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
        }

        saveZipLibraryExtraDataObjectToFile(target, zipLibraryExtraData);
        return zipLibraryExtraData;

    }

    public static ZipLibraryExtraData getZipLibraryExtraDataObjectFromFile(String target){
        return jsonToZipLibraryExtraDataObject(loadDataFromFile(LIB_EXTRA_DATA_PREFIX + target, FILE_EXTRA_DIR));
    }

    public static void saveZipLibraryExtraDataObjectToFile(String target, ZipLibraryExtraData zipLibraryExtraData){
        saveDataToFile(LIB_EXTRA_DATA_PREFIX + target, FILE_EXTRA_DIR, zipLibraryExtraDataObjectToJson(zipLibraryExtraData));
    }
    private static String zipLibraryExtraDataObjectToJson(ZipLibraryExtraData zipLibraryExtraData) {
        Gson gson = new Gson();
        return gson.toJson(zipLibraryExtraData);
    }
    private static ZipLibraryExtraData jsonToZipLibraryExtraDataObject(String jsonString) {
        Gson gson = new Gson();
        return gson.fromJson(jsonString, ZipLibraryExtraData.class);
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

    public static Bitmap createThumbnail(InputStream inputStream, int width, int height, Bitmap placeholder, String fileName) {

        Bitmap thumbnail = null;

        // check if it is a video extension before trying to create a thumbnail
        if(!EVideoExtensions.contains(getFileExtension(fileName))) {
            try {
                // Attempt to decode the input stream into a bitmap
                Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);

                // Check if the decoding failed, possibly due to unsupported format
                if (originalBitmap != null) {
                    // Create the thumbnail
                    thumbnail = ThumbnailUtils.extractThumbnail(originalBitmap, width, height);
                } else {
                    Log.d("DB1", "ZipUtilities.createThumbnail: Decoding failed: Unsupported file format or corrupt image.");
                }
            } catch (Exception e) {
                Log.d("DB1", "ZipUtilities.createThumbnail: Thumbnail generation failed: " + e.getMessage());
            }
        } else {
            Log.d("DB1", "ZipUtilities.createThumbnail: file is found to be a video file");
        }
        return thumbnail != null ? thumbnail : placeholder;

    }

    public static InputStream getImageInputStream(String fileName, String target, String zipkey) {

        ZipFile zipFile = new ZipFile(new File(ZipApplication.getAppContext().getFilesDir(), target), zipkey.toCharArray());

        FileHeader fileHeader = null;

        // Get the file header for the file you want
        try {
            fileHeader = zipFile.getFileHeader(fileName);
        } catch(Exception e) {
            Log.d("DB1", "ZipUtilities.getImageInputStream: " + e.getMessage());
        }

        assert fileHeader != null;

        // Create an InputStream from the zip entry
        try {
            return zipFile.getInputStream(fileHeader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "UND"; // No extension found
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toUpperCase();
    }

    public static List<ZipEntryData> getZipContentsFromAsset(String source, String target, String zipkey) {

        List<ZipEntryData> entryDataList = new ArrayList<>();
        String strData;
        // first check if we have a saved version for this library, if we have we will use that
        strData = loadDataFromFile(target, FILE_EXTRA_DIR);
        if(!strData.isEmpty()){
            try {
                entryDataList = jsonToZipEntryDataList(strData);
                Log.d("DB1", "ZipUtilities.getZipContentsFromAsset: Extra data retrieved from file " + FILE_EXTRA_DIR + target);
                return entryDataList;
            } catch(Exception e) {
                // something went wrong so we must continue with actually iterating file headers
                Log.d("DB1", "ZipUtilities.getZipContentsFromAsset: Extra data was found but could not be retrieved from file " + FILE_EXTRA_DIR + target);
            }
        }

        Context context = ZipApplication.getAppContext();

        // Copy ZIP file from assets to a file if it does not already exist
        copyZipFromAsset(target, source);
        File tempFile = new File(context.getFilesDir(), target);

        // file is copied form assets to files dir where zip4j can reach it. Zip4j cannot read from stream
        // We must now read the contents from the library, assuming we have access for now
        try (ZipFile zipFile = new ZipFile(tempFile, zipkey.toCharArray())) {

            for (FileHeader fileHeader : zipFile.getFileHeaders()) {
                ZipEntryData data = new ZipEntryData(
                        fileHeader.getFileName(),
                        fileHeader.getUncompressedSize(),
                        fileHeader.getLastModifiedTimeEpoch()
                );
                data.setCacheName(target, String.valueOf(fileHeader.getFileName().hashCode()));
                data.setCacheFolder(target);
                data.setFinal(false);
                entryDataList.add(data);
            }

            // got the extra data here, we now save it for quick extraction next time
            saveDataToFile(target, FILE_EXTRA_DIR, zipEntryDataListToJson(entryDataList));
            Log.d("DB1", "ZipUtilities.getZipContentsFromAsset: Extra data saved to file " + FILE_EXTRA_DIR + target);

        } catch (Exception e) {
            Log.d("DB1", "ZipUtilities.getZipContentsFromAsset: Zip file " + source + " could not be read -> " + e.getMessage());
        }

        return entryDataList;
    }

    public static void saveDataToFile(String fileName, String folder, String data) {

        Log.d("DB1", "ZipUtilities.saveDataToFile: saving data for -> " + data);

        File extraDataDir = new File(ZipApplication.getAppContext().getFilesDir(), folder);
        if (!extraDataDir.exists()) extraDataDir.mkdirs();

        File file = new File(extraDataDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data.getBytes());
            fos.flush();
        } catch (IOException e) {
            // do nothing, we just have no file that's all
            Log.d("DB1", "ZipUtilities.saveDataToFile: Non lethal error saving data -> " + e.getMessage());
        }
    }

    public static String loadDataFromFile(String fileName, String folder) {

        File extraDataDir = new File(ZipApplication.getAppContext().getFilesDir(), folder);
        File file = new File(extraDataDir, fileName);

        StringBuilder data = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                data.append(line).append("\n");
            }
        } catch (IOException e) {
            // do nothing, just no extra data from file
            Log.d("DB1", "ZipUtilities.loadDataFromFile: Non lethal error loading data -> " + e.getMessage());
        }

        return data.toString().trim();
    }

    private static String zipEntryDataListToJson(List<ZipEntryData> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }
    private static List<ZipEntryData> jsonToZipEntryDataList(String jsonString) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<ZipEntryData>>() {}.getType();
        return gson.fromJson(jsonString, listType);
    }

    // Convert Coordinates object to JSON
    public static String coordinatesToJson(Coordinates coordinates) {
        Gson gson = new Gson();
        return gson.toJson(coordinates);
    }

    // Convert JSON to Coordinates object
    public static Coordinates jsonToCoordinates(String jsonString) {
        Gson gson = new Gson();
        return gson.fromJson(jsonString, Coordinates.class);
    }



    public static void saveByteDataToFile(String fileName, String folder, byte[] data) {
        File extraDataDir = new File(ZipApplication.getAppContext().getFilesDir(), folder);
        if (!extraDataDir.exists()) extraDataDir.mkdirs();

        File file = new File(extraDataDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
            fos.flush();
        } catch (IOException e) {
            // do nothing, we just have no file that's all
            Log.d("DB1", "ZipUtilities.saveByteDataToFile: Non lethal error saving data -> " + e.getMessage());
        }

    }

    public static void saveZipEntriesToFile(String target, List<ZipEntryData> zipEntries) {
        saveDataToFile(target, FILE_EXTRA_DIR, zipEntryDataListToJson(zipEntries));
    }

}
