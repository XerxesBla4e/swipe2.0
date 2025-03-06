package com.example.e_sholpine.adapters;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.e_sholpine.R;
import com.example.e_sholpine.activity.CommentActivity;
import com.example.e_sholpine.activity.DeleteVideoSettingActivity;
import com.example.e_sholpine.activity.MainActivity;
import com.example.e_sholpine.activity.ProfileActivity;
import com.example.e_sholpine.helper.OnSwipeTouchListener;
import com.example.e_sholpine.helper.StaticVariable;
import com.example.e_sholpine.model.Notification;
import com.example.e_sholpine.model.Video;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private List<Video> videos;
    private Context context;
    private static FirebaseUser user = null;
    private List<VideoViewHolder> videoViewHolders;
    private int currentPosition;
    int numberOfClick = 0;
    float volume;
    boolean isPlaying = true;
    public String videoUri;
    private static final String TAG = "VideoAdapter";
    private static SimpleCache cache; // Shared cache instance

    public VideoAdapter(Context context, List<Video> videos) {
        this.context = context;
        this.videos = videos;
        videoViewHolders = new ArrayList<>();
        currentPosition = 0;

        // Initialize cache
        if (cache == null) {
            File cacheDir = new File(context.getCacheDir(), "media_cache");
            cache = new SimpleCache(cacheDir, new LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024)); // 100MB cache
        }
    }

    public static void setUser(FirebaseUser user) {
        VideoAdapter.user = user;
    }

    public void addVideoObject(Video video) {
        this.videos.add(video);
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VideoViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.video_container, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        holder.setVideoObjects(videos.get(position));
        videoViewHolders.add(position, holder);

        // Preload next video
        int nextPosition = position + 1;
        if (nextPosition < videos.size() && nextPosition < videoViewHolders.size()) {
            VideoViewHolder nextHolder = videoViewHolders.get(nextPosition);
            if (nextHolder != null && nextHolder.exoPlayer != null) {
                MediaItem nextMediaItem = MediaItem.fromUri(videos.get(nextPosition).getVideoUri());
                nextHolder.exoPlayer.setMediaItem(nextMediaItem);
                nextHolder.exoPlayer.prepare();
                nextHolder.exoPlayer.setPlayWhenReady(false);
            }
        }
    }

    public void updateCurrentPosition(int pos) {
        currentPosition = pos;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void pauseVideo(int position) {
        if (position >= 0 && position < videoViewHolders.size()) {
            videoViewHolders.get(position).pauseVideo();
            Log.d(TAG, "Paused video at position: " + position);
        }
    }

    public void playVideo(int position) {
        if (position >= 0 && position < videoViewHolders.size()) {
            videoViewHolders.get(position).playVideo();
            Log.d(TAG, "Playing video at position: " + position);
        }
    }

    public void stopVideo(int position) {
        if (position >= 0 && position < videoViewHolders.size()) {
            videoViewHolders.get(position).stopVideo();
            Log.d(TAG, "Stopped video at position: " + position);
        }
    }

    public void updateWatchCount(int position) {
        if (position >= 0 && position < videoViewHolders.size()) {
            videoViewHolders.get(position).updateWatchCount();
        }
    }

    @Override
    public void onViewAttachedToWindow(VideoViewHolder holder) {
        holder.playVideo();
        isPlaying = true;
    }

    @Override
    public void onViewDetachedFromWindow(VideoViewHolder holder) {
        holder.stopVideo();
        isPlaying = false;
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    public class VideoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        StyledPlayerView videoView;
        ExoPlayer exoPlayer;
        ImageView imvAvatar, imvPause, imvMore, imvAppear, imvVolume, imvShare, imvDownload;
        TextView txvDescription, tvTitle;
        TextView tvComment, tvFavorites;
        ProgressBar pgbWait;
        String authorId;
        String videoId;
        int totalLikes;
        int totalComments;
        DocumentReference docRef;
        FirebaseFirestore db;
        final String LIKE_COLLECTION = "likes";
        String userId;
        boolean isPaused = false;
        boolean isLiked = false;
        String videoUri; // Store video URI locally

        Handler handler = new Handler();

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.videoView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            txvDescription = itemView.findViewById(R.id.txvDescription);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvFavorites = itemView.findViewById(R.id.tvFavorites);
            imvAvatar = itemView.findViewById(R.id.imvAvatar);
            imvPause = itemView.findViewById(R.id.imvPause);
            imvMore = itemView.findViewById(R.id.imvMore);
            imvAppear = itemView.findViewById(R.id.imv_appear);
            imvVolume = itemView.findViewById(R.id.imvVolume);
            imvShare = itemView.findViewById(R.id.imvShare);
            imvDownload = itemView.findViewById(R.id.imvDownload);
            db = FirebaseFirestore.getInstance();

            videoView.setOnClickListener(this);
            imvAvatar.setOnClickListener(this);
            tvTitle.setOnClickListener(this);
            tvComment.setOnClickListener(this);
            imvMore.setOnClickListener(this);
            tvFavorites.setOnClickListener(this);
            imvVolume.setOnClickListener(this);
            imvShare.setOnClickListener(this);
            imvDownload.setOnClickListener(this);

            videoView.setOnTouchListener(new OnSwipeTouchListener(itemView.getContext()) {
                @Override
                public void onSwipeLeft() {
                    moveToProfile(videoView.getContext(), authorId);
                }
            });
        }

        public void playVideo() {
            if (exoPlayer == null) {
                exoPlayer = new ExoPlayer.Builder(videoView.getContext())
                        .setLoadControl(new com.google.android.exoplayer2.DefaultLoadControl.Builder()
                                .setBufferDurationsMs(2000, 5000, 500, 1000)
                                .build())
                        .build();
                videoView.setPlayer(exoPlayer);
                DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(videoView.getContext());
                CacheDataSource.Factory cacheDataSourceFactory = new CacheDataSource.Factory()
                        .setCache(cache)
                        .setUpstreamDataSourceFactory(dataSourceFactory);
                MediaItem mediaItem = MediaItem.fromUri(videoUri);
                exoPlayer.setMediaSource(new ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                        .createMediaSource(mediaItem));
                exoPlayer.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE);
                exoPlayer.prepare();
            }
            if (!exoPlayer.isPlaying() && exoPlayer.getPlaybackState() != Player.STATE_ENDED) {
                exoPlayer.setPlayWhenReady(true);
                exoPlayer.play();
            }
        }

        public void pauseVideo() {
            if (exoPlayer != null && exoPlayer.isPlaying()) {
                exoPlayer.setPlayWhenReady(false);
                isPaused = true;
            }
        }

        public void stopVideo() {
            if (exoPlayer != null) {
                exoPlayer.setPlayWhenReady(false);
                exoPlayer.stop();
                exoPlayer.release();
                exoPlayer = null;
                isPaused = true;
            }
        }

        public void appearImage(int src) {
            imvAppear.setImageResource(src);
            imvAppear.setVisibility(View.VISIBLE);
            new Handler().postDelayed(() -> imvAppear.setVisibility(View.GONE), 1000);
        }

        @SuppressLint("ClickableViewAccessibility")
        public void setVideoObjects(final Video videoObject) {
            tvTitle.setText("@" + videoObject.getUsername());
            txvDescription.setText(videoObject.getDescription());
            tvComment.setText(String.valueOf(videoObject.getTotalComments()));

            this.videoUri = videoObject.getVideoUri();
            MediaItem mediaItem = MediaItem.fromUri(videoUri);

            DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(videoView.getContext());
            CacheDataSource.Factory cacheDataSourceFactory = new CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(dataSourceFactory);

            if (exoPlayer == null) {
                exoPlayer = new ExoPlayer.Builder(videoView.getContext())
                        .setLoadControl(new com.google.android.exoplayer2.DefaultLoadControl.Builder()
                                .setBufferDurationsMs(2000, 5000, 500, 1000)
                                .build())
                        .build();
                videoView.setPlayer(exoPlayer);
            }
            exoPlayer.clearMediaItems();
            exoPlayer.setMediaSource(new ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                    .createMediaSource(mediaItem));
            exoPlayer.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE);
            exoPlayer.prepare();
            pauseVideo();
            isPaused = true;

            authorId = videoObject.getAuthorId();
            videoId = videoObject.getVideoId();
            totalComments = videoObject.getTotalComments();
            totalLikes = videoObject.getTotalLikes();
            userId = user == null ? "" : user.getUid();

            docRef = db.collection(LIKE_COLLECTION).document(videoId);

            if (!userId.isEmpty()) {
                setLikes(videoId, userId);
            }

            showAvt(imvAvatar, videoObject.getAuthorId());

            db.collection("videos").document(videoId)
                    .addSnapshotListener((document, e) -> {
                        if (e != null) {
                            Log.w(TAG, "listen:error", e);
                            return;
                        }
                        Integer totalLikes = document.get("totalLikes", Integer.class);
                        Integer totalComments = document.get("totalComments", Integer.class);
                        tvFavorites.setText(String.valueOf(totalLikes));
                        tvComment.setText(String.valueOf(totalComments));
                    });

            db.collection("profiles").document(authorId)
                    .addSnapshotListener((document, e) -> {
                        if (e != null) {
                            Log.w(TAG, "listen:error", e);
                            return;
                        }
                        tvTitle.setText("@" + document.get("username", String.class));
                    });

            if (!userId.equals(authorId)) {
                imvMore.setVisibility(View.GONE);
            }
        }

        @Override
        public void onClick(View view) {
            if (view.getId() == imvAvatar.getId() || view.getId() == tvTitle.getId()) {
                moveToProfile(videoView.getContext(), authorId);
                return;
            }

            if (view.getId() == tvComment.getId()) {
                if (user == null) {
                    showNiceDialogBox(view.getContext(), null, null);
                    return;
                }
                Intent intent = new Intent(view.getContext(), CommentActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("videoId", videoId);
                bundle.putString("authorId", authorId);
                bundle.putInt("totalComments", totalComments);
                intent.putExtras(bundle);
                view.getContext().startActivity(intent);
                return;
            }

            if (view.getId() == imvDownload.getId()) {
                downloadVideo();
            }

            if (view.getId() == imvMore.getId()) {
                if (authorId.equals(user.getUid())) {
                    Intent intent = new Intent(view.getContext(), DeleteVideoSettingActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("videoId", videoId);
                    bundle.putString("authorId", authorId);
                    intent.putExtras(bundle);
                    view.getContext().startActivity(intent);
                } else {
                    moveToProfile(videoView.getContext(), authorId);
                }
                return;
            }

            if (view.getId() == tvFavorites.getId()) {
                handleTymClick(view);
                return;
            }

            if (view.getId() == videoView.getId()) {
                numberOfClick++;
                float currentVolume = exoPlayer != null ? exoPlayer.getVolume() : 0;
                boolean isMuted = (currentVolume == 0);
                handler.postDelayed(() -> {
                    if (numberOfClick == 1) {
                        if (isPlaying) {
                            pauseVideo();
                            isPlaying = false;
                            imvAppear.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                            imvAppear.setVisibility(View.VISIBLE);
                        } else {
                            playVideo();
                            isPlaying = true;
                            imvAppear.setVisibility(View.GONE);
                        }
                    } else if (numberOfClick == 2) {
                        handleTymClick(view);
                        imvAppear.setVisibility(View.GONE);
                        appearImage(R.drawable.ic_fill_favorite);
                    }
                    numberOfClick = 0;
                }, 500);
            }

            if (view.getId() == imvVolume.getId()) {
                float currentVolume = exoPlayer != null ? exoPlayer.getVolume() : 0;
                boolean isMuted = (currentVolume == 0);
                if (isMuted) {
                    if (exoPlayer != null) {
                        exoPlayer.setVolume(volume);
                    }
                    imvVolume.setImageResource(R.drawable.ic_baseline_volume_up_24);
                    appearImage(R.drawable.ic_baseline_volume_up_24);
                } else {
                    volume = exoPlayer != null ? exoPlayer.getVolume() : 1.0f;
                    if (exoPlayer != null) {
                        exoPlayer.setVolume(0);
                    }
                    imvVolume.setImageResource(R.drawable.ic_baseline_volume_off_24);
                    appearImage(R.drawable.ic_baseline_volume_off_24);
                }
            }

            if (view.getId() == imvShare.getId()) {
                shareVideoLink();
            }
        }

        public void updateWatchCount() {
            db.collection("profiles").document(authorId)
                    .collection("public_videos").document(videoId).update("watchCount", FieldValue.increment(1));
            final String REGEX_HASHTAG = "#([A-Za-z0-9_-]+)";
            Matcher matcher = Pattern.compile(REGEX_HASHTAG).matcher(txvDescription.getText().toString());
            while (matcher.find()) {
                String hashtag = matcher.group(0);
                db.collection("hashtags").document(hashtag).collection("video_summaries")
                        .document(videoId).update("watchCount", FieldValue.increment(1));
            }
        }

        private void shareVideoLink() {
            Log.d(TAG, "Attempting to share video with URI: " + videoUri);
            if (videoUri.startsWith("http")) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, videoUri);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this video!");
                context.startActivity(Intent.createChooser(shareIntent, "Share video via"));
                Log.d(TAG, "Sharing direct URL: " + videoUri);
            } else {
                StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(videoUri);
                storageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String downloadUrl = uri.toString();
                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("text/plain");
                            shareIntent.putExtra(Intent.EXTRA_TEXT, downloadUrl);
                            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this video!");
                            context.startActivity(Intent.createChooser(shareIntent, "Share video via"));
                            Log.d(TAG, "Sharing download URL: " + downloadUrl);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to get download URL: ", e);
                            Toast.makeText(context, "Failed to share video: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        }

        private void notifyLike() {
            db.collection("users").document(user.getUid())
                    .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            String username = task.getResult().getString("username");
                            Notification.pushNotification(username, authorId, StaticVariable.LIKE);
                        }
                    });
        }

        private void moveToProfile(Context context, String authorId) {
            isPlaying = false;
            Intent intent = new Intent(context, ProfileActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("id", authorId);
            intent.putExtras(bundle);
            context.startActivity(intent);
        }

        private void downloadVideo() {
            DownloadManager.Request request = new DownloadManager.Request(android.net.Uri.parse(videoUri));
            request.setTitle("Downloading Video");
            request.setDescription("Your video is being downloaded");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "video_" + System.currentTimeMillis() + ".mp4");

            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                downloadManager.enqueue(request);
                Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Download failed: DownloadManager not available", Toast.LENGTH_SHORT).show();
            }
        }

        private void showAvt(ImageView imv, String authorId) {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference().child("/user_avatars/" + authorId);
            storageRef.getBytes(StaticVariable.MAX_BYTES_AVATAR)
                    .addOnSuccessListener(bytes -> {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        imv.setImageBitmap(bitmap);
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to load avatar", e));
        }

        private void setLikes(String videoId, String userId) {
            docRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    if (task.getResult().contains(userId)) {
                        setFillLiked(true);
                        isLiked = true;
                    } else {
                        setFillLiked(false);
                        isLiked = false;
                    }
                } else {
                    setFillLiked(false);
                    isLiked = false;
                }
            });
        }

        private void handleTymClick(View view) {
            if (user == null) {
                showNiceDialogBox(view.getContext(), null, null);
                return;
            }

            setFillLiked(!isLiked);

            docRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists() && document.contains(userId)) {
                        docRef.update(userId, FieldValue.delete());
                    } else {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put(userId, null);
                        docRef.set(updates, com.google.firebase.firestore.SetOptions.merge());
                        notifyLike();
                    }
                }
            });

            totalLikes = isLiked ? totalLikes - 1 : totalLikes + 1;
            isLiked = !isLiked;
            updateTotalLike(totalLikes);
        }

        private void updateTotalLike(int totalLikes) {
            db.collection("videos").document(videoId).update("totalLikes", totalLikes);
        }

        private void setFillLiked(boolean isLiked) {
            tvFavorites.setCompoundDrawablesWithIntrinsicBounds(0, isLiked ? R.drawable.ic_fill_favorite : R.drawable.ic_favorite, 0, 0);
            tvFavorites.setText(String.valueOf(totalLikes));
        }

        public void showNiceDialogBox(Context context, @Nullable String title, @Nullable String message) {
            if (title == null) title = context.getString(R.string.request_account_title);
            if (message == null) message = context.getString(R.string.request_account_message);
            new AlertDialog.Builder(context, R.style.AlertDialogTheme)
                    .setIcon(R.drawable.swipe)
                    .setTitle(title)
                    .setMessage(message)
                    .setNegativeButton("Cancel", (dialog, which) -> {})
                    .setPositiveButton("Sign up/Sign in", (dialog, which) -> {
                        Intent intent = new Intent(context, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        context.startActivity(intent);
                    })
                    .show();
        }
    }
}
