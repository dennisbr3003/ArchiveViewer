package com.dennisbrink.mt.global.mypackedfileviewer;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.dennisbrink.mt.global.mypackedfileviewer.events.OpenZipLibraryEvent;
import com.dennisbrink.mt.global.mypackedfileviewer.libraries.ThumbnailCache;
import com.dennisbrink.mt.global.mypackedfileviewer.libraries.ZipUtilities;
import com.dennisbrink.mt.global.mypackedfileviewer.structures.ZipLibrary;
import com.dennisbrink.mt.global.mypackedfileviewer.structures.ZipLibraryExtraData;

import net.lingala.zip4j.ZipFile;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class ZipLibraryAdapter extends RecyclerView.Adapter<ZipLibraryAdapter.LibraryViewHolder> implements IZipApplication {

    ThumbnailCache thumbnailCache = new ThumbnailCache();
    private boolean blockClickListener = false;
    public ZipLibraryAdapter() {}
    ZipLibraryExtraData zipLibraryExtraData;
    ZipLibrary library;
    @NonNull
    @Override
    public LibraryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.zip_library_item, parent, false);
        return new LibraryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LibraryViewHolder holder, int position) {

        // Atomic because it is being updates in a handler
        AtomicReference<Boolean> zipFileIsEncrypted = new AtomicReference<>(false);

        try {

            library = ZipApplication.getLibraries().get(position);
            holder.nameTextView.setText(library.getName());

            try {
                ZipUtilities.initAssetManager();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // gather (and save) all possible asset data without actually opening the file
            zipLibraryExtraData = ZipUtilities.getZipLibraryExtraData(library.getTarget(), library.getSource(), library.getZipkey(), position);

            holder.libraryError.setVisibility(View.GONE);
            holder.libraryWarning.setVisibility(View.GONE);
            holder.sourceTextView.setVisibility(View.VISIBLE);

            // check for a thumbnail. If found load it, else load the placeholder
            if (thumbnailCache.isThumbnailCached("", "cache_" + library.getSource().hashCode())) {
                holder.libraryImageView.setImageBitmap(thumbnailCache.loadThumbnail("", "cache_" + library.getSource().hashCode()));
            } else { // otherwise use the placeholder
                holder.libraryImageView.setImageResource(R.drawable.archive);
            }
            holder.libraryStateImageview.setImageResource(R.drawable.lib_ok);

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
                holder.libraryStateImageview.setImageResource(R.drawable.lib_error);
            }
            if (!zipLibraryExtraData.getValidZip() && zipLibraryExtraData.getIsCopied()) {
                holder.sourceTextView.setVisibility(View.GONE);
                holder.libraryWarning.setVisibility(View.GONE);
                holder.libraryError.setVisibility(View.VISIBLE);
                holder.libraryError.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.red));
                holder.libraryError.setText(zipLibraryExtraData.getErrorMessage());
                holder.libraryStateImageview.setImageResource(R.drawable.lib_error);
            }

            switch (zipLibraryExtraData.getLockState()) {
                case LOCKED_PASSWORD:
                    holder.lockStateImageView.setImageResource(R.drawable.unlocked);
                    break;
                case LOCKED_NO_PASSWORD:
                    holder.lockStateImageView.setImageResource(R.drawable.locked);
                    break;
                case NOT_LOCKED:
                    holder.lockStateImageView.setImageResource(R.drawable.nolock);
                    break;
                case LOCKED_CORRUPTED:
                    holder.lockStateImageView.setImageResource(R.drawable.locked);
                    holder.passwordWarningImageView.setVisibility(View.VISIBLE);
                    break;
                default:
                    holder.lockStateImageView.setImageResource(R.drawable.unknown);
                    break;
            }

            holder.fileSizeTextView.setText(zipLibraryExtraData.getFileSize());
            if (zipLibraryExtraData.getInAssets() && zipLibraryExtraData.getIsCopied()) {
                holder.sourceTextView.setText(zipLibraryExtraData.getFileDate());
                holder.fileCountTextView.setText(zipLibraryExtraData.getNumFiles());
            } else {
                holder.fileCountTextView.setText("");
            }

            // add a click listener to the holder
            holder.itemView.setOnClickListener(v -> {

                if (blockClickListener) return;

                // load the file that we saved with the extra library data
                ZipLibraryExtraData zipLibraryExtraDataHolder = ZipUtilities.getZipLibraryExtraDataObjectFromFile(ZipApplication.getLibraries().get(position).getTarget());

                // block click on invalid and not existing assets
                if (!zipLibraryExtraDataHolder.getValidZip() && zipLibraryExtraDataHolder.getIsCopied()) return;
                if (!zipLibraryExtraDataHolder.getInAssets()) return;

                Log.d("DB1", "ZipLibraryAdapter.onBindViewHolder - Current lock state for library " + ZipApplication.getLibraries().get(position).getName() + " = " + zipLibraryExtraDataHolder.getLockState());

                // what to do if there is no file copied and no password. we need to copy it and check
                // if the file is encrypted. If it is, show the dialog, if it's not open it because there is no password.
                // also we need to check if the password is correct (also if the password is already known). We should always
                // copy the file here if it is not already
                blockClickListener = true;

                // show the infinite progress bar
                if (holder.progressBarLib != null) {
                    if (holder.progressBarLib.getVisibility() == View.INVISIBLE) {
                        holder.progressBarLib.setVisibility(View.VISIBLE);
                    } else {
                        holder.progressBarLib.setVisibility(View.INVISIBLE);
                    }
                }

                // 1. copy file here
                // 2. check zip file vitals
                // 3. decide if we need to show the dialog

                ExecutorService executor = Executors.newSingleThreadExecutor();

                executor.execute(() -> {

                    boolean showDialog = false;

                    if (ZipUtilities.copyZipFromAsset(ZipApplication.getLibraries().get(position).getTarget(),
                            ZipApplication.getLibraries().get(position).getSource())) {

                        File tempFile = new File(ZipApplication.getAppContext().getFilesDir(),
                                ZipApplication.getLibraries().get(position).getTarget());

                        try (ZipFile zipFile = new ZipFile(tempFile)) {
                            if (zipFile.isValidZipFile()) {
                                if (zipFile.isEncrypted()) {
                                    zipFileIsEncrypted.set(true);
                                    if (ZipApplication.getLibraries().get(position).getZipkey().isEmpty())
                                        showDialog = true;
                                    else {
                                        if(zipLibraryExtraDataHolder.getLockState() != ELockStatus.LOCKED_PASSWORD){
                                            Log.d("DB1", "ZipLibraryAdapter.onBindViewHolder.executor.execute - verifying lock state using configured password");
                                            showDialog = !ZipUtilities.isValidZipPassword(
                                                    ZipApplication.getLibraries().get(position).getTarget(),
                                                    ZipApplication.getLibraries().get(position).getZipkey());
                                        }
                                        if (showDialog) {
                                            try {
                                                zipLibraryExtraDataHolder.setLockState(ELockStatus.LOCKED_CORRUPTED);
                                                Log.d("DB1", "ZipLibraryAdapter.onBindViewHolder.executor.execute - Target ED_" + ZipApplication.getLibraries().get(position).getTarget());
                                                ZipUtilities.saveZipLibraryExtraDataObjectToFile(ZipApplication.getLibraries().get(position).getTarget(), zipLibraryExtraDataHolder);
                                                // we are not on the main thread here and UI manipulation must be done on the main thread.
                                                // So we need a handler to get to the main thread and notify the adapter to update itself.
                                                new Handler(Looper.getMainLooper()).post(() -> notifyItemChanged(position));
                                            } catch (Exception e) {
                                                Log.d("DB1", "ZipLibraryAdapter.onBindViewHolder.executor.execute - Error: " + e.getMessage());
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (IOException e) {
                            showDialog = false;
                        }
                    }

                    boolean finalShowDialog = showDialog;

                    // in order to reach the main thread we need this handler
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (finalShowDialog) {
                            showPasswordDialog(v.getContext(), position, holder);
                            if (holder.progressBarLib != null) {
                                holder.progressBarLib.setVisibility(holder.progressBarLib.getVisibility() == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
                            }
                        } else {
                            handleZipOpening(library.getZipkey(), position, holder, zipFileIsEncrypted.get());
                        }
                    });

                });

                // Shut down the executor service appropriately, depending on your use case
                executor.shutdown();

            });
        }catch (Exception e) {
            Log.d("DB1", "ZipLibraryAdapter.onBindViewHolder - Error: " + e.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return ZipApplication.getLibraries().size();
    }

    public void handleZipOpening(String password, int position, LibraryViewHolder holder, Boolean zipFileIsEncrypted ) {

        if ((password != null && !password.isEmpty()) || !zipFileIsEncrypted) {
            ZipApplication.getLibraries().get(position).setZipkey(password);
            notifyItemChanged(position);
        } else {
            // do not start the detail view
            blockClickListener = false;
            if (holder.progressBarLib != null) {
                holder.progressBarLib.setVisibility(View.INVISIBLE);
            }
            return;
        }

        EventBus.getDefault().post(new OpenZipLibraryEvent(position));

        blockClickListener=false;

        if (holder.progressBarLib != null) {
            holder.progressBarLib.setVisibility(View.INVISIBLE);
        }

    }

    public void showPasswordDialog(Context context, int position, LibraryViewHolder holder) {

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
                .setNegativeButton("Cancel",null) // Temporarily set null
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

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(view -> {
            blockClickListener = false;
            dialog.dismiss();
        });

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

            // copy file from assets to files dir if needed
            ZipUtilities.copyZipFromAsset(ZipApplication.getLibraries().get(position).getTarget(), ZipApplication.getLibraries().get(position).getSource());

            String password = passwordInput.getText().toString();

            // verify the password
            if(!ZipUtilities.isValidZipPassword(ZipApplication.getLibraries().get(position).getTarget(), password )){
                textViewError.setText(R.string.password_incorrect); // password not valid?
                return;
            }
            dialog.dismiss();  // Dismiss the dialog on successful validation
            handleZipOpening(password, position, holder, true);
        });
    }

    public static class LibraryViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, sourceTextView, fileSizeTextView, fileCountTextView, libraryError, libraryWarning;
        ImageView libraryImageView, lockStateImageView, libraryStateImageview, passwordWarningImageView;
        ProgressBar progressBarLib;

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
            progressBarLib = itemView.findViewById(R.id.progressBarLib);

        }
    }

}