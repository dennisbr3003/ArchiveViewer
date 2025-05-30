package com.dennisbrink.mt.global.mypackedfileviewer;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class ZipLibraryAdapter extends RecyclerView.Adapter<ZipLibraryAdapter.LibraryViewHolder> {

    private final List<ZipLibrary> libraries;
    private final ActivityResultLauncher<Intent> launcher;
    ThumbnailCache thumbnailCache = new ThumbnailCache();

    public ZipLibraryAdapter(List<ZipLibrary> libraries, ActivityResultLauncher<Intent> launcher) {
        this.libraries = libraries;
        this.launcher = launcher;
    }

    @NonNull
    @Override
    public LibraryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.zip_library_item, parent, false);
        return new LibraryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LibraryViewHolder holder, int position) {
        ZipLibrary library = libraries.get(position);
        holder.nameTextView.setText(library.getName());

        try {
            ZipUtilities.initAssetManager();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ZipLibraryExtraData zipLibraryExtraData = ZipUtilities.getFileData(library.getTarget(), library.getSource(), library.getZipkey());

        holder.libraryError.setVisibility(View.GONE);
        holder.libraryWarning.setVisibility(View.GONE);
        holder.sourceTextView.setVisibility(View.VISIBLE);

        // check if there is a thumbnail
        if (thumbnailCache.isThumbnailCached("cache_" + library.getSource().hashCode())) {
            holder.libraryImageView.setImageBitmap(thumbnailCache.loadThumbnail("cache_" + library.getSource().hashCode()));
        } else { // otherwise use the placeholder
            holder.libraryImageView.setImageResource(R.drawable.archive);
        }


        if(!zipLibraryExtraData.getIsCopied() && zipLibraryExtraData.getInAssets()){
            holder.sourceTextView.setVisibility(View.GONE);
            holder.libraryError.setVisibility(View.GONE);
            holder.libraryWarning.setVisibility(View.VISIBLE);
            holder.libraryWarning.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.orange));
            holder.libraryWarning.setText(zipLibraryExtraData.getWarningMessage());
        }
        if(!zipLibraryExtraData.getInAssets()){
            holder.sourceTextView.setVisibility(View.GONE);
            holder.libraryWarning.setVisibility(View.GONE);
            holder.libraryError.setVisibility(View.VISIBLE);
            holder.libraryError.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.red));
            holder.libraryError.setText(zipLibraryExtraData.getErrorMessage());
        }

        holder.fileSizeTextView.setText(zipLibraryExtraData.getFileSize());
        if(zipLibraryExtraData.getInAssets() && zipLibraryExtraData.getIsCopied()) {
            holder.sourceTextView.setText(zipLibraryExtraData.getFileDate());
            holder.fileCountTextView.setText(zipLibraryExtraData.getNumFiles());
        } else {
            holder.fileCountTextView.setText("");
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ZipFileActivity.class);
            intent.putExtra("source", library.getSource());
            intent.putExtra("target", library.getTarget());
            intent.putExtra("name", library.getName());
            intent.putExtra("zipkey", library.getZipkey());
            intent.putExtra("position", position);
            try {
                launcher.launch(intent);
            } catch (Exception e) {
                Log.d("DB1", Objects.requireNonNull(e.getMessage()));
            }
        });

    }

    @Override
    public int getItemCount() {
        return libraries.size();
    }

    public static class LibraryViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, sourceTextView, fileSizeTextView, fileCountTextView, libraryError, libraryWarning;
        ImageView libraryImageView;

        public LibraryViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.libraryName);
            sourceTextView = itemView.findViewById(R.id.libraryCopied);
            fileSizeTextView = itemView.findViewById(R.id.fileSize);
            fileCountTextView = itemView.findViewById(R.id.fileCount);
            libraryError = itemView.findViewById(R.id.LibraryError);
            libraryWarning = itemView.findViewById(R.id.libraryWarning);
            libraryImageView = itemView.findViewById(R.id.libraryImageView);

        }
    }

}