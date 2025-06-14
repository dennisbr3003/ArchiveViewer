package com.dennisbrink.mt.global.mypackedfileviewer;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.ObjectKey;
import com.dennisbrink.mt.global.mypackedfileviewer.libraries.ZipUtilities;
import com.dennisbrink.mt.global.mypackedfileviewer.structures.ZipEntryData;
import com.github.chrisbanes.photoview.PhotoView;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
    private final List<ZipEntryData> zipEntries;
    private final String zipkey, target;
    public ImageAdapter(List<ZipEntryData> zipEntries, String zipkey, String target) {
        this.zipEntries = zipEntries;
        this.zipkey = zipkey;
        this.target = target;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        PhotoView photoView; // https://www.geeksforgeeks.org/photoview-in-android/
        ImageView imageView;
        public TextView fileNameView, fileExtensionView, fileDateView, fileSizeView, filePositionView;

        public ViewHolder(View view) {
            super(view);
            photoView = view.findViewById(R.id.photoView);
            imageView = view.findViewById(R.id.imageViewGif);
            fileNameView = view.findViewById(R.id.fileNameView);
            fileExtensionView = view.findViewById(R.id.fileExtensionView);
            fileDateView = view.findViewById(R.id.fileDateView);
            fileSizeView = view.findViewById(R.id.fileSizeView);
            filePositionView = view.findViewById(R.id.filePositionView);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_item, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        ZipEntryData entryData = zipEntries.get(position);

        Log.d("DB1", "ImageAdapter.onBindViewHolder: load file " + entryData.getFileName());

        InputStream inputStream = null;
        try {
            inputStream = getImageInputStream(entryData.getFileName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String fileExtension =  ZipUtilities.getFileExtension(entryData.getFileName());

        holder.fileExtensionView.setText(fileExtension);
        holder.fileNameView.setText(entryData.getFileName());
        holder.fileDateView.setText(entryData.getDisplayDateTime());
        holder.fileSizeView.setText(entryData.getDisplaySize());
        holder.filePositionView.setText(MessageFormat.format("{0}/{1}", (position + 1), zipEntries.size()));

        Glide.with(holder.photoView.getContext()).clear(holder.photoView);

        if(!fileExtension.equals("GIF")) {
            Log.d("DB1", "ImageAdapter.onBindViewHolder: use PhotoView");
            holder.photoView.setVisibility(View.VISIBLE);
            holder.imageView.setVisibility(View.INVISIBLE);
            Glide.with(holder.photoView.getContext())
                    .load(inputStream)
                    .listener(new GlideRequestListener())
                    .error(R.drawable.no_image)
                    .signature(new ObjectKey(UUID.randomUUID().toString())) // use a signature and keep using cache without the mix-ups
                    .into(holder.photoView);
        } else {
            Log.d("DB1", "ImageAdapter.onBindViewHolder: use ImageView");
            holder.photoView.setVisibility(View.INVISIBLE);
            holder.imageView.setVisibility(View.VISIBLE);
            Glide.with(holder.photoView.getContext())
                    .asGif()
                    .load(inputStream)
                    .listener(new GlideGifRequestListener())
                    .error(R.drawable.no_image)
                    .signature(new ObjectKey(UUID.randomUUID().toString())) // use a signature and keep using cache without the mix-ups
                    .into(holder.imageView);
        }
    }

    @Override
    public int getItemCount() {
        return zipEntries.size();
    }

    public InputStream getImageInputStream(String fileName) throws IOException, ZipException {

        ZipFile zipFile = new ZipFile(new File(ZipApplication.getAppContext().getFilesDir(), this.target), this.zipkey.toCharArray());
        FileHeader fileHeader = null;

        // Get the file header for the file you want
        try {
            fileHeader = zipFile.getFileHeader(fileName);
        } catch(Exception e) {
            Log.d("DB1", e.getMessage());
        }

        if (fileHeader != null) {
            Log.d("DB1", fileHeader.getFileName());
        }

        // Create an InputStream from the zip entry
        return zipFile.getInputStream(fileHeader);
    }

    private static class GlideGifRequestListener implements RequestListener<GifDrawable> {
        public GlideGifRequestListener() {
        }

        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<GifDrawable> target, boolean isFirstResource) {
            return false;
        }

        @Override
        public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target, DataSource dataSource, boolean isFirstResource) {
            return false;
        }
    }

    private static class GlideRequestListener implements RequestListener<Drawable> {
        public GlideRequestListener() {
        }

        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            return false; // Let Glide handle it further
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            return false; // Let Glide set the image
        }
    }
}