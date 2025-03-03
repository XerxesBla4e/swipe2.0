package com.example.e_sholpine.activity;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.e_sholpine.R;
import com.example.e_sholpine.helper.Validator;
import com.example.e_sholpine.model.Video;
import com.example.e_sholpine.model.VideoSummary;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DescriptionVideoActivity extends FragmentActivity implements View.OnClickListener {
    private EditText edtDescription;
    private Button btnDescription;
    private ImageView imvShortCutVideo;
    private final String REGEX_HASHTAG = "#([A-Za-z0-9_-]+)";
    private final long MAXIMUM_DURATION = 30000; // milliseconds

    private String username;
    private Uri videoUri;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private Validator validator;
    private ArrayList<String> hashtags;
    private String videoId;
    private Bitmap thumbnail;
    private final String TAG = "DescriptionVideoActivity";

    private Handler handler = new Handler(Looper.getMainLooper());
    private NotificationManagerCompat mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private ProgressDialog progressDialog; // Progress dialog for upload

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description_video);

        // Initialize UI components
        edtDescription = findViewById(R.id.edtDescription);
        btnDescription = findViewById(R.id.btnDescription);
        imvShortCutVideo = findViewById(R.id.imvShortCutVideo);

        validator = Validator.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        // Initialize ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading video...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false); // Prevent dismissal by tapping outside

        // Get video URI from intent
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle == null || !bundle.containsKey("videoUri")) {
            Toast.makeText(this, "No video selected", Toast.LENGTH_SHORT).show();
            moveToAnotherActivity(CameraActivity.class);
            return;
        }
        String videoPath = bundle.getString("videoUri");
        videoUri = Uri.parse(videoPath);
        hashtags = new ArrayList<>();

        // Initialize thumbnail and validate video
        if (!initializeVideoAndThumbnail()) {
            return; // Exit if initialization fails
        }

        // Request notification permission if needed
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        createNotificationChannel();
        setupNotification();

        btnDescription.setOnClickListener(this);
    }

    private boolean initializeVideoAndThumbnail() {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(getApplicationContext(), videoUri);
            String height = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String width = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long durationMs = Long.parseLong(duration);

            // Extract thumbnail
            thumbnail = mmr.getScaledFrameAtTime(1000000, MediaMetadataRetriever.OPTION_NEXT_SYNC, 1000, 1000);
            if (thumbnail == null) {
                Log.e(TAG, "Failed to generate thumbnail");
                Toast.makeText(this, "Failed to generate video thumbnail", Toast.LENGTH_SHORT).show();
                moveToAnotherActivity(CameraActivity.class);
                return false;
            }

            // Validate video properties
            if (!validator.isNumeric(height) || !validator.isNumeric(width)) {
                Toast.makeText(this, getString(R.string.error_undefined), Toast.LENGTH_SHORT).show();
                moveToAnotherActivity(CameraActivity.class);
                return false;
            } else if (durationMs > MAXIMUM_DURATION) {
                Toast.makeText(this, getString(R.string.error_upload_video), Toast.LENGTH_SHORT).show();
                moveToAnotherActivity(CameraActivity.class);
                return false;
            }

            imvShortCutVideo.setImageBitmap(thumbnail);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing video: " + e.getMessage(), e);
            Toast.makeText(this, "Invalid video file", Toast.LENGTH_SHORT).show();
            moveToAnotherActivity(CameraActivity.class);
            return false;
        } finally {
            try {
                mmr.release();
            } catch (IOException e) {
                Log.e(TAG, "Error releasing MediaMetadataRetriever", e);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("Video", "Video Uploads", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setSound(null, null);
            channel.setDescription("Notifications for video uploads");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void setupNotification() {
        mNotifyManager = NotificationManagerCompat.from(this);
        mBuilder = new NotificationCompat.Builder(this, "Video")
                .setContentTitle("Video uploading")
                .setContentText("Upload in progress")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_download);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == btnDescription.getId()) {
            if (thumbnail == null || videoUri == null) {
                Toast.makeText(this, "Cannot proceed: video or thumbnail not available", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show progress dialog
            progressDialog.setProgress(0);
            progressDialog.show();

            mBuilder.setProgress(100, 0, false);
            mNotifyManager.notify(0, mBuilder.build());

            // Extract hashtags
            Matcher matcher = Pattern.compile(REGEX_HASHTAG).matcher(edtDescription.getText().toString());
            while (matcher.find()) {
                hashtags.add(matcher.group(0));
            }
            videoId = String.valueOf(System.currentTimeMillis());

            // Start upload process
            uploadThumbnail();
            fetchUsernameAndUploadVideo();
        }
    }

    private void fetchUsernameAndUploadVideo() {
        DocumentReference docRef = db.collection("profiles").document(user.getUid());
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    username = document.getString("username");
                    if (videoUri != null) {
                        handler.post(this::uploadVideo);
                    }
                } else {
                    Log.d(TAG, "No such document");
                    Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            } else {
                Log.d(TAG, "Get failed with ", task.getException());
                Toast.makeText(this, "Failed to fetch profile", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        });
    }

    private String getFileType(Uri videoUri) {
        ContentResolver r = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String ext = mimeTypeMap.getExtensionFromMimeType(r.getType(videoUri));
        if (ext == null) {
            ext = mimeTypeMap.getFileExtensionFromUrl(videoUri.toString());
        }
        return ext != null ? ext : "mp4"; // Default to mp4 if unknown
    }

    private void uploadVideo() {
        String fileExtension = getFileType(videoUri);
        FirebaseStorage.getInstance().getReference("videos/" + videoId + "." + fileExtension)
                .putFile(videoUri)
                .addOnSuccessListener(taskSnapshot -> taskSnapshot.getStorage().getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String downloadUri = uri.toString();
                            String description = edtDescription.getText().toString().trim();
                            Video video = new Video(videoId, downloadUri, user.getUid(), username, description);
                            writeNewVideo(video);

                            mBuilder.setContentText("Upload complete").setProgress(0, 0, false);
                            mNotifyManager.notify(0, mBuilder.build());

                            if (!isFinishing()) {
                                progressDialog.dismiss();
                                Toast.makeText(this, "Upload successful", Toast.LENGTH_SHORT).show();
                                moveToAnotherActivity(HomeScreenActivity.class);
                            }
                        }))
                .addOnFailureListener(e -> {
                    if (!isFinishing()) {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Video upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    Log.e(TAG, "Video upload failed", e);
                })
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    progressDialog.setProgress((int) progress);
                    mBuilder.setProgress(100, (int) progress, false);
                    mNotifyManager.notify(0, mBuilder.build());
                });
    }

    private void uploadThumbnail() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        FirebaseStorage.getInstance().getReference("thumbnails/" + videoId + ".jpg")
                .putBytes(data)
                .addOnSuccessListener(taskSnapshot -> taskSnapshot.getStorage().getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String downloadUri = uri.toString();
                            VideoSummary videoSummary = new VideoSummary(videoId, downloadUri, 0L, hashtags);
                            writeNewVideoSummary(videoSummary);
                            writeHashtags(hashtags, videoSummary);
                            Log.i(TAG, "Thumbnail uploaded successfully");
                        }))
                .addOnFailureListener(e -> {
                    if (!isFinishing()) {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Thumbnail upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    Log.e(TAG, "Thumbnail upload failed", e);
                });
    }

    private void writeNewVideo(Video video) {
        db.collection("videos").document(video.getVideoId())
                .set(video.toMap())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Video document written successfully"))
                .addOnFailureListener(e -> Log.w(TAG, "Error writing video document", e));
    }

    private void writeNewVideoSummary(VideoSummary videoSummary) {
        db.collection("profiles").document(user.getUid())
                .collection("public_videos").document(videoSummary.getVideoId())
                .set(videoSummary.toMap())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Video summary written successfully"))
                .addOnFailureListener(e -> Log.w(TAG, "Error writing video summary", e));
    }

    private void writeHashtags(ArrayList<String> hashtags, VideoSummary videoSummary) {
        Map<String, Object> hashtagData = new HashMap<>();
        hashtagData.put("createdAt", FieldValue.serverTimestamp());

        for (String hashtag : hashtags) {
            db.collection("hashtags").document(hashtag)
                    .set(hashtagData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> db.collection("hashtags").document(hashtag)
                            .collection("video_summaries").document(videoId)
                            .set(videoSummary.toMap())
                            .addOnSuccessListener(unused -> Log.d(TAG, "Hashtag video summary written"))
                            .addOnFailureListener(e -> Log.w(TAG, "Error writing hashtag video summary", e)))
                    .addOnFailureListener(e -> Log.w(TAG, "Error writing hashtag document", e));
        }
    }

    private void moveToAnotherActivity(Class<?> cls) {
        Intent intent = new Intent(this, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}