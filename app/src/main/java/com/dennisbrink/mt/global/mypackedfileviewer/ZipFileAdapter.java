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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ZipFileAdapter extends RecyclerView.Adapter<ZipFileAdapter.ViewHolder> {
    private final String libraryName;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private List<ZipEntryData> zipEntries;
    ThumbnailCache thumbnailCache = new ThumbnailCache();
    Bitmap placeholder = BitmapFactory.decodeResource(ZipApplication.getAppContext().getResources(), R.drawable.unsupported_small);
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    private OnItemClickListener listener;

    public ZipFileAdapter(List<ZipEntryData> zipEntries, OnItemClickListener listener, String libraryName) {
        this.zipEntries = zipEntries;
        this.listener = listener;
        this.libraryName = libraryName;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView fileNameView, fileSizeView, creationDateView;
        public ImageView thumbNail;

        public ViewHolder(View view, OnItemClickListener listener) {
            super(view);
            fileNameView = view.findViewById(R.id.fileName);
            fileSizeView = view.findViewById(R.id.fileSize);
            creationDateView = view.findViewById(R.id.creationDate);
            thumbNail = view.findViewById(R.id.thumbnail);

            view.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAbsoluteAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(position);
                    }
                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.zip_entry_item, parent, false);
        return new ViewHolder(view, this.listener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ZipEntryData entryData = zipEntries.get(position);
        holder.fileNameView.setText(entryData.getFileName());
        holder.fileSizeView.setText(String.valueOf(entryData.getDisplaySize()));
        holder.creationDateView.setText(String.valueOf(entryData.getDisplayDateTime()));

        if (!thumbnailCache.isThumbnailCached(entryData.getCacheName())) {
            if (entryData.getThumbnail() == null) {
                executorService.execute(() -> {
                    InputStream inputStream = ZipUtilities.getImageInputStream(entryData.getFileName());
                    if (inputStream != null) {
                        Bitmap thumbnail = ZipUtilities.createThumbnail(inputStream, 45, 45, placeholder);
                        try {
                            thumbnailCache.saveThumbnail(entryData.getCacheName(), thumbnail);
                            if(position==0){ // save the first image as library thumbnail
                                thumbnailCache.saveThumbnail("cache_" + this.libraryName.hashCode(), thumbnail);
                            }
                        } catch (IOException e) {
                            Log.d("DB1", "Saving the thumbnail to the app's files folder failed " + e.getMessage());
                        }
                        entryData.setThumbnail(thumbnail);

                        // Update the UI on the main thread using Handler
                        uiHandler.post(() -> holder.thumbNail.setImageBitmap(entryData.getThumbnail()));
                    } else {
                        Log.d("DB1", "InputStream was null for file: " + entryData.getFileName());
                    }
                });
            } else {
                holder.thumbNail.setImageBitmap(entryData.getThumbnail());
            }
        } else {
            if (entryData.getThumbnail() == null) {
                entryData.setThumbnail(thumbnailCache.loadThumbnail(entryData.getCacheName()));
            }
            holder.thumbNail.setImageBitmap(entryData.getThumbnail());
        }
    }

    @Override
    public int getItemCount() {
        return zipEntries.size();
    }
}