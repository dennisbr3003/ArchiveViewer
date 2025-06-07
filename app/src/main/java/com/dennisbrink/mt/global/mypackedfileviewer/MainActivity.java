package com.dennisbrink.mt.global.mypackedfileviewer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;

public class MainActivity extends AppCompatActivity implements IZipApplication {

    private ZipLibraryAdapter adapter;
    TextView statusBar;

    private final ActivityResultLauncher<Intent> libraryContentsLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        (ActivityResult result) -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                int position = result.getData().getIntExtra("position", -1);
                if (position != -1) {
                    adapter.notifyItemChanged(position); // to refresh the recycler view with new data
                }
            }
        }
    );

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ImageButton buttonClose = findViewById(R.id.buttonClose);
        buttonClose.setOnClickListener(v -> {
            finish(); // Close the activity
        });

        ImageButton buttonClearCache = findViewById(R.id.buttonClearCache);
        buttonClearCache.setOnClickListener(view -> {
            ThumbnailCache thumbnailCache = new ThumbnailCache();
            thumbnailCache.clearCacheFolder(CACHE_DIR);
            adapter.notifyDataSetChanged(); // this is correct in this case -->  all thumbnails are cleared
        });

        ImageButton buttonClearSavedData = findViewById(R.id.buttonClearSavedData);
        buttonClearSavedData.setOnClickListener(view -> {
            ThumbnailCache thumbnailCache = new ThumbnailCache();
            thumbnailCache.clearCacheFolder(FILE_EXTRA_DIR); // no effect on thumbnails
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerView1);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        statusBar = findViewById(R.id.statusBar);

        adapter = new ZipLibraryAdapter(libraryContentsLauncher);
        recyclerView.setAdapter(adapter);

        setStatusBarText();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main4), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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

}