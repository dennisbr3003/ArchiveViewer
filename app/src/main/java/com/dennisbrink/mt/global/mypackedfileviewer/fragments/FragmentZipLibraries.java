package com.dennisbrink.mt.global.mypackedfileviewer.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.dennisbrink.mt.global.mypackedfileviewer.IZipApplication;
import com.dennisbrink.mt.global.mypackedfileviewer.IZipLibraryActivityListener;
import com.dennisbrink.mt.global.mypackedfileviewer.R;
import com.dennisbrink.mt.global.mypackedfileviewer.Receiver;
import com.dennisbrink.mt.global.mypackedfileviewer.libraries.ThumbnailCache;
import com.dennisbrink.mt.global.mypackedfileviewer.ZipApplication;
import com.dennisbrink.mt.global.mypackedfileviewer.libraries.ZipDialogs;
import com.dennisbrink.mt.global.mypackedfileviewer.ZipLibraryAdapter;

public class FragmentZipLibraries extends Fragment implements IZipApplication, IZipLibraryActivityListener {

    private ZipLibraryAdapter adapter;
    TextView statusBar;
    Receiver receiver = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_zip_libraries, container, false);

        ImageButton buttonClose = view.findViewById(R.id.buttonClose);
        buttonClose.setOnClickListener(v -> {
            requireActivity().finishAffinity(); // Close all the activities and close the app
        });

        ImageButton buttonClearAll = view.findViewById(R.id.buttonClearAll);
        buttonClearAll.setOnClickListener(v -> {
            ZipDialogs.createAndShowDialog(
                    v.getContext(),
                    getString(R.string.confirm_deletion_title),
                    getString(R.string.textmessage_delete_app_data),
                    DELETE_APP_DATA
            );
        });

        ImageButton buttonClearCache = view.findViewById(R.id.buttonClearCache);
        buttonClearCache.setOnClickListener(v -> {
            ZipDialogs.createAndShowDialog(
                    v.getContext(),
                    getString(R.string.confirm_deletion_title),
                    getString(R.string.textmessage_delete_cached_data),
                    DELETE_CACHED_DATA
            );
        });

        ImageButton buttonClearSavedData = view.findViewById(R.id.buttonClearSavedData);
        buttonClearSavedData.setOnClickListener(v -> {
            ZipDialogs.createAndShowDialog(
                    v.getContext(),
                    getString(R.string.confirm_deletion_title),
                    getString(R.string.textmessage_delete_extra_data),
                    DELETE_EXTRA_DATA
            );
        });

        ImageButton buttonClearTouchPoints = view.findViewById(R.id.buttonClearTouchpoints);
        buttonClearTouchPoints.setOnClickListener(v -> {
            ZipDialogs.createAndShowDialog(
                    v.getContext(),
                    getString(R.string.confirm_deletion_title),
                    getString(R.string.textmessage_delete_touch_point_sequence),
                    DELETE_TOUCH_POINTS
            );
        });

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewZipLibrary);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));

        statusBar = view.findViewById(R.id.statusBar);

        adapter = new ZipLibraryAdapter(requireActivity());
        recyclerView.setAdapter(adapter);

        setStatusBarText();

        return view;
    }

    public void setStatusBarText() {
        @SuppressLint("DefaultLocale") String text = String.format(
            "%s %d %s %s",
            getString(R.string.libraries),
            ZipApplication.getLibraries().size(),
            getString(R.string.size),
            ZipApplication.getZipLibraries().getTotalFileSize()
        );
        statusBar.setText(text);
    }

    // registering this activity for use of broadcast-receiver
    private IntentFilter getFilter(){
        IntentFilter intentFilter = new IntentFilter();
        Log.d("DB1", "FragmentZipLibraries.getFilter: Registering for broadcast actions (4)");
        intentFilter.addAction(DELETE_TOUCH_POINTS);
        intentFilter.addAction(DELETE_APP_DATA);
        intentFilter.addAction(DELETE_EXTRA_DATA);
        intentFilter.addAction(DELETE_CACHED_DATA);
        return intentFilter;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (receiver != null){
            Log.d("DB1", "FragmentZipLibraries.onPause: Unregistering receiver");
            requireActivity().unregisterReceiver(receiver);
            receiver = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onStart() {
        super.onStart();
        if(receiver == null){
            Log.d("DB1", "FragmentZipLibraries.onResume: Registering receiver");
            receiver = new Receiver();
            // Make yourself known to the receiver. This way the receiver will know this activity exists
            // and will be able to run any of the methods connected to the actions registered
            // This uses polymorphism --> the class is of type IZipLibraryActivityListener
            receiver.setZipLibraryActivity(this);
        }
        requireActivity().registerReceiver(receiver, getFilter(), Context.RECEIVER_EXPORTED);
    }

    @Override
    public void clearTouchPointSequence() {
        // this is fired from the receiver and only if the "yes" button is hit in the dialog
        ThumbnailCache thumbnailCache = new ThumbnailCache();
        thumbnailCache.clearCacheFolder(COORDINATE_DIR); // no effect on thumbnails --> entry code sequence is deleted
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void clearAllAppData() {
        // this is fired from the receiver and only if the "yes" button is hit in the dialog
        ThumbnailCache thumbnailCache = new ThumbnailCache();
        thumbnailCache.clearAll();
        adapter.notifyDataSetChanged(); // this is correct in this case -->  all copied data is deleted
    }

    @Override
    public void clearExtraData() {
        // this is fired from the receiver and only if the "yes" button is hit in the dialog
        ThumbnailCache thumbnailCache = new ThumbnailCache();
        thumbnailCache.clearCacheFolder(FILE_EXTRA_DIR); // no effect on thumbnails --> alle extra data is cleared
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void clearCachedData() {
        // this is fired from the receiver and only if the "yes" button is hit in the dialog
        ThumbnailCache thumbnailCache = new ThumbnailCache();
        thumbnailCache.clearCacheFolder(CACHE_DIR);
        adapter.notifyDataSetChanged(); // this is correct in this case -->  all thumbnails are cleared
    }

}