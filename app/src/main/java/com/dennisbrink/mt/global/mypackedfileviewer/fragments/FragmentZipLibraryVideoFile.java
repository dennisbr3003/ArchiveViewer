package com.dennisbrink.mt.global.mypackedfileviewer.fragments;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.media3.common.C;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FragmentZipLibraryVideoFile extends Fragment implements IZipApplication {

    private ExoPlayer player;
    private ImageButton soundToggle;
    private int startPosition = 0;
    private boolean isFinal = false;
    private String source, target, zipkey, filename, cacheName, cacheFolder;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    Bitmap placeholder = BitmapFactory.decodeResource(ZipApplication.getAppContext().getResources(), R.drawable.no_image_small);

    TextView tvFileName, tvCurrentTime, tvPlayTime;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_zip_library_video_file, container, false);
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PlayerView playerView = view.findViewById(R.id.player_view);
        soundToggle = view.findViewById(R.id.imageButtonSoundOnOff);

        if (getArguments() != null) {
            startPosition = getArguments().getInt("position", 0);
            source = getArguments().getString("source");
            target = getArguments().getString("target");
            zipkey = getArguments().getString("zipkey");
            filename = getArguments().getString("filename");
            cacheName = getArguments().getString("cacheName");
            cacheFolder = getArguments().getString("cacheFolder");
            isFinal = getArguments().getBoolean("isFinal");
        }

        tvFileName = view.findViewById(R.id.textViewVideoFileName);
        tvCurrentTime = view.findViewById(R.id.textViewVideoCurrentTime);
        tvPlayTime = view.findViewById(R.id.textViewViedeoPlayTime);

        File tempFile = new File(ZipApplication.getAppContext().getFilesDir(), filename.hashCode() + ".mp4");

        soundToggle.setOnClickListener(view1 -> {
            Log.d("DB1", "FragmentZipLibraryVideoFile.onViewCreated.soundToggle.setOnClickListener - sound toggle click listener " + player.isDeviceMuted());
            if(player.getVolume()==0) {
                player.setVolume(1.0f);
                soundToggle.setImageResource(R.drawable.sound_off); // Muted icon
            } else {
                player.setVolume(0.0f);
                soundToggle.setImageResource(R.drawable.sound_on);
            }
        });

        // Handle thumbnail creation
        if(!isFinal) {
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
        }

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

        tvFileName.setText(filename);

        // start polling the playing video
        executorService.scheduleWithFixedDelay(() -> {
            mainHandler.post(() -> {
                tvPlayTime.setText(formatTime(player.getDuration()));
                tvCurrentTime.setText(formatTime(player.getCurrentPosition()));
            });
        }, 0, 1, TimeUnit.SECONDS);

        if (savedInstanceState != null) { // executed after configuration change
            long savedPosition = savedInstanceState.getLong("current_position", 0);
            player.seekTo(savedPosition);
        }
    }

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

    @SuppressLint("DefaultLocale")
    private String formatTime(long timeInMillis) {
        if (timeInMillis == C.TIME_UNSET) {
            return "00:00"; // duration could not yet be determined (player sends constant -9223372036854775807)
        }
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public Bitmap createVideoThumbnail(String videoPath) throws IOException {
        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
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