package com.dennisbrink.mt.global.mypackedfileviewer.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import com.dennisbrink.mt.global.mypackedfileviewer.IZipApplication;
import com.dennisbrink.mt.global.mypackedfileviewer.R;
import com.dennisbrink.mt.global.mypackedfileviewer.ZipApplication;
import com.dennisbrink.mt.global.mypackedfileviewer.events.VideoThumbnailFinalEvent;
import com.dennisbrink.mt.global.mypackedfileviewer.libraries.ThumbnailCache;
import com.dennisbrink.mt.global.mypackedfileviewer.structures.ZipEntryData;

import org.greenrobot.eventbus.EventBus;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentZipLibraryVideoFile extends Fragment implements IZipApplication {

    private PlayerView playerView;
    private ExoPlayer player;
    private ImageButton soundToggle;
    private int startPosition = 0;
    private String source, target, zipkey, filename, cacheName, cacheFolder;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ExecutorService executorService2 = Executors.newSingleThreadExecutor();
    Bitmap placeholder = BitmapFactory.decodeResource(ZipApplication.getAppContext().getResources(), R.drawable.no_image_small);

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_zip_library_video_file, container, false);
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        playerView = view.findViewById(R.id.player_view);
        soundToggle = view.findViewById(R.id.imageButtonSoundOnOff);

        if (getArguments() != null) {
            startPosition = getArguments().getInt("position", 0);
            source = getArguments().getString("source");
            target = getArguments().getString("target");
            zipkey = getArguments().getString("zipkey");
            filename = getArguments().getString("filename");
            cacheName = getArguments().getString("cacheName");
            cacheFolder = getArguments().getString("cacheFolder");
        }

        File tempFile = new File(ZipApplication.getAppContext().getFilesDir(), filename.hashCode() + ".mp4");
//        if (!tempFile.exists()) {
//            // show ProgressBar
//
//            executorService.execute(() -> {
//                byte[] videoBytes;
//                try {
//                    ZipFile zipFile = new ZipFile(new File(ZipApplication.getAppContext().getFilesDir(), this.target), this.zipkey.toCharArray());
//                    FileHeader fileHeader = zipFile.getFileHeader(filename);
//                    InputStream inputStream = zipFile.getInputStream(fileHeader);
//                    videoBytes = inputStream.readAllBytes();
//                } catch (Exception e) {
//                    Log.d("DB1", "Unable to create inputStream: " + e.getMessage());
//                    new Handler(Looper.getMainLooper()).post(this::hideProgressbar);
//                    return;
//                }
//
//                ZipUtilities.saveByteDataToFile(filename.hashCode() + ".mp4", "", videoBytes);
//
//                new Handler(Looper.getMainLooper()).post(this::hideProgressbar);
//
//            });
//        }

        soundToggle.setOnClickListener(view1 -> {
            Log.d("DB1", "sound toggle click listener " + player.isDeviceMuted());
            if(player.getVolume()==0) {
                player.setVolume(1.0f);
                soundToggle.setImageResource(R.drawable.sound_off); // Muted icon
            } else {
                player.setVolume(0.0f);
                soundToggle.setImageResource(R.drawable.sound_on);
            }
        });

        // Handle thumbnail creation
        executorService.execute(() -> {
            Bitmap videoThumbnail;
            try {
                videoThumbnail = createVideoThumbnail(tempFile.getAbsolutePath());
            } catch (Exception e) {
                Log.d("DB1", "FragmentZipLibraryVideoFile.onViewCreated.executorService.execute - Creating thumbnail error: " + e.getMessage());
                throw new RuntimeException(e);
            }
            ThumbnailCache thumbnailCache = new ThumbnailCache();
            try {
                thumbnailCache.saveThumbnail(cacheFolder, cacheName, videoThumbnail);
                if (startPosition == 0) {
                    thumbnailCache.saveThumbnail("", "cache_" + source.hashCode(), videoThumbnail);
                }
            } catch (IOException e) {
                Log.d("DB1", "FragmentZipLibraryVideoFile.onViewCreated.executorService.execute - Thumbnail not saved: " + e.getMessage());
            }
            EventBus.getDefault().post(new VideoThumbnailFinalEvent(startPosition, target, source, zipkey));
        });

        // Setup ExoPlayer
        player = new ExoPlayer.Builder(requireContext()).build();
        playerView.setPlayer(player);
        playerView.setUseController(false);
        MediaItem mediaItem = MediaItem.fromUri(Uri.fromFile(tempFile));

        player.setMediaItem(mediaItem);
        player.prepare();


        player.setPlayWhenReady(true);
        player.setVolume(0.0f);

        playerView.setControllerShowTimeoutMs(-1);
        playerView.hideController();
        playerView.setUseController(true);

        player.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE);

        if (savedInstanceState != null) {
            long savedPosition = savedInstanceState.getLong("current_position", 0);
            player.seekTo(savedPosition);
        }
    }

//    private void hideProgressbar() {
//        Log.d("DB1", "Hide progressbar");
//    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (player != null) {
            long currentPosition = player.getCurrentPosition();
            outState.putLong("current_position", currentPosition);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (player != null) {
            player.setPlayWhenReady(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (player != null) {
            player.release();
            player = null;
        }
        executorService.shutdown();
    }

    public Bitmap createVideoThumbnail(String videoPath) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(videoPath);
            Bitmap originalThumbnail = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            return (originalThumbnail != null) ? Bitmap.createScaledBitmap(originalThumbnail, 45, 45, true) : placeholder;
        } catch (Exception e) {
            Log.d("DB1", "FragmentZipLibraryVideoFile.createVideoThumbnail - No thumbnail created: " + e.getMessage());
            return placeholder;
        } finally {
            retriever.release();
        }
    }

    // Static factory method for creating the fragment and passing arguments
    public static FragmentZipLibraryVideoFile newInstance(int position, String source, String target, String zipkey, ZipEntryData entryData) {
        FragmentZipLibraryVideoFile fragment = new FragmentZipLibraryVideoFile();
        Bundle args = new Bundle();
        args.putInt("position", position);
        args.putString("source", source);
        args.putString("target", target);
        args.putString("zipkey", zipkey);
        args.putString("filename", entryData.getFileName());
        args.putString("cacheFolder", entryData.getCacheFolder());
        args.putString("cacheName", entryData.getCacheName());
        args.putBoolean("isFinal", entryData.getFinal());
        fragment.setArguments(args);
        return fragment;
    }

}