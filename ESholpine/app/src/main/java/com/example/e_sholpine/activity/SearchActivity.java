package com.example.e_sholpine.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.e_sholpine.R;
import com.example.e_sholpine.WrapContentLinearLayoutManager;
import com.example.e_sholpine.adapters.UserAdapter;
import com.example.e_sholpine.adapters.VideoSummaryAdapter;
import com.example.e_sholpine.model.User;
import com.example.e_sholpine.model.VideoSummary;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class SearchActivity extends Activity implements View.OnClickListener {

    final String USERNAME_LABEL = "username";
    RecyclerView rcv_users;
    UserAdapter userAdapter;
    SearchView searchView;
    ArrayList<VideoSummary> videoSummaries;
    VideoSummaryAdapter videoSummaryAdapter;
    RecyclerView rcvVideoSummary;
    ImageButton imbBackToHome;
    TextView tvSubmitSearch;
    final String TAG = "SearchActivity";
    ArrayList<User> userArrayList = new ArrayList<>();
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searching);

        db = FirebaseFirestore.getInstance();

        tvSubmitSearch = findViewById(R.id.tvSubmitSearch);
        // imbBackToHome = findViewById(R.id.imbBackToHome);
        rcv_users = findViewById(R.id.rcv_users);
        rcvVideoSummary = findViewById(R.id.rcvVideoSummary);
        searchView = findViewById(R.id.searchView);

        rcv_users.setLayoutManager(new WrapContentLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        userAdapter = new UserAdapter(this, userArrayList);
        rcv_users.setAdapter(userAdapter);
        rcv_users.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        videoSummaries = new ArrayList<>();
        videoSummaryAdapter = new VideoSummaryAdapter(this, videoSummaries);
        rcvVideoSummary.setLayoutManager(gridLayoutManager);
        rcvVideoSummary.addItemDecoration(new GridSpacingItemDecoration(3, 10, true));
        rcvVideoSummary.setAdapter(videoSummaryAdapter);

        // imbBackToHome.setOnClickListener(this);
        tvSubmitSearch.setOnClickListener(this);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                handleSearch(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                handleSearch(newText);
                return false;
            }
        });
    }

    private void handleSearch(String query) {
        if (!query.isEmpty()) {
            if (query.startsWith("#")) {
                String hashtag = query.toLowerCase(); // Normalize to lowercase
                setVideoSummaries(hashtag);
                rcv_users.setVisibility(View.GONE);
                rcvVideoSummary.setVisibility(View.VISIBLE);
            } else {
                getData(query);
                rcvVideoSummary.setVisibility(View.GONE);
                rcv_users.setVisibility(View.VISIBLE);
            }
        } else {
            userArrayList.clear();
            userAdapter.notifyDataSetChanged();
            videoSummaries.clear();
            videoSummaryAdapter.notifyDataSetChanged();
            rcvVideoSummary.setVisibility(View.GONE);
            rcv_users.setVisibility(View.VISIBLE);
        }
    }

    private void getData(String key) {
        userArrayList.clear();
        db.collection("users")
                .orderBy(USERNAME_LABEL)
                .startAt(key)
                .endAt(key + "\uf8ff")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot snapshot = task.getResult();
                            Log.d(TAG, "User search returned " + snapshot.size() + " results");
                            for (QueryDocumentSnapshot document : snapshot) {
                                User user = new User(document.getString("userId"), document.getString(USERNAME_LABEL));
                                userArrayList.add(user);
                                Log.d(TAG, "Found user: " + user.getUserName());
                            }
                            userAdapter.notifyDataSetChanged();
                            if (userArrayList.isEmpty()) {
                                Toast.makeText(SearchActivity.this, "No users found", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "Error getting users: ", task.getException());
                            Toast.makeText(SearchActivity.this, "Error searching users!", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void setVideoSummaries(String hashtag) {
        Log.d(TAG, "Searching videos for hashtag: " + hashtag);
        db.collection("hashtags")
                .document(hashtag)
                .collection("video_summaries")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot snapshot = task.getResult();
                            videoSummaries.clear();
                            Log.d(TAG, "Video search returned " + snapshot.size() + " results");

                            if (snapshot.isEmpty()) {
                                Toast.makeText(SearchActivity.this, "No videos found for " + hashtag, Toast.LENGTH_SHORT).show();
                            }

                            for (QueryDocumentSnapshot document : snapshot) {
                                try {
                                    VideoSummary summary = document.toObject(VideoSummary.class);
                                    videoSummaries.add(summary);
                                    Log.d(TAG, "Found video - ID: " + summary.getVideoId() +
                                            ", Thumbnail: " + summary.getThumbnailUri() +
                                            ", WatchCount: " + summary.getWatchCount());
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing video summary: " + document.getId(), e);
                                }
                            }
                            videoSummaryAdapter.notifyDataSetChanged();

                            if (videoSummaries.isEmpty() && !snapshot.isEmpty()) {
                                Toast.makeText(SearchActivity.this,
                                        "Videos found but failed to load - check data format",
                                        Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.e(TAG, "Error getting video summaries: ", task.getException());
                            Toast.makeText(SearchActivity.this,
                                    "Error searching videos: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == tvSubmitSearch.getId()) {
            searchView.clearFocus();
        }
    }

    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;
                if (position < spanCount) {
                    outRect.top = spacing;
                }
                outRect.bottom = spacing;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;
                if (position >= spanCount) {
                    outRect.top = spacing;
                }
            }
        }
    }
}