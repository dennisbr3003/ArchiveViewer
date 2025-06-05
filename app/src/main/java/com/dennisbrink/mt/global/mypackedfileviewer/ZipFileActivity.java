package com.dennisbrink.mt.global.mypackedfileviewer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;


public class ZipFileActivity extends AppCompatActivity {

    private int position = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_zip_file);

        // Add a callback to handle the back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

                Log.d("DB1", "Back pressed, setting result and finishing");

                // Prepare result data
                Intent resultIntent = new Intent();
                // Put the position in the intent extras
                resultIntent.putExtra("position", position);

                // Set the result with RESULT_OK or any appropriate result code
                setResult(RESULT_OK, resultIntent);

                // Finish the activity
                finish();
            }
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerView2);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

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
        numEntriesTextView.setText(String.valueOf(zipContent.size()));

        ZipFileAdapter adapter = new ZipFileAdapter(zipContent, position -> {
            Intent intent = new Intent(ZipFileActivity.this, ZipContentViewerActivity.class);
            intent.putExtra("position", position);
            intent.putExtra("target", target);
            intent.putExtra("source", source);
            intent.putExtra("zipkey", zipkey);
            startActivity(intent);
        }, source);

        recyclerView.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main2), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

}