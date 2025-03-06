package com.example.e_sholpine.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.e_sholpine.R;
import com.example.e_sholpine.adapters.VideoAdapter;
import com.example.e_sholpine.model.Video;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class VideoFragment extends Fragment implements View.OnClickListener {
    private Context context = null;
    private TextView tvVideo;
    private ViewPager2 viewPager2;
    public ArrayList<Video> videos;
    public VideoAdapter videoAdapter;
    FirebaseAuth mAuth;
    FirebaseUser user;
    FirebaseFirestore db;
    Uri videoUri;
    private static final String TAG = "VideoFragment";

    public static VideoFragment newInstance(String strArg) {
        VideoFragment fragment = new VideoFragment();
        Bundle args = new Bundle();
        args.putString("name", strArg);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            context = getActivity();
        } catch (IllegalStateException e) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseVideo();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_video, null);
        tvVideo = layout.findViewById(R.id.tvVideo);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        viewPager2 = layout.findViewById(R.id.viewPager);
        videos = new ArrayList<>();
        videoAdapter = new VideoAdapter(context, videos);
        VideoAdapter.setUser(user);
        viewPager2.setAdapter(videoAdapter);
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                videoAdapter.pauseVideo(videoAdapter.getCurrentPosition());
                videoAdapter.playVideo(position);
                videoAdapter.updateWatchCount(position);
                Log.e("Selected_Page", String.valueOf(videoAdapter.getCurrentPosition()));
                videoAdapter.updateCurrentPosition(position);

                // Preload next video
                int nextPosition = position + 1;
                if (nextPosition < videos.size()) {
                    videoAdapter.playVideo(nextPosition); // Prepare and start loading
                    videoAdapter.pauseVideo(nextPosition); // Pause immediately
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });
        viewPager2.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {
            }

            @Override
            public void onViewDetachedFromWindow(View view) {
                videoAdapter.stopVideo(videoAdapter.getCurrentPosition());
                Log.d(TAG, "View detached, stopping video at position: " + videoAdapter.getCurrentPosition());
            }
        });

        loadVideos();
        return layout;
    }

    @Override
    public void onStart() {
        super.onStart();
        continueVideo();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopVideo();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopVideo();
        viewPager2.setAdapter(null);
    }

    @Override
    public void onClick(View view) {
    }

    public void pauseVideo() {
        SharedPreferences currentPosPref = context.getSharedPreferences("position", Context.MODE_PRIVATE);
        SharedPreferences.Editor positionEditor = currentPosPref.edit();
        int currentPosition = videoAdapter.getCurrentPosition();
        videoAdapter.pauseVideo(currentPosition);
        positionEditor.putInt("position", currentPosition);
        positionEditor.apply();
        Log.d(TAG, "Paused video at position: " + currentPosition);
    }

    public void continueVideo() {
        SharedPreferences currentPosPref = context.getSharedPreferences("position", Context.MODE_PRIVATE);
        int currentPosition = currentPosPref.getInt("position", -1);
        if (currentPosition != -1) {
            videoAdapter.playVideo(currentPosition);
            Log.d(TAG, "Continued video at position: " + currentPosition);
        }
    }

    public void stopVideo() {
        int currentPosition = videoAdapter.getCurrentPosition();
        videoAdapter.stopVideo(currentPosition);
        Log.d(TAG, "Stopped video at position: " + currentPosition);
    }

    private void loadVideos() {
        db.collection("videos")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "listen:error", e);
                            return;
                        }

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            switch (dc.getType()) {
                                case ADDED:
                                    Video video = dc.getDocument().toObject(Video.class);
                                    videos.add(0, video);
                                    videoAdapter.notifyItemInserted(0);
                                    break;
                                case MODIFIED:
                                    Log.d(TAG, "Modified city: " + dc.getDocument().getData());
                                    break;
                                case REMOVED:
                                    Log.d(TAG, "Removed city: " + dc.getDocument().getData());
                                    break;
                            }
                        }
                    }
                });
    }
}
