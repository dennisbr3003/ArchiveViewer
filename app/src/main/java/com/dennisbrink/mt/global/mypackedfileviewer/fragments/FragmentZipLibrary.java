package com.dennisbrink.mt.global.mypackedfileviewer.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.dennisbrink.mt.global.mypackedfileviewer.IZipApplication;
import com.dennisbrink.mt.global.mypackedfileviewer.R;
import com.dennisbrink.mt.global.mypackedfileviewer.structures.ZipEntryData;
import com.dennisbrink.mt.global.mypackedfileviewer.ZipFileAdapter;
import com.dennisbrink.mt.global.mypackedfileviewer.libraries.ZipUtilities;

import java.util.List;
import java.util.Objects;


public class FragmentZipLibrary extends Fragment implements IZipApplication {

    ZipFileAdapter adapter;
    List<ZipEntryData> zipContent;

    public static FragmentZipLibrary newInstance(String source, String target, String name, String zipkey, int position) {
        FragmentZipLibrary fragment = new FragmentZipLibrary();
        Bundle args = new Bundle();
        args.putString("source", source);
        args.putString("target", target);
        args.putString("name", name);
        args.putString("zipkey", zipkey);
        args.putInt("position", position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_zip_library, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewZipLibraryFiles);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));

        Bundle args = getArguments();
        assert args != null;

        String source = args.getString("source");
        String target = args.getString("target");
        String name = args.getString("name");
        String zipkey = args.getString("zipkey");

        int position = args.getInt("position", -1);

        Log.d("DB1", "FragmentZipLibrary.onCreateView - position after opening fragment: " + position);
        try {
            // Example usage of the collected data
            TextView targetTextView = view.findViewById(R.id.targetTextView);
            TextView numEntriesTextView = view.findViewById(R.id.targetNumEntries);

            targetTextView.setText(name);

            zipContent = ZipUtilities.getZipContentsFromAsset(source, target, zipkey);

            String summary = getString(
                    R.string.file_summary,
                    zipContent.size(),
                    ZipUtilities.convertBytesToKilobytes(ZipUtilities.getZipLibrarySize(target, source))
            );
            numEntriesTextView.setText(summary);

            ImageButton buttonUp = view.findViewById(R.id.imageButtonUp);
            buttonUp.setOnClickListener(v -> smoothScrollToPosition(recyclerView, requireActivity(), 0));

            ImageButton buttonDown = view.findViewById(R.id.imageButtonDown);
            buttonDown.setOnClickListener(v -> smoothScrollToPosition(recyclerView, requireActivity(), zipContent.size() - 1));

            adapter = new ZipFileAdapter(zipContent, position);
            recyclerView.setAdapter(adapter);

        } catch (Exception e){
            Log.d("DB1", "FragmentZipLibrary.onCreateView - Error after opening fragment: " + e.getMessage());
        }
        return view;
    }

    public void smoothScrollToPosition(RecyclerView recyclerView, Context context, int position) {
        RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(context) {
            @Override
            protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_START;
            }
        };
        smoothScroller.setTargetPosition(position);
        Objects.requireNonNull(recyclerView.getLayoutManager()).startSmoothScroll(smoothScroller);
    }
}