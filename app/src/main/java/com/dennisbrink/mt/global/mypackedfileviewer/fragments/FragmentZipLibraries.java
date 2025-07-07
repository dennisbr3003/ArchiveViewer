package com.dennisbrink.mt.global.mypackedfileviewer.fragments;

import android.annotation.SuppressLint;
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
import com.dennisbrink.mt.global.mypackedfileviewer.R;
import com.dennisbrink.mt.global.mypackedfileviewer.events.DialogResultActionEvent;
import com.dennisbrink.mt.global.mypackedfileviewer.events.UpdateLibraryLockState;
import com.dennisbrink.mt.global.mypackedfileviewer.libraries.ThumbnailCache;
import com.dennisbrink.mt.global.mypackedfileviewer.ZipApplication;
import com.dennisbrink.mt.global.mypackedfileviewer.libraries.ZipDialogs;
import com.dennisbrink.mt.global.mypackedfileviewer.ZipLibraryAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class FragmentZipLibraries extends Fragment implements IZipApplication {

    private ZipLibraryAdapter adapter;
    TextView statusBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_zip_libraries, container, false);

        ImageButton buttonClearAll = view.findViewById(R.id.buttonClearAll);
        buttonClearAll.setOnClickListener(v -> ZipDialogs.createAndShowDialog(
                v.getContext(),
                getString(R.string.confirm_deletion_title),
                getString(R.string.textmessage_delete_app_data),
                DELETE_APP_DATA
        ));

        ImageButton buttonClearCache = view.findViewById(R.id.buttonClearCache);
        buttonClearCache.setOnClickListener(v -> ZipDialogs.createAndShowDialog(
                v.getContext(),
                getString(R.string.confirm_deletion_title),
                getString(R.string.textmessage_delete_cached_data),
                DELETE_CACHED_DATA
        ));

        ImageButton buttonClearSavedData = view.findViewById(R.id.buttonClearSavedData);
        buttonClearSavedData.setOnClickListener(v -> ZipDialogs.createAndShowDialog(
                v.getContext(),
                getString(R.string.confirm_deletion_title),
                getString(R.string.textmessage_delete_extra_data),
                DELETE_EXTRA_DATA
        ));

        ImageButton buttonClearTouchPoints = view.findViewById(R.id.buttonClearTouchpoints);
        buttonClearTouchPoints.setOnClickListener(v -> ZipDialogs.createAndShowDialog(
                v.getContext(),
                getString(R.string.confirm_deletion_title),
                getString(R.string.textmessage_delete_touch_point_sequence),
                DELETE_TOUCH_POINTS
        ));

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewZipLibrary);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));

        statusBar = view.findViewById(R.id.statusBar);

        adapter = new ZipLibraryAdapter();
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

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onUpdateLibraryLockState(UpdateLibraryLockState event) {
        // Handle the event
        // Update your data or UI
        Log.d("DB1", "FragmentZipLibraries.onUpdateLibraryLockState: Event captured, execute logic position " + event.position);
        adapter.notifyItemChanged(event.position); // This causes an endless loop

        EventBus.getDefault().removeStickyEvent(event);
    }

    // Use @Subscribe to handle the Event
    @SuppressLint("NotifyDataSetChanged")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDialogResultActionEvent(DialogResultActionEvent event) {
        Log.d("DB1", "FragmentZipLibraries.onDialogResultActionEvent: Event captured, execute logic for action " + event.action);

        ThumbnailCache thumbnailCache = new ThumbnailCache();

        switch(event.action){
            case DELETE_TOUCH_POINTS:
                thumbnailCache.clearCacheFolder(COORDINATE_DIR);
                break;
            case DELETE_APP_DATA:
                thumbnailCache.clearAll();
                adapter.notifyDataSetChanged(); // this is correct in this case -->  all copied data is deleted
                break;
            case DELETE_EXTRA_DATA:
                thumbnailCache.clearCacheFolder(FILE_EXTRA_DIR);
                break;
            case DELETE_CACHED_DATA:
                thumbnailCache.clearCacheFolder(CACHE_DIR);
                adapter.notifyDataSetChanged(); // this is correct in this case -->  all thumbnails are cleared
                break;
        }
    }
}