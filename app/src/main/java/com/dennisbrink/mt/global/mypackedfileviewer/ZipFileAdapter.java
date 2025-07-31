package com.dennisbrink.mt.global.mypackedfileviewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dennisbrink.mt.global.mypackedfileviewer.events.OpenZipLibraryFileEvent;
import com.dennisbrink.mt.global.mypackedfileviewer.events.VideoThumbnailFinalEvent;
import com.dennisbrink.mt.global.mypackedfileviewer.libraries.ThumbnailCache;
import com.dennisbrink.mt.global.mypackedfileviewer.libraries.ZipUtilities;
import com.dennisbrink.mt.global.mypackedfileviewer.structures.ZipEntryData;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ZipFileAdapter extends RecyclerView.Adapter<ZipFileAdapter.ViewHolder> implements IZipApplication {
    private final String libraryTarget, libraryZipKey, librarySource;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final List<ZipEntryData> zipEntries;
    ThumbnailCache thumbnailCache = new ThumbnailCache();
    Bitmap placeholder = BitmapFactory.decodeResource(ZipApplication.getAppContext().getResources(), R.drawable.no_image_small);
    private boolean blockClickListener = false;
   // ProgressBar progressBarLib2;

    public ZipFileAdapter(List<ZipEntryData> zipEntries, int libraryPosition) {
        this.zipEntries = zipEntries;
        this.libraryTarget = ZipApplication.getLibraries().get(libraryPosition).getTarget();
        this.libraryZipKey = ZipApplication.getLibraries().get(libraryPosition).getZipkey();
        this.librarySource = ZipApplication.getLibraries().get(libraryPosition).getSource();
        Log.d("DB1", "ZipFileAdapter.ZipFileAdapter - constructor complete: " + this.libraryTarget + "/" + this.libraryZipKey + "/" + this.librarySource);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.zip_entry_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        ZipEntryData entryData = zipEntries.get(position);
        holder.fileNameView.setText(entryData.getFileName());
        holder.fileSizeView.setText(String.valueOf(entryData.getDisplaySize()));
        holder.creationDateView.setText(String.valueOf(entryData.getDisplayDateTime()));

        Log.d("DB1", "ZipFileAdapter.onBindViewHolder - position " + position + ", thumbnail cached: " + thumbnailCache.isThumbnailCached(entryData.getCacheFolder(), entryData.getCacheName()));

        if (!thumbnailCache.isThumbnailCached(entryData.getCacheFolder(), entryData.getCacheName())) {
            if (entryData.getThumbnail() == null) {
                executorService.execute(() -> {
                    InputStream inputStream = null;
                    try {
                        inputStream = ZipUtilities.getImageInputStream(entryData.getFileName(), libraryTarget, libraryZipKey);
                    } catch (Exception e) {
                        Log.d("DB1", "ZipFileAdapter.onBindViewHolder - Error creating inputStream " + e.getMessage());
                    }
                    if (inputStream != null) {
                        Bitmap thumbnail = null;
                        try {
                            thumbnail = ZipUtilities.createThumbnail(inputStream, 45, 45, placeholder, entryData.getFileName());
                            thumbnailCache.saveThumbnail(entryData.getCacheFolder(), entryData.getCacheName(), thumbnail);
                            if (position == 0) { // save the first image as library thumbnail
                                thumbnailCache.saveThumbnail("", "cache_" + this.librarySource.hashCode(), thumbnail);
                            }
                        } catch (IOException e) {
                            Log.d("DB1", "ZipFileAdapter.onBindViewHolder - Saving the thumbnail to the app's files folder failed " + e.getMessage());
                        }
                        entryData.setThumbnail(thumbnail);

                        // Update the UI on the main thread using Handler
                        uiHandler.post(() -> holder.thumbNail.setImageBitmap(entryData.getThumbnail()));
                    } else {
                        Log.d("DB1", "ZipFileAdapter.onBindViewHolder - InputStream was null for file: " + entryData.getFileName());
                    }
                });

            } else {
                holder.thumbNail.setImageBitmap(entryData.getThumbnail());
            }
        } else {
            if (entryData.getThumbnail() == null) {
                entryData.setThumbnail(thumbnailCache.loadThumbnail(entryData.getCacheFolder(), entryData.getCacheName()));
            }
            holder.thumbNail.setImageBitmap(entryData.getThumbnail());
        }

        holder.itemView.setOnClickListener(v -> {
            if (blockClickListener) return;
            blockClickListener = true;

            executorService.execute(() -> {

                File tempFile = new File(ZipApplication.getAppContext().getFilesDir(), entryData.getFileName().hashCode() + ".mp4");
                if (!tempFile.exists()) {

                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (holder.progressBarLib2 != null) {
                            if (holder.progressBarLib2.getVisibility() == View.INVISIBLE) {
                                holder.progressBarLib2.setVisibility(View.VISIBLE);
                            } else {
                                holder.progressBarLib2.setVisibility(View.INVISIBLE);
                            }
                        }
                    });


                    byte[] videoBytes;
                    try {
                        ZipFile zipFile = new ZipFile(new File(ZipApplication.getAppContext().getFilesDir(), libraryTarget), libraryZipKey.toCharArray());
                        FileHeader fileHeader = zipFile.getFileHeader(entryData.getFileName());
                        InputStream inputStream = zipFile.getInputStream(fileHeader);
                        videoBytes = inputStream.readAllBytes();
                    } catch (Exception e) {
                        Log.d("DB1", "Unable to create inputStream: " + e.getMessage());
                        new Handler(Looper.getMainLooper()).post(() -> hideProgressbar(holder));
                        return;
                    }

                    ZipUtilities.saveByteDataToFile(entryData.getFileName().hashCode() + ".mp4", "", videoBytes);
                    new Handler(Looper.getMainLooper()).post(() -> hideProgressbar(holder));

                }
                EventBus.getDefault().post(new OpenZipLibraryFileEvent(position, librarySource, libraryTarget, libraryZipKey, entryData));
            });

        });

    }

    private void hideProgressbar(ViewHolder holder) {
        if (holder.progressBarLib2 != null) {
            holder.progressBarLib2.setVisibility(holder.progressBarLib2.getVisibility() == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return zipEntries.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView fileNameView, fileSizeView, creationDateView;
        public ImageView thumbNail;
        public ProgressBar progressBarLib2;

        public ViewHolder(View view) {

            super(view);
            try {
                fileNameView = view.findViewById(R.id.fileName);
                fileSizeView = view.findViewById(R.id.fileSize);
                creationDateView = view.findViewById(R.id.creationDate);
                thumbNail = view.findViewById(R.id.thumbnail);
                progressBarLib2 = view.findViewById(R.id.progressBarLib2);
            } catch (Exception e) {
                Log.d("DB1", "ZipFileAdapter.ViewHolder.ViewHolder (constructor) - Error: " + e.getMessage());
            }
        }
    }

}