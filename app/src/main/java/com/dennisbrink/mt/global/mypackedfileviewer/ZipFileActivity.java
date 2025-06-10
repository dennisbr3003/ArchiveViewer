package com.dennisbrink.mt.global.mypackedfileviewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Objects;


public class ZipFileActivity extends AppCompatActivity {

    private int position = -1;
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_zip_file);

        // Add a callback to handle the back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

                Intent resultIntent = new Intent(); // Prepare result data
                resultIntent.putExtra("position", position); // Put the position in the intent extras
                setResult(RESULT_OK, resultIntent); // Set the result with RESULT_OK or any appropriate result code

                finish(); // Finish the activity
            }
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerView2);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ImageButton buttonClose = findViewById(R.id.imageButtonClose);
        buttonClose.setOnClickListener(v -> {
            finishAffinity(); // Close all the activities and close the app
        });

        String source = getIntent().getStringExtra("source");
        String target = getIntent().getStringExtra("target");
        String name = getIntent().getStringExtra("name");
        String zipkey = getIntent().getStringExtra("zipkey");
        position = getIntent().getIntExtra("position", -1);

        Log.d("DB1", "position after opening activity: " + position);

        // Example usage of the collected data
        TextView targetTextView = findViewById(R.id.targetTextView);
        TextView numEntriesTextView = findViewById(R.id.targetNumEntries);

        targetTextView.setText(name);

        List<ZipEntryData> zipContent = ZipUtilities.getZipContentsFromAsset(source, target, zipkey);

        Log.d("DB1", String.valueOf(zipContent.size()));
        numEntriesTextView.setText(zipContent.size() + getString(R.string.files_comma) +  ZipUtilities.convertBytesToKilobytes(ZipUtilities.getZipLibrarySize(target, source)));

        ImageButton buttonUp = findViewById(R.id.imageButtonUp);
        buttonUp.setOnClickListener(v -> {
            smoothScrollToPosition(recyclerView, this, 0); // Close all the activities and close the app
        });

        ImageButton buttonDown = findViewById(R.id.imageButtonDown);
        buttonDown.setOnClickListener(v -> {
            smoothScrollToPosition(recyclerView, this, zipContent.size() - 1); // Close all the activities and close the app
        });

        ZipFileAdapter adapter = new ZipFileAdapter(zipContent, position -> {
            Intent intent = new Intent(ZipFileActivity.this, ZipContentViewerActivity.class);
            intent.putExtra("position", position);
            intent.putExtra("target", target);
            intent.putExtra("source", source);
            intent.putExtra("zipkey", zipkey);
            startActivity(intent);
        }, position);

        recyclerView.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main2), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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