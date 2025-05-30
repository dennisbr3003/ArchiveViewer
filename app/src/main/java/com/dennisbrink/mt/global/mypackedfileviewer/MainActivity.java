package com.dennisbrink.mt.global.mypackedfileviewer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private ZipLibraryAdapter adapter;
    TextView statusBar;

    private final ActivityResultLauncher<Intent> libraryContentsLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                int position = result.getData().getIntExtra("position", -1);
                if (position != -1) {
                    updateData(position);
                }
            }
        }
    );

    private void updateData(int position) {
        try {
            adapter.notifyItemChanged(position);
        } catch (Exception e){
            Log.d("DB1", e.getMessage());
        }
    }

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
            thumbnailCache.clearThumbNailCache();
            adapter.notifyDataSetChanged(); // this is correct in this case -->  all thumbnails are cleared
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerView1);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ZipLibraries zipLibraries = new ZipLibraries();

        statusBar = findViewById(R.id.statusBar);

        try {
            zipLibraries = loadLibrariesFromAssets("libraries-dev.json");
        } catch (Exception e) {
            Log.d("DB1", Objects.requireNonNull(e.getMessage()));
        }

        adapter = new ZipLibraryAdapter(zipLibraries.getLibraries(), libraryContentsLauncher);
        recyclerView.setAdapter(adapter);

        setStatusBarText(zipLibraries);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main4), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public ZipLibraries loadLibrariesFromAssets(String fileName) {
        AssetManager assetManager = ZipApplication.getAppContext().getAssets();

        try (InputStream inputStream = assetManager.open(fileName);
             InputStreamReader reader = new InputStreamReader(inputStream)) {

            Gson gson = new Gson();
            return gson.fromJson(reader, ZipLibraries.class);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setStatusBarText(ZipLibraries zipLibraries) {
        @SuppressLint("DefaultLocale") String text = String.format(
                "%s %d %s %s",
                getString(R.string.libraries),
                zipLibraries.getLibraries().size(),
                getString(R.string.size),
                zipLibraries.getTotalFileSize()
        );
        statusBar.setText(text);
    }

}