package com.dennisbrink.mt.global.mypackedfileviewer.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dennisbrink.mt.global.mypackedfileviewer.IZipApplication;
import com.dennisbrink.mt.global.mypackedfileviewer.ImageAdapter;
import com.dennisbrink.mt.global.mypackedfileviewer.R;
import com.dennisbrink.mt.global.mypackedfileviewer.events.VideoThumbnailFinalEvent;
import com.dennisbrink.mt.global.mypackedfileviewer.structures.ZipEntryData;
import com.dennisbrink.mt.global.mypackedfileviewer.libraries.ZipUtilities;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class FragmentZipLibraryFile extends Fragment implements IZipApplication {

    private LinearLayoutManager layoutManager;
    private int startPosition = 0;
    private String source, target, zipkey;
    List<ZipEntryData> zipContent;
    ImageAdapter adapter;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_zip_library_files, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get arguments (pass these via newInstance or setArguments from activity/parent fragment)
        if (getArguments() != null) {
            startPosition = getArguments().getInt("position", 0);
            source = getArguments().getString("source");
            target = getArguments().getString("target");
            zipkey = getArguments().getString("zipkey");
        }

//        List<ZipEntryData>
        zipContent = ZipUtilities.getZipContentsFromAsset(source, target, zipkey);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView4);
        layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);

        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

//        ImageAdapter
        adapter = new ImageAdapter(zipContent, zipkey, target);
        recyclerView.setAdapter(adapter);

        Log.d("DB1", "FragmentZipLibraryFile.onViewCreated: Position " + startPosition);
        Log.d("DB1", "FragmentZipLibraryFile.onViewCreated: Source " + source);
        Log.d("DB1", "FragmentZipLibraryFile.onViewCreated: Target " + target);
        Log.d("DB1", "FragmentZipLibraryFile.onViewCreated: Zipkey " + zipkey);

        if (savedInstanceState != null) {
            int savedPosition = savedInstanceState.getInt("current_position");
            recyclerView.scrollToPosition(savedPosition);
        } else {
            recyclerView.scrollToPosition(startPosition);
        }

    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Get the current position from the LayoutManager
        int currentPosition = layoutManager.findFirstVisibleItemPosition();
        outState.putInt("current_position", currentPosition);
    }

    // Static factory method for creating the fragment and passing arguments
    public static FragmentZipLibraryFile newInstance(int position, String source, String target, String zipkey) {
        FragmentZipLibraryFile fragment = new FragmentZipLibraryFile();
        Bundle args = new Bundle();
        args.putInt("position", position);
        args.putString("source", source);
        args.putString("target", target);
        args.putString("zipkey", zipkey);
        fragment.setArguments(args);
        return fragment;
    }
}
