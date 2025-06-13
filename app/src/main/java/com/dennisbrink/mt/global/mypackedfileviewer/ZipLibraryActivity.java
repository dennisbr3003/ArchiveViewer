package com.dennisbrink.mt.global.mypackedfileviewer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ZipLibraryActivity extends AppCompatActivity implements IZipApplication, IZipLibraryActivityListener {

    private ZipLibraryAdapter adapter;
    TextView statusBar;
    Receiver receiver = null;
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

   // @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_zip_library);

        // What FLAG_SECURE Does:
        // Prevents Screenshots: Blocks the screen from being captured or recorded.
        // Hides in Recents: Prevents your app’s contents from appearing in the recent apps preview.
        // Limitations
        // Visibility: This flag doesn't display a custom view but effectively obscures the screen by
        // preventing its display in the ways mentioned above.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        ImageButton buttonClose = findViewById(R.id.buttonClose);
        buttonClose.setOnClickListener(v -> {
            finishAffinity(); // Close all the activities and close the app
        });

        ImageButton buttonClearAll = findViewById(R.id.buttonClearAll);
        buttonClearAll.setOnClickListener(view -> {
            ZipDialogs.createAndShowDialog(
                    view.getContext(),
                    getString(R.string.confirm_deletion_title),
                    getString(R.string.textmessage_delete_app_data),
                    DELETE_APP_DATA
            );
        });

        ImageButton buttonClearCache = findViewById(R.id.buttonClearCache);
        buttonClearCache.setOnClickListener(view -> {
            ZipDialogs.createAndShowDialog(
                    view.getContext(),
                    getString(R.string.confirm_deletion_title),
                    getString(R.string.textmessage_delete_cached_data),
                    DELETE_CACHED_DATA
            );
        });

        ImageButton buttonClearSavedData = findViewById(R.id.buttonClearSavedData);
        buttonClearSavedData.setOnClickListener(view -> {
            ZipDialogs.createAndShowDialog(
                    view.getContext(),
                    getString(R.string.confirm_deletion_title),
                    getString(R.string.textmessage_delete_extra_data),
                    DELETE_EXTRA_DATA
            );
        });

        ImageButton buttonClearTouchPoints = findViewById(R.id.buttonClearTouchpoints);
        buttonClearTouchPoints.setOnClickListener(view -> {
            ZipDialogs.createAndShowDialog(
                    view.getContext(),
                    getString(R.string.confirm_deletion_title),
                    getString(R.string.textmessage_delete_touch_point_sequence),
                    DELETE_TOUCH_POINTS
            );
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerViewZipLibrary);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        statusBar = findViewById(R.id.statusBar);

        adapter = new ZipLibraryAdapter(libraryContentsLauncher);
        recyclerView.setAdapter(adapter);

        setStatusBarText();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.constraintLayoutZipLibrary), (v, insets) -> {
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

    // registering this activity for use of broadcast-receiver
    private IntentFilter getFilter(){
        IntentFilter intentFilter = new IntentFilter();
        Log.d("DB1", "ZipLibraryActivity.class: (getFilter) Registering for broadcast action " + DELETE_TOUCH_POINTS);
        intentFilter.addAction(DELETE_TOUCH_POINTS);
        intentFilter.addAction(DELETE_APP_DATA);
        intentFilter.addAction(DELETE_EXTRA_DATA);
        intentFilter.addAction(DELETE_CACHED_DATA);
        return intentFilter;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (receiver != null){
            Log.d("DB1", "ZipLibraryActivity.class: (onPause) Unregistering receiver");
            this.unregisterReceiver(receiver);
            receiver = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onResume() {
        super.onResume();
        if(receiver == null){
            Log.d("DB1", "ZipLibraryActivity.class: (onResume) Registering receiver");
            receiver = new Receiver();
            // Make yourself known to the receiver. This way the receiver will know this activity exists
            // and will be able to run any of the methods connected to the actions registered
            // This uses polymorphism --> the class is of type IZipLibraryActivityListener
            receiver.setZipLibraryActivity(this);
        }
        this.registerReceiver(receiver, getFilter(), Context.RECEIVER_EXPORTED);
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