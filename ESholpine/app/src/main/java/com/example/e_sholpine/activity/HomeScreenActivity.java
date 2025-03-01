package com.example.e_sholpine.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.e_sholpine.R;
import com.example.e_sholpine.fragment.InboxFragment;
import com.example.e_sholpine.fragment.ProfileFragment;
import com.example.e_sholpine.fragment.SearchFragment;
import com.example.e_sholpine.fragment.VideoFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class HomeScreenActivity extends FragmentActivity implements View.OnClickListener {

    private static final int SIGN_IN_REQUEST_CODE = 1;
    private static final String TAG = "HomeScreenActivity";
    private VideoFragment videoFragment;
    private SearchFragment searchFragment;
    private ProfileFragment profileFragment;
    private InboxFragment inboxFragment;
    private long pressedTime;
    private Button btnHome, btnAddVideo, btnInbox, btnProfile, btnSearch, btnReferEarn;
    private FirebaseUser user;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Intent fragmentIntent = null;
    private boolean openAppFromLink = false;
    private String phoneNumber;
    private DocumentReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_TikTokCloneProject);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);
        fragmentIntent = getIntent();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();

        btnHome = findViewById(R.id.btnHome);
        btnAddVideo = findViewById(R.id.btnAddVideo);
        btnInbox = findViewById(R.id.btnInbox);
        btnProfile = findViewById(R.id.btnProfile);
        btnSearch = findViewById(R.id.btnSearch);
        btnReferEarn = findViewById(R.id.btnReferandEarn);

        btnHome.setOnClickListener(this);
        btnAddVideo.setOnClickListener(this);
        btnInbox.setOnClickListener(this);
        btnProfile.setOnClickListener(this);
        btnSearch.setOnClickListener(this);
        btnReferEarn.setOnClickListener(this);

        setupInitialFragment();

        if (user != null) {
            String userId = user.getUid();
            userRef = db.collection("users").document(userId);
            getUserPhoneNumber();
            checkAndUpdateUserData();
        }
    }

    private void setupInitialFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (fragmentIntent.getExtras() != null) {
            if (fragmentIntent.hasExtra("id")) openAppFromLink = true;
            if (fragmentIntent.hasExtra("fragment_inbox")) {
                inboxFragment = InboxFragment.newInstance("inbox");
                ft.replace(R.id.main_fragment, inboxFragment);
            } else if (fragmentIntent.hasExtra("fragment_profile")) {
                profileFragment = ProfileFragment.newInstance("profile", "");
                ft.replace(R.id.main_fragment, profileFragment);
            } else if (fragmentIntent.hasExtra("fragment_search")) {
                searchFragment = SearchFragment.newInstance("search");
                ft.replace(R.id.main_fragment, searchFragment);
            } else {
                videoFragment = VideoFragment.newInstance("fragment_video");
                ft.replace(R.id.main_fragment, videoFragment);
            }
        } else {
            videoFragment = VideoFragment.newInstance("fragment_video");
            ft.replace(R.id.main_fragment, videoFragment);
        }
        ft.commit();
        Log.d(TAG, "Initial fragment setup completed");
    }

    @Override
    protected void onStart() {
        super.onStart();
        ensureVideoFragmentVisible();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ensureVideoFragmentVisible();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopVideoFragment();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopVideoFragment();
    }

    @Override
    public void onBackPressed() {
        if (pressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
            finish();
        } else {
            Toast.makeText(getBaseContext(), "Press back again to exit", Toast.LENGTH_SHORT).show();
        }
        pressedTime = System.currentTimeMillis();
    }

    @Override
    public void onClick(View view) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        int position = -1;

        if (view.getId() == btnHome.getId()) {
            position = 0;
            if (videoFragment == null) {
                videoFragment = VideoFragment.newInstance("fragment_video");
                ft.add(R.id.main_fragment, videoFragment);
            }
        } else if (view.getId() == btnSearch.getId()) {
            position = 1;
            if (searchFragment == null) {
                searchFragment = SearchFragment.newInstance("search");
                ft.add(R.id.main_fragment, searchFragment);
            }
        } else if (view.getId() == btnInbox.getId()) {
            handleInboxClick();
            return;
        } else if (view.getId() == btnProfile.getId()) {
            handleProfileClick();
            return;
        } else if (view.getId() == btnAddVideo.getId()) {
            handleAddClick();
            return;
        } else if (view.getId() == btnReferEarn.getId()) {
            initiateReferralFlow();
            return;
        }

        if (position >= 0) {
            showFragments(ft, position);
            ft.commit();
        }
    }

    private void handleProfileClick() {
        if (user == null) {
            Intent intent = new Intent(this, SignupChoiceActivity.class);
            startActivityForResult(intent, SIGN_IN_REQUEST_CODE);
            return;
        }
        if (openAppFromLink) {
            Intent intent = new Intent(this, EditProfileActivity.class);
            intent.putExtras(fragmentIntent.getExtras());
            startActivity(intent);
        } else {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            if (profileFragment == null) {
                profileFragment = ProfileFragment.newInstance("profile", "");
                ft.add(R.id.main_fragment, profileFragment);
            }
            showFragments(ft, 3);
            ft.commit();
        }
    }

    private void handleAddClick() {
        if (user == null) {
            showNiceDialogBox(this, null, null);
            return;
        }
        stopVideoFragment();
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_right_to_left, R.anim.fade_in);
    }

    private void handleInboxClick() {
        if (user == null) {
            showNiceDialogBox(this, null, null);
            return;
        }
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (inboxFragment == null) {
            inboxFragment = InboxFragment.newInstance("inbox");
            ft.add(R.id.main_fragment, inboxFragment);
        }
        showFragments(ft, 2);
        ft.commit();
    }

    private void initiateReferralFlow() {
        if (user == null) {
            showNiceDialogBox(this, null, "Sign in to join the referral system.");
            return;
        }
        String userId = user.getUid();

        // Check for deep link with referral code
        Intent intent = getIntent();
        Uri data = intent.getData();
        Intent referralIntent = new Intent(this, ReferralActivity.class);
        if (data != null && data.getQueryParameter("code") != null) {
            String referralCode = data.getQueryParameter("code");
            Log.d(TAG, "Deep link detected with referral code: " + referralCode);
            referralIntent.putExtra("referralCode", referralCode);
        }

        // Always redirect to ReferralActivity
        startActivity(referralIntent);
    }

    private void promptPhoneNumberUpdate(String userId, boolean forReferral) {
        new AlertDialog.Builder(this)
                .setTitle("Update Phone Number")
                .setMessage("Please update your phone number to proceed with the referral system.")
                .setPositiveButton("Update", (dialog, which) -> {
                    Intent intent = new Intent(this, EditProfileActivity.class);
                    intent.putExtra("userId", userId);
                    intent.putExtra("forReferral", forReferral);
                    startActivityForResult(intent, SIGN_IN_REQUEST_CODE);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SIGN_IN_REQUEST_CODE && resultCode == RESULT_OK) {
            user = mAuth.getCurrentUser();
            if (user != null) {
                String userId = user.getUid();
                userRef = db.collection("users").document(userId);
                userRef.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        phoneNumber = task.getResult().getString("phone");
                        Log.d(TAG, "Phone number after sign-in: " + phoneNumber);
                        ensureVideoFragmentVisible();
                        checkAndUpdateUserData();
                    }
                });
            }
        }
    }

    private void getUserPhoneNumber() {
        if (user != null) {
            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        phoneNumber = document.getString("phone");
                        Log.d(TAG, "Retrieved phone number: " + phoneNumber);
                        if (phoneNumber == null || phoneNumber.isEmpty()) {
                            showError("Phone number not found. Please update your profile.");
                        }
                    } else {
                        showError("User document does not exist.");
                    }
                } else {
                    showError("Failed to retrieve user data: " + task.getException().getMessage());
                }
            });
        } else {
            Log.e(TAG, "User is null, cannot retrieve phone number");
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.e(TAG, message);
    }

    private void checkAndUpdateUserData() {
        if (user != null) {
            String userId = user.getUid();
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (!documentSnapshot.exists()) {
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("hasLoggedInBefore", true);
                            userData.put("points", 0L);
                            userData.put("joinedReferral", false);
                            db.collection("users").document(userId).set(userData);
                        } else {
                            Map<String, Object> updates = new HashMap<>();
                            if (!documentSnapshot.contains("joinedReferral")) updates.put("joinedReferral", false);
                            if (!documentSnapshot.contains("points")) updates.put("points", 0L);
                            if (!updates.isEmpty()) {
                                db.collection("users").document(userId).set(updates, SetOptions.merge());
                            }
                        }
                    });
        }
    }

    private void showNiceDialogBox(Context context, @Nullable String title, @Nullable String message) {
        if (title == null) title = getString(R.string.request_account_title);
        if (message == null) message = getString(R.string.request_account_message);
        new AlertDialog.Builder(context, R.style.AlertDialogTheme)
                .setIcon(R.drawable.splash_background)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Cancel", (dialogInterface, i) -> {})
                .setPositiveButton("Sign up/Sign in", (dialog, whichOne) -> {
                    Intent intent = new Intent(context, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivityForResult(intent, SIGN_IN_REQUEST_CODE);
                })
                .show();
    }

    private void showFragments(FragmentTransaction ft, int position) {
        if (videoFragment != null && position != 0) ft.hide(videoFragment);
        if (searchFragment != null && position != 1) ft.hide(searchFragment);
        if (inboxFragment != null && position != 2) ft.hide(inboxFragment);
        if (profileFragment != null && position != 3) ft.hide(profileFragment);

        switch (position) {
            case 0:
                if (videoFragment != null) {
                    ft.show(videoFragment);
                    continueVideoFragment();
                }
                break;
            case 1:
                if (searchFragment != null) ft.show(searchFragment);
                break;
            case 2:
                if (inboxFragment != null) ft.show(inboxFragment);
                break;
            case 3:
                if (profileFragment != null) ft.show(profileFragment);
                break;
        }
        Log.d(TAG, "Showing fragment at position: " + position);
    }

    public void stopVideoFragment() {
        if (videoFragment != null) {
            videoFragment.pauseVideo();
            Log.d(TAG, "Video fragment stopped");
        }
    }

    public void continueVideoFragment() {
        if (videoFragment != null) {
            videoFragment.continueVideo();
            Log.d(TAG, "Video fragment continued");
        }
    }

    private void ensureVideoFragmentVisible() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (videoFragment == null || !videoFragment.isVisible()) {
            videoFragment = VideoFragment.newInstance("fragment_video");
            ft.replace(R.id.main_fragment, videoFragment);
            ft.commit();
        } else {
            showFragments(ft, 0);
            ft.commit();
        }
        continueVideoFragment();
        Log.d(TAG, "Ensuring VideoFragment is visible");
    }
}
