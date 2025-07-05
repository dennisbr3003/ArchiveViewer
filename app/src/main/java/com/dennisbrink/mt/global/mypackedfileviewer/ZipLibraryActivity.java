package com.dennisbrink.mt.global.mypackedfileviewer;

import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.dennisbrink.mt.global.mypackedfileviewer.events.OpenZipLibraryEvent;
import com.dennisbrink.mt.global.mypackedfileviewer.events.OpenZipLibraryFileEvent;
import com.dennisbrink.mt.global.mypackedfileviewer.fragments.FragmentZipLibraries;
import com.dennisbrink.mt.global.mypackedfileviewer.fragments.FragmentZipLibrary;
import com.dennisbrink.mt.global.mypackedfileviewer.fragments.FragmentZipLibraryFile;
import com.dennisbrink.mt.global.mypackedfileviewer.fragments.FragmentZipLibraryVideoFile;
import com.dennisbrink.mt.global.mypackedfileviewer.structures.ZipLibrary;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class ZipLibraryActivity extends AppCompatActivity implements IZipApplication  {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_zip_library);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new FragmentZipLibraries())
                    .commit();
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.constraintLayoutZipLibrary), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations() && !isFinishing()) {
            finishAffinity();
        }
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    // Use @Subscribe to handle the Event
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOpenZipLibraryEvent(OpenZipLibraryEvent event) {
        Log.d("DB1", "ZipLibraryActivity.onOpenZipLibraryEvent: Event captured, open library fragment for position " + event.position);
        loadFragmentZipLibrary(event.position);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOpenZipLibraryFileEvent(OpenZipLibraryFileEvent event) {
        Log.d("DB1", "ZipLibraryActivity.onOpenZipLibraryFileEvent: Event captured source: "
                                + event.source + ", target: " + event.target + ", zipkey: " + event.zipkey
                                + ", type: " + event.eFileType.name() + ", position: " + event.position
                                + ", fileName: " + event.fileName);
        if(event.eFileType.equals(EFileTypes.VIDEO)) {
            loadFragmentZipLibraryVideoFile(event.position, event.source, event.target, event.zipkey, event.fileName);
        } else {
            loadFragmentZipLibraryFile(event.position, event.source, event.target, event.zipkey);
        }
    }

    private void loadFragmentZipLibrary(int position) {
        ZipLibrary item = ZipApplication.getLibraries().get(position);
        FragmentZipLibrary fragment = FragmentZipLibrary.newInstance(
                item.getSource(),
                item.getTarget(),
                item.getName(),
                item.getZipkey(),
                position
        );
        this.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment) // Use your FrameLayout's ID
                .addToBackStack(null)
                .commit();
    }

    private void loadFragmentZipLibraryFile (int position, String source, String target, String zipkey) {
        FragmentZipLibraryFile fragment = FragmentZipLibraryFile.newInstance(
                position,
                source,
                target,
                zipkey
        );
        this.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void loadFragmentZipLibraryVideoFile (int position, String source, String target, String zipkey, String fileName) {
        FragmentZipLibraryVideoFile fragment = FragmentZipLibraryVideoFile.newInstance(
                position,
                source,
                target,
                zipkey,
                fileName
        );
        this.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

}