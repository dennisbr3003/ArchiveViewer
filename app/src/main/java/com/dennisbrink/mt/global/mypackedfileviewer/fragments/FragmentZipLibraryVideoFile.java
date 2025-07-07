package com.dennisbrink.mt.global.mypackedfileviewer.fragments;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.dennisbrink.mt.global.mypackedfileviewer.IZipApplication;
import com.dennisbrink.mt.global.mypackedfileviewer.R;
import com.dennisbrink.mt.global.mypackedfileviewer.ZipApplication;
import com.dennisbrink.mt.global.mypackedfileviewer.events.DialogResultActionEvent;
import com.dennisbrink.mt.global.mypackedfileviewer.events.VideoThumbnailFinalEvent;
import com.dennisbrink.mt.global.mypackedfileviewer.libraries.ThumbnailCache;
import com.dennisbrink.mt.global.mypackedfileviewer.libraries.ZipUtilities;
import com.dennisbrink.mt.global.mypackedfileviewer.structures.ZipEntryData;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentZipLibraryVideoFile extends Fragment implements IZipApplication {

    private VLCVideoLayout videoLayout;
    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Controls
    private Button playPauseBtn, loopBtn, soundBtn;
    private TextView totalTimeTv, currentTimeTv, timeRemainingTv;
    private SeekBar seekBar;
    private boolean isPlaying = true;
    private boolean isLooping = true;
    private boolean isMuted = true;
    private long durationMs = 0;
    private Runnable progressUpdater;
    private Boolean isFinal;
    private int startPosition = 0;
    private String source, target, zipkey, filename, cacheName, cacheFolder;
    FileHeader fileHeader = null;
    ThumbnailCache thumbnailCache = new ThumbnailCache();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    Bitmap placeholder = BitmapFactory.decodeResource(ZipApplication.getAppContext().getResources(), R.drawable.no_image_small);
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_zip_library_video_file, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get arguments (pass these via newInstance or setArguments from activity/parent fragment)
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

        videoLayout = view.findViewById(R.id.vlc_video_layout);
        playPauseBtn = view.findViewById(R.id.btn_play_pause);
        loopBtn = view.findViewById(R.id.btn_loop);
        soundBtn = view.findViewById(R.id.btn_sound);

        totalTimeTv = view.findViewById(R.id.total_time);
        currentTimeTv = view.findViewById(R.id.current_time);
        timeRemainingTv = view.findViewById(R.id.time_remaining);
        seekBar = view.findViewById(R.id.seek_bar);

        Log.d("DB1", "FragmentZipLibraryVideoFile.onViewCreated: Position " + startPosition);
        Log.d("DB1", "FragmentZipLibraryVideoFile.onViewCreated: Source " + source);
        Log.d("DB1", "FragmentZipLibraryVideoFile.onViewCreated: Target " + target);
        Log.d("DB1", "FragmentZipLibraryVideoFile.onViewCreated: Zipkey " + zipkey);
        Log.d("DB1", "FragmentZipLibraryVideoFile.onViewCreated: Filename " + filename);
        Log.d("DB1", "FragmentZipLibraryVideoFile.onViewCreated: Cache Folder " + cacheFolder);
        Log.d("DB1", "FragmentZipLibraryVideoFile.onViewCreated: Cache Name " + cacheName);
        Log.d("DB1", "FragmentZipLibraryVideoFile.onViewCreated: Final " + isFinal);

        // -- Load video into memory (in real apps, exercise caution with large files!)
        byte[] videoBytes;
        try {
            ZipFile zipFile = new ZipFile(new File(ZipApplication.getAppContext().getFilesDir(), this.target), this.zipkey.toCharArray());
            fileHeader = zipFile.getFileHeader(filename);
            InputStream inputStream = zipFile.getInputStream(fileHeader); // Create an InputStream from the zip entry
            videoBytes = inputStream.readAllBytes();
        } catch (Exception e) {
            Log.d("DB1", "FragmentZipLibraryVideoFile.onViewCreated: Unable to create inputStream: " + e.getMessage());
            return;
        }

        Log.d("DB1", "FragmentZipLibraryVideoFile.onViewCreated: Check for file : "
                                + VIDEO_FILES_DIR + VIDEO_FILES_PREFIX + filename.hashCode());

        File tempFile = new File(ZipApplication.getAppContext().getFilesDir(),
                                VIDEO_FILES_DIR + VIDEO_FILES_PREFIX + filename.hashCode());
        if(!tempFile.exists()) {
            ZipUtilities.saveByteDataToFile(VIDEO_FILES_PREFIX + filename.hashCode(), VIDEO_FILES_DIR, videoBytes);
            Log.d("DB1", "FragmentZipLibraryVideoFile.onViewCreated: tempFile copied");
        }

        // always execute because the placeholder is cached as thumbnail (so we cannot check if there is none)
        if(!isFinal) {
            executorService.execute(() -> {
                Log.d("DB1", "FragmentZipLibraryVideoFile.onViewCreated: thumbnail to be created");
                Bitmap videoThumbnail = createVideoThumbnail(tempFile.getAbsolutePath());
                try {
                    thumbnailCache.saveThumbnail(cacheFolder, cacheName, videoThumbnail);
                    Log.d("DB1", "FragmentZipLibraryVideoFile.onViewCreated: thumbnail saved");
                    if (startPosition == 0) { // save the first image as library thumbnail
                        thumbnailCache.saveThumbnail("", "cache_" + source.hashCode(), videoThumbnail);
                        Log.d("DB1", "FragmentZipLibraryVideoFile.onViewCreated: library thumbnail saved");
                    }
                } catch (IOException e) {
                    Log.d("DB1", "FragmentZipLibraryVideoFile.onViewCreated: Thumbnail not saved: " + e.getMessage());
                }
                // send a event that the thumbnail is set now and the ZipEntryData must be updated
                Log.d("DB1", "FragmentZipLibraryVideoFile.onViewCreated: sending VideoThumbnailFinalEvent for " + startPosition);
                EventBus.getDefault().post(new VideoThumbnailFinalEvent(startPosition, target, source, zipkey));
            });
        }
        // -- Setup VLC
        libVLC = new LibVLC(requireContext());
        mediaPlayer = new MediaPlayer(libVLC);
        mediaPlayer.attachViews(videoLayout, null, false, false);

        // Set up media and play
        Media media = new Media(libVLC, tempFile.getAbsolutePath());
        media.setHWDecoderEnabled(true, false);
        mediaPlayer.setVolume(0);
        mediaPlayer.setMedia(media);
        mediaPlayer.play();
        mediaPlayer.setEventListener(event -> {
            if (event.type == MediaPlayer.Event.EndReached) {
                Log.d("DB1", "End of media reached.");
                if (!isLooping) {
                    // Clean up on completion
                    mediaPlayer.stop();
                    media.release();
                    mediaPlayer.release();
                    libVLC.release();
                    // tempFile.delete(); // Delete the temp file when done
                } else {
                    // Start - stop
                    mediaPlayer.stop();
                    durationMs = 0; // reset progress bar hopefully
                    mediaPlayer.play(); // Restart the video
                }
            }
            // -- Listen for prepared/duration
            if (event.type == MediaPlayer.Event.LengthChanged) {
                durationMs = event.getLengthChanged();
                seekBar.setMax((int) (durationMs / 1000));
                totalTimeTv.setText(formatTime(durationMs));
                timeRemainingTv.setText("(-" + formatTime(durationMs) + ")");
            }
        });

        // -- SeekBar and progress updates
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean userSeeking = false;

            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser && durationMs > 0) {
                    long seekMs = progress * 1000L;
                    currentTimeTv.setText(formatTime(seekMs));
                    timeRemainingTv.setText("(-" + formatTime(durationMs - seekMs) +
                            "(-" + formatTime(durationMs - seekMs) + ")");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
                userSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                if (durationMs > 0) {
                    int seekToSec = bar.getProgress();
                    long seekToMs = seekToSec * 1000L;
                    mediaPlayer.setTime(seekToMs);
                }
                userSeeking = false;
            }
        });

        // -- Controls setup
        playPauseBtn.setOnClickListener(v -> togglePlayPause());
        loopBtn.setOnClickListener(v -> toggleLoop());
        soundBtn.setOnClickListener(v -> toggleSound());

        // -- Progress update loop
        progressUpdater = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && durationMs > 0) {
                    long posMs = mediaPlayer.getTime();
                    currentTimeTv.setText(formatTime(posMs));
                    timeRemainingTv.setText("(-" + formatTime(durationMs - posMs) + ")");
                    if (!seekBar.isPressed()) {
                        seekBar.setProgress((int) (posMs / 1000));
                    }
                }
                handler.postDelayed(this, 500);
            }
        };
        handler.postDelayed(progressUpdater, 500);
    }

    private void togglePlayPause() {
        if (isPlaying) {
            mediaPlayer.pause();
            playPauseBtn.setText("Play");
        } else {
            mediaPlayer.play();
            playPauseBtn.setText("Pause");
        }
        isPlaying = !isPlaying;
    }

    private void toggleLoop() {
        isLooping = !isLooping;
        //mediaPlayer.setRepeat(isLooping);
        loopBtn.setText("Loop: " + (isLooping ? "On" : "Off"));
    }

    private void toggleSound() {
        if (isMuted) {
            mediaPlayer.setVolume(100);
            soundBtn.setText("Sound: On");
        } else {
            mediaPlayer.setVolume(0);
            soundBtn.setText("Sound: Off");
        }
        isMuted = !isMuted;
    }

    private String formatTime(long ms) {
        int totalSeconds = (int) (ms / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.detachViews();
            mediaPlayer.release();
        }
        if (libVLC != null) {
            libVLC.release();
        }

    }

    public Bitmap createVideoThumbnail(String videoPath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(videoPath);
            // Extract frame at the specified time (ms)
            Bitmap originalThumbnail = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            // Resize the thumbnail to 45x45
            if (originalThumbnail != null) {
                return Bitmap.createScaledBitmap(originalThumbnail, 45, 45, true);
            } else {
                Log.d("DB1", "FragmentZipLibraryVideoFile.createVideoThumbnail: Thumbnail = null");
                return placeholder;
            }
        } catch (Exception e) {
            Log.d("DB1", "FragmentZipLibraryVideoFile.createVideoThumbnail: No thumbnail created: " + e.getMessage());
            return placeholder;
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                Log.d("DB1", "FragmentZipLibraryVideoFile.createVideoThumbnail: Retriever release failed: " + e.getMessage());
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