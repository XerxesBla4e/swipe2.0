package com.example.e_sholpine.activity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.e_sholpine.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class CameraActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_GALLERY_PERMISSION = 101;
    private static final int REQUEST_GALLERY_PICK = 5;
    private static final long RECORDING_DURATION_MS = 30_000;

    // UI Components
    private PreviewView previewView;
    private Button recordButton;
    private Button flipButton;
    private ImageButton uploadButton;

    // CameraX Components
    private ProcessCameraProvider cameraProvider;
    private Recorder recorder;
    private VideoCapture<Recorder> videoCapture;
    private Recording recording;

    // State Management
    private boolean isRecording = false;
    private File outputFile;
    private int currentLensFacing = CameraSelector.LENS_FACING_BACK;
    private final Handler recordingHandler = new Handler(Looper.getMainLooper());
    private final Runnable stopRecordingRunnable = this::stopRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        initializeViews();
        setupPermissions();
        setupButtonListeners();
    }

    private void initializeViews() {
        previewView = findViewById(R.id.previewView);
        recordButton = findViewById(R.id.recordButton);
        flipButton = findViewById(R.id.flipButton);
        uploadButton = findViewById(R.id.uploadButton);
    }

    private void setupPermissions() {
        if (checkCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }
    }

    private void setupButtonListeners() {
        recordButton.setOnClickListener(v -> toggleRecording());
        flipButton.setOnClickListener(v -> flipCamera());
        uploadButton.setOnClickListener(v -> pickVideoFromGallery());
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                REQUEST_CAMERA_PERMISSION);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                showToast("Camera initialization failed: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        recorder = new Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build();
        videoCapture = new VideoCapture.Builder<>(recorder).build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(currentLensFacing)
                .build();

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture);
        } catch (IllegalArgumentException e) {
            showToast("Camera binding failed: " + e.getMessage());
        }
    }

    private void toggleRecording() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        outputFile = new File(getExternalFilesDir(null), "video_" + System.currentTimeMillis() + ".mp4");
        FileOutputOptions fileOutputOptions = new FileOutputOptions.Builder(outputFile).build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        recording = recorder.prepareRecording(this, fileOutputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this), this::handleVideoRecordEvent);

        isRecording = true;
        updateUIForRecordingStart();
        recordingHandler.postDelayed(stopRecordingRunnable, RECORDING_DURATION_MS);
    }

    private void handleVideoRecordEvent(VideoRecordEvent event) {
        if (event instanceof VideoRecordEvent.Start) {
            runOnUiThread(this::updateUIForRecordingStart);
        } else if (event instanceof VideoRecordEvent.Finalize) {
            VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) event;
            if (finalizeEvent.hasError()) {
                handleRecordingError(finalizeEvent.getError());
            } else {
                handleRecordingSuccess();
            }
        }
    }

    private void updateUIForRecordingStart() {
        recordButton.setText("Stop Recording");
        flipButton.setEnabled(false);
        uploadButton.setEnabled(false);
    }

    private void stopRecording() {
        if (recording != null) {
            recording.stop();
            recording = null;
        }
        isRecording = false;
        recordingHandler.removeCallbacks(stopRecordingRunnable);
    }

    private void handleRecordingSuccess() {
        runOnUiThread(() -> {
            recordButton.setText("Start Recording");
            flipButton.setEnabled(true);
            uploadButton.setEnabled(true);
            startUploadingActivity(Uri.fromFile(outputFile));
        });
    }

    private void handleRecordingError(int errorCode) {
        runOnUiThread(() -> {
            showToast("Recording failed with error: " + errorCode);
            recordButton.setText("Start Recording");
            flipButton.setEnabled(true);
            uploadButton.setEnabled(true);
        });
    }

    private void flipCamera() {
        if (isRecording) {
            showToast("Cannot flip camera while recording");
            return;
        }
        currentLensFacing = (currentLensFacing == CameraSelector.LENS_FACING_BACK) ?
                CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
        bindCameraUseCases();
    }

    private void pickVideoFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_VIDEO},
                        REQUEST_GALLERY_PERMISSION);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_GALLERY_PERMISSION);
                return;
            }
        }

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setType("video/*");

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_GALLERY_PICK);
        } else {
            showToast("No video picker app found");
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_GALLERY_PICK && resultCode == RESULT_OK && data != null) {
            Uri videoUri = data.getData();
            if (videoUri == null) {
                showToast("Invalid video selection");
                return;
            }

            long duration = 0;
            try {
                duration = getVideoDuration(videoUri);
            } catch (IOException e) {
                showToast("Error reading video file");
                Log.e("CameraActivity", "Error reading video", e);
                return;
            }
            if (duration > 30_000) {
                showToast("Video exceeds 30 seconds");
                pickVideoFromGallery(); // Optionally, you might want to inform the user and let them choose again
            } else {
                startUploadingActivity(videoUri);
                showToast("Video selected successfully");
            }
        }
    }

    private long getVideoDuration(Uri videoUri) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try (AssetFileDescriptor afd = getContentResolver().openAssetFileDescriptor(videoUri, "r")) {
            retriever.setDataSource(afd.getFileDescriptor());
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return durationStr != null ? Long.parseLong(durationStr) : 0;
        } catch (Exception e) {
            Log.e("CameraActivity", "Error getting duration", e);
            return 0;
        } finally {
            retriever.release();
        }
    }

    private void startUploadingActivity(Uri videoUri) {
        Intent intent = new Intent(this, DescriptionVideoActivity.class)
                .putExtra("videoUri", videoUri.toString());
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                showToast("Camera & microphone permissions required");
                finish();
            }
        } else if (requestCode == REQUEST_GALLERY_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickVideoFromGallery();
            } else {
                showToast("Permission required to access videos");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        recordingHandler.removeCallbacks(stopRecordingRunnable);
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}