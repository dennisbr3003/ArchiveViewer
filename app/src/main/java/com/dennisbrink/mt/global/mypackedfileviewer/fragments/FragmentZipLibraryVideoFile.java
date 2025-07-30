package com.dennisbrink.mt.global.mypackedfileviewer.fragments;

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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import com.dennisbrink.mt.global.mypackedfileviewer.IZipApplication;
import com.dennisbrink.mt.global.mypackedfileviewer.R;
import com.dennisbrink.mt.global.mypackedfileviewer.ZipApplication;
import com.dennisbrink.mt.global.mypackedfileviewer.events.VideoThumbnailFinalEvent;
import com.dennisbrink.mt.global.mypackedfileviewer.libraries.ThumbnailCache;
import com.dennisbrink.mt.global.mypackedfileviewer.libraries.ZipUtilities;
import com.dennisbrink.mt.global.mypackedfileviewer.structures.ZipEntryData;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import org.greenrobot.eventbus.EventBus;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentZipLibraryVideoFile extends Fragment implements IZipApplication {

    private PlayerView playerView;
    private ExoPlayer player;
//    private TextView totalTimeTv, currentTimeTv, timeRemainingTv;
//    private SeekBar seekBar;
    private ImageButton soundToggle;
    private int startPosition = 0;
    private String source, target, zipkey, filename, cacheName, cacheFolder;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
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
//        totalTimeTv = view.findViewById(R.id.total_time);
//        currentTimeTv = view.findViewById(R.id.current_time);
//        timeRemainingTv = view.findViewById(R.id.time_remaining);
//        seekBar = view.findViewById(R.id.seek_bar);

//        Handler handler = new Handler(Looper.getMainLooper());
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
        if (!tempFile.exists()) {

            byte[] videoBytes;
            try {
                ZipFile zipFile = new ZipFile(new File(ZipApplication.getAppContext().getFilesDir(), this.target), this.zipkey.toCharArray());
                FileHeader fileHeader = zipFile.getFileHeader(filename);
                InputStream inputStream = zipFile.getInputStream(fileHeader);
                videoBytes = inputStream.readAllBytes();
            } catch (Exception e) {
                Log.d("DB1", "Unable to create inputStream: " + e.getMessage());
                return;
            }

            ZipUtilities.saveByteDataToFile(filename.hashCode() + ".mp4", "", videoBytes);
        }

        soundToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("DB1", "sound toggle click listener " + player.isDeviceMuted());
                if(player.getVolume()==0) {
                    player.setVolume(1.0f);
                    soundToggle.setImageResource(R.drawable.sound_off); // Muted icon
                } else {
                    player.setVolume(0.0f);
                    soundToggle.setImageResource(R.drawable.sound_on);
                }
            }
        });

        // Handle thumbnail creation
        executorService.execute(() -> {
            Log.d("DB1", "Creating thumbnail executorService");
            Bitmap videoThumbnail;
            Log.d("DB1", "Creating thumbnail executorService 2 " + tempFile.getAbsolutePath());
            try {
                videoThumbnail = createVideoThumbnail(tempFile.getAbsolutePath());
            } catch (Exception e) {
                Log.d("DB1", "Creating thumbnail 2 not reached " + e.getMessage());
                throw new RuntimeException(e);
            }
            Log.d("DB1", "Creating thumbnail 2");
            ThumbnailCache thumbnailCache = new ThumbnailCache();
            try {
                thumbnailCache.saveThumbnail(cacheFolder, cacheName, videoThumbnail);
                if (startPosition == 0) {
                    thumbnailCache.saveThumbnail("", "cache_" + source.hashCode(), videoThumbnail);
                }
            } catch (IOException e) {
                Log.d("DB1", "Thumbnail not saved: " + e.getMessage());
            }
            Log.d("DB1", "Creating thumbnail 3");
            EventBus.getDefault().post(new VideoThumbnailFinalEvent(startPosition, target, source, zipkey));
            Log.d("DB1", "Creating thumbnail 4");
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

        // Runnable to update progress
//        Runnable updateSeekBar = new Runnable() {
//            @Override
//            public void run() {
//                if (player != null) {
////                    seekBar.setProgress((int) (player.getCurrentPosition() / 1000));
////                    handler.postDelayed(this, 1000);
//
//                    long currentPosition = player.getCurrentPosition();
//                    long totalDuration = player.getDuration();
//                    Log.d("DB1", "currentPosition/totalDuration " + currentPosition  + "/" + totalDuration);
////                    currentTimeTv.setText(formatTime(currentPosition));
////                    totalTimeTv.setText(formatTime(totalDuration));
////                    timeRemainingTv.setText(formatTime(totalDuration - currentPosition));
//
//                }
//            }
//        };
//        player.addListener(new Player.Listener() {
//            @Override
//            public void onPlaybackStateChanged(int playbackState) {
//                if (playbackState == ExoPlayer.STATE_READY) {
//                    seekBar.setMax((int) (player.getDuration() / 1000));
//                }
//            }
//        });
        // Start the update loop
     //   handler.post(updateSeekBar);

//        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                if (fromUser) {
//                    long seekMs = progress * 1000L;
//                    player.seekTo(seekMs);
//                }
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {}
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {}
//        });

        if (savedInstanceState != null) {
            long savedPosition = savedInstanceState.getLong("current_position", 0);
            player.seekTo(savedPosition);
        }
    }

//    private void toggleSound() {
//        if (isMuted) {
//            player.setVolume(1.0f);
//            soundBtn.setText("Sound: On");
//        } else {
//            player.setVolume(0.0f);
//            soundBtn.setText("Sound: Off");
//        }
//        isMuted = !isMuted;
//    }

//    private String formatTime(long ms) {
//        int totalSeconds = (int) (ms / 1000);
//        int minutes = totalSeconds / 60;
//        int seconds = totalSeconds % 60;
//        return String.format("%02d:%02d", minutes, seconds);
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
            Log.d("DB1", "No thumbnail created: " + e.getMessage());
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