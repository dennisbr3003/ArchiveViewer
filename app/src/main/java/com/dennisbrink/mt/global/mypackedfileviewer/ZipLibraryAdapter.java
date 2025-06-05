package com.dennisbrink.mt.global.mypackedfileviewer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ZipLibraryAdapter extends RecyclerView.Adapter<ZipLibraryAdapter.LibraryViewHolder> {

    private final ActivityResultLauncher<Intent> launcher;
    ThumbnailCache thumbnailCache = new ThumbnailCache();

    public ZipLibraryAdapter(ActivityResultLauncher<Intent> launcher) {
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
        try {

            Log.d("DB1", "Library onBindViewHolder 1");

            ZipLibrary library = ZipApplication.getLibraries().get(position);
            holder.nameTextView.setText(library.getName());

            Log.d("DB1", "Library onBindViewHolder 2");

            try {
                ZipUtilities.initAssetManager();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Log.d("DB1", "Start getting extra data");
            ZipLibraryExtraData zipLibraryExtraData = ZipUtilities.getZipLibraryExtraData(library.getTarget(), library.getSource(), library.getZipkey());

            holder.libraryError.setVisibility(View.GONE);
            holder.libraryWarning.setVisibility(View.GONE);
            holder.sourceTextView.setVisibility(View.VISIBLE);

            // check if there is a thumbnail
            if (thumbnailCache.isThumbnailCached("cache_" + library.getSource().hashCode())) {
                holder.libraryImageView.setImageBitmap(thumbnailCache.loadThumbnail("cache_" + library.getSource().hashCode()));
            } else { // otherwise use the placeholder
                holder.libraryImageView.setImageResource(R.drawable.archive);
            }

            holder.libraryStateImageview.setImageResource(R.drawable.zipfile_ok);

            if (!zipLibraryExtraData.getIsCopied() && zipLibraryExtraData.getInAssets()) {
                holder.sourceTextView.setVisibility(View.GONE);
                holder.libraryError.setVisibility(View.GONE);
                holder.libraryWarning.setVisibility(View.VISIBLE);
                holder.libraryWarning.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.orange));
                holder.libraryWarning.setText(zipLibraryExtraData.getWarningMessage());
            }
            if (!zipLibraryExtraData.getInAssets()) {
                holder.sourceTextView.setVisibility(View.GONE);
                holder.libraryWarning.setVisibility(View.GONE);
                holder.libraryError.setVisibility(View.VISIBLE);
                holder.libraryError.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.red));
                holder.libraryError.setText(zipLibraryExtraData.getErrorMessage());
                holder.libraryStateImageview.setImageResource(R.drawable.zipfile_error);
            }
            if (!zipLibraryExtraData.getValidZip() && zipLibraryExtraData.getIsCopied()) {
                holder.sourceTextView.setVisibility(View.GONE);
                holder.libraryWarning.setVisibility(View.GONE);
                holder.libraryError.setVisibility(View.VISIBLE);
                holder.libraryError.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.red));
                holder.libraryError.setText(zipLibraryExtraData.getErrorMessage());
                holder.libraryStateImageview.setImageResource(R.drawable.zipfile_error);
            }

            switch(zipLibraryExtraData.getLockState()) {
                case LOCKED_PASSWORD:
                    holder.lockStateImageView.setImageResource(R.drawable.lockstate_unlocked);
                    break;
                case LOCKED_NO_PASSWORD:
                    holder.lockStateImageView.setImageResource(R.drawable.lockstate_locked);
                    break;
                case NOT_LOCKED:
                    holder.lockStateImageView.setImageResource(R.drawable.lockstate_nolockstate);
                    break;
                case LOCKED_CORRUPTED:
                    holder.lockStateImageView.setImageResource(R.drawable.lockstate_locked);
                    holder.passwordWarningImageView.setVisibility(View.VISIBLE);
                    break;
                default:
                     holder.lockStateImageView.setImageResource(R.drawable.lockstate_unknown);
                     break;
            }

//            if(zipLibraryExtraData.getIsCopied() && zipLibraryExtraData.getValidZip()) {
//
//            }

            holder.fileSizeTextView.setText(zipLibraryExtraData.getFileSize());
            if (zipLibraryExtraData.getInAssets() && zipLibraryExtraData.getIsCopied()) {
                holder.sourceTextView.setText(zipLibraryExtraData.getFileDate());
                holder.fileCountTextView.setText(zipLibraryExtraData.getNumFiles());
            } else {
                holder.fileCountTextView.setText("");
            }

            // add a click listener to the holder
            holder.itemView.setOnClickListener(v -> {

                Log.d("DB1", "clicked on library");
                Log.d("DB1", "zip copied locally " + zipLibraryExtraData.getIsCopied());
                Log.d("DB1", "valid zip " + zipLibraryExtraData.getValidZip());
                Log.d("DB1", "valid zip in assets " + zipLibraryExtraData.getInAssets());

                // block click on invalid and not existing archives
                if((!zipLibraryExtraData.getValidZip() && zipLibraryExtraData.getIsCopied()) || !zipLibraryExtraData.getInAssets()) return;

                // what to do if there is no file copied and no password. we need to copy it and check
                // if the file is encrypted. If it is, show the dialog, if it's not open it because there is no password.
                // also we need to check if the password is correct (also if the password is already known). We should always
                // copy the file here if it is not already

                boolean showDialog = false;

                // 1. copy file here
                // 2. check zip file vitals
                // 3. decide if we need to show the dialog

                if (ZipUtilities.copyZipFromAsset(ZipApplication.getLibraries().get(position).getTarget(),
                    ZipApplication.getLibraries().get(position).getSource())) {
                    File tempFile = new File(ZipApplication.getAppContext().getFilesDir(), ZipApplication.getLibraries().get(position).getTarget());
                    try(ZipFile zipFile = new ZipFile(tempFile)){
                        if(zipFile.isValidZipFile()) {
                            if (zipFile.isEncrypted()) {
                                // encrypted library but no password so we need user input here
                                if(ZipApplication.getLibraries().get(position).getZipkey().isEmpty()) showDialog = true; // valid zip file no password
                                else {
                                    // try the password, if it is ok then we move on else we do not and ask the correct one
                                    // password not correct
                                    showDialog = !ZipUtilities.isValidZipPassword(ZipApplication.getLibraries().get(position).getTarget(),
                                                                                  ZipApplication.getLibraries().get(position).getSource(),
                                                                                  ZipApplication.getLibraries().get(position).getZipkey()); // password valid?
                                }
                            }
                        }
                    } catch (IOException e) {
                        showDialog = false; // zip file is not valid
                    }
                }

//                if (zipLibraryExtraData.getLockState().equals(LockStatus.LOCKED_CORRUPTED) ||
//                    zipLibraryExtraData.getLockState().equals(LockStatus.LOCKED_NO_PASSWORD)) {
                if(showDialog){
                    showPasswordDialog(v.getContext(), position);
                } else {
                    handleZipOpening(v.getContext(), library.getZipkey(), position);
                }
            });

        }catch (Exception e) {
            Log.d("DB1", "Error: " + e.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return ZipApplication.getLibraries().size();
    }

    public void handleZipOpening(Context context, String password, int position) {

        Log.d("DB1", "password " + password + " position " + position);

        if (password != null && !password.isEmpty()) {
            ZipApplication.getLibraries().get(position).setZipkey(password);
            notifyItemChanged(position);
        } else {
            // do not start the detail view
            return;
        }

        Intent intent = new Intent(context, ZipFileActivity.class);
        intent.putExtra("source", ZipApplication.getLibraries().get(position).getSource());
        intent.putExtra("target", ZipApplication.getLibraries().get(position).getTarget());
        intent.putExtra("name", ZipApplication.getLibraries().get(position).getName());
        intent.putExtra("zipkey", ZipApplication.getLibraries().get(position).getZipkey());
        intent.putExtra("position", position);

        try {
            // context.startActivity(intent);
            launcher.launch(intent);
        } catch (Exception e) {
            Log.d("DB1", "Error opening ZipFileActivity: " + e.getMessage());
        }
    }

    public void showPasswordDialog(Context context, int position) {

        // Inflate the custom layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dlg_password, null);

        // Find the EditText in the inflated layout
        EditText passwordInput = dialogView.findViewById(R.id.passwordInput);
        TextView textViewError = dialogView.findViewById(R.id.textViewError);

        // Create and display the dialog
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(false)  // Prevent dismiss on clicking outside
                .setPositiveButton("OK", null)  // Temporarily set null
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();

        dialog.show();

        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                textViewError.setText(null);  // Hide the error message
            }
        });

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

            File tempFile = new File(ZipApplication.getAppContext().getFilesDir(), ZipApplication.getLibraries().get(position).getTarget());
            if(!tempFile.exists()) {
                try (InputStream inputStream = context.getAssets().open(ZipApplication.getLibraries().get(position).getSource());
                     FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                }
                catch (Exception e){
                    textViewError.setText(R.string.zip_not_copied_files);
                    return;
                }
            }

            String password = passwordInput.getText().toString();

            if (tempFile.exists()) {
                try (ZipFile zipFile = new ZipFile(tempFile, password.toCharArray())) {
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
                    textViewError.setText(R.string.password_incorrect);
                    return;
                }
            }

            dialog.dismiss();  // Dismiss the dialog on successful validation
            handleZipOpening(context, password, position);
        });

    }

    public static class LibraryViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, sourceTextView, fileSizeTextView, fileCountTextView, libraryError, libraryWarning;
        ImageView libraryImageView, lockStateImageView, libraryStateImageview, passwordWarningImageView;

        public LibraryViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.libraryName);
            sourceTextView = itemView.findViewById(R.id.libraryCopied);
            fileSizeTextView = itemView.findViewById(R.id.fileSize);
            fileCountTextView = itemView.findViewById(R.id.fileCount);
            libraryError = itemView.findViewById(R.id.LibraryError);
            libraryWarning = itemView.findViewById(R.id.libraryWarning);
            libraryImageView = itemView.findViewById(R.id.libraryImageView);
            lockStateImageView = itemView.findViewById(R.id.imgLockStatus);
            libraryStateImageview = itemView.findViewById(R.id.imgLibraryState);
            passwordWarningImageView = itemView.findViewById(R.id.imgPasswordWarning);

        }
    }

}