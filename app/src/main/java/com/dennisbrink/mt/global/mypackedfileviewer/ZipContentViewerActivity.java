package com.dennisbrink.mt.global.mypackedfileviewer;

import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ZipContentViewerActivity extends AppCompatActivity {
    private LinearLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_zip_content_viewer);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        int startPosition = getIntent().getIntExtra("position", 0);
        String source = getIntent().getStringExtra("source");
        String target = getIntent().getStringExtra("target");
        String zipkey = getIntent().getStringExtra("zipkey");

        List<ZipEntryData> zipContent = ZipUtilities.getZipContentsFromAsset(source, target, zipkey);

        RecyclerView recyclerView = findViewById(R.id.recyclerView4);

        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);

        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        ImageAdapter adapter = new ImageAdapter(zipContent, zipkey, target);
        recyclerView.setAdapter(adapter);

        Log.d("DB1", "Startposition " + startPosition);
        if (savedInstanceState != null) {
            int savedPosition = savedInstanceState.getInt("current_position");
            recyclerView.scrollToPosition(savedPosition);
        } else {
            recyclerView.scrollToPosition(startPosition);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.constraintLayoutZipLibrary), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Get the current position from the LayoutManager
        int currentPosition = layoutManager.findFirstVisibleItemPosition();
        outState.putInt("current_position", currentPosition);
    }

}