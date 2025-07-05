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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dennisbrink.mt.global.mypackedfileviewer.events.OpenZipLibraryFileEvent;
import com.dennisbrink.mt.global.mypackedfileviewer.libraries.ThumbnailCache;
import com.dennisbrink.mt.global.mypackedfileviewer.libraries.ZipUtilities;
import com.dennisbrink.mt.global.mypackedfileviewer.structures.ZipEntryData;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ZipFileAdapter extends RecyclerView.Adapter<ZipFileAdapter.ViewHolder> {
    private final String libraryTarget, libraryZipKey, librarySource;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final List<ZipEntryData> zipEntries;
    ThumbnailCache thumbnailCache = new ThumbnailCache();
    Bitmap placeholder = BitmapFactory.decodeResource(ZipApplication.getAppContext().getResources(), R.drawable.no_image_small);

    public ZipFileAdapter(List<ZipEntryData> zipEntries, int libraryPosition) {
        this.zipEntries = zipEntries;
        this.libraryTarget = ZipApplication.getLibraries().get(libraryPosition).getTarget();
        this.libraryZipKey = ZipApplication.getLibraries().get(libraryPosition).getZipkey();
        this.librarySource = ZipApplication.getLibraries().get(libraryPosition).getSource();

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

        if (!thumbnailCache.isThumbnailCached(entryData.getCacheFolder(), entryData.getCacheName())) {
            if (entryData.getThumbnail() == null) {
                executorService.execute(() -> {
                    InputStream inputStream = ZipUtilities.getImageInputStream(entryData.getFileName(), libraryTarget, libraryZipKey);
                    if (inputStream != null) {
                        Bitmap thumbnail = ZipUtilities.createThumbnail(inputStream, 45, 45, placeholder, entryData.getFileName());
                        try {
                            thumbnailCache.saveThumbnail(entryData.getCacheFolder(), entryData.getCacheName(), thumbnail);
                            if(position==0){ // save the first image as library thumbnail
                                thumbnailCache.saveThumbnail("", "cache_" + this.librarySource.hashCode(), thumbnail);
                            }
                        } catch (IOException e) {
                            Log.d("DB1", "ZipFileAdapter.onBindViewHolder: Saving the thumbnail to the app's files folder failed " + e.getMessage());
                        }
                        entryData.setThumbnail(thumbnail);

                        // Update the UI on the main thread using Handler
                        uiHandler.post(() -> holder.thumbNail.setImageBitmap(entryData.getThumbnail()));
                    } else {
                        Log.d("DB1", "ZipFileAdapter.onBindViewHolder: InputStream was null for file: " + entryData.getFileName());
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

        holder.itemView.setOnClickListener(v -> EventBus.getDefault().post(new OpenZipLibraryFileEvent(position, librarySource, libraryTarget, libraryZipKey, entryData.getFileType(), entryData.getFileName())));

    }

    @Override
    public int getItemCount() {
        return zipEntries.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView fileNameView, fileSizeView, creationDateView;
        public ImageView thumbNail;

        public ViewHolder(View view) {
            super(view);
            fileNameView = view.findViewById(R.id.fileName);
            fileSizeView = view.findViewById(R.id.fileSize);
            creationDateView = view.findViewById(R.id.creationDate);
            thumbNail = view.findViewById(R.id.thumbnail);
        }
    }

}