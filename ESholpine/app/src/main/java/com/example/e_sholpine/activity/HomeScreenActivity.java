package com.example.e_sholpine.activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class HomeScreenActivity extends FragmentActivity implements View.OnClickListener {

    private static final int SIGN_IN_REQUEST_CODE = 1;
    private static final String TAG = "NavigationFragment";
    private static final String DEADLINE_DATE = "2025-03-07"; // Updated deadline to March 7, 2025
    VideoFragment videoFragment;
    SearchFragment searchFragment;
    ProfileFragment profileFragment;
    InboxFragment inboxFragment;
    private long pressedTime;
    private Button btnHome, btnAddVideo, btnInbox, btnProfile, btnSearch, btnReferEarn;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private Intent fragmentIntent = null;
    private boolean openAppFromLink = false;
    public boolean isAppUsable = true; // Cached usability status for deadline
    private String phoneNumber;
    private DocumentReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_TikTokCloneProject);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);
        fragmentIntent = getIntent();

        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

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

        if (user != null) {
            String userId = user.getUid();
            userRef = db.collection("users").document(userId);
            getUserPhoneNumber();
            checkAndUpdateUserData();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        checkDeadline(); // Check deadline on start
    }

    private void checkDeadline() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Use UTC for consistency

            Date deadlineDate = sdf.parse(DEADLINE_DATE);
            Date currentDate = sdf.parse(sdf.format(new Date()));

            Log.d(TAG, "Deadline Date: " + deadlineDate);
            Log.d(TAG, "Current Date: " + currentDate);
            Log.d(TAG, "Is Current Date Before Deadline? " + currentDate.before(deadlineDate));

            isAppUsable = currentDate.before(deadlineDate);

            if (!isAppUsable) {
                showPaymentDialog();
            } else {
                setupInitialFragment(); // Load UI only if before deadline
                ensureVideoFragmentVisible();
            }
        } catch (Exception e) {
            e.printStackTrace();
            isAppUsable = false; // Default to unusable on error
            //showPaymentDialog();
        }
    }

    private void showPaymentDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Payment Deadline Reached")
                .setMessage("The development payment deadline (Mar 7, 2025) has passed. Please complete the payment to continue using the app.")
                .setCancelable(false)
                .setPositiveButton("Contact Us", (dialog, which) -> {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                    emailIntent.setData(Uri.parse("mailto:edgarmugisha22@gmail.com"));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Development Payment for Swipe App");
                    emailIntent.putExtra(Intent.EXTRA_TEXT, "Please provide payment details for the Swipe app development.");
                    try {
                        startActivity(emailIntent);
                    } catch (Exception e) {
                        Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Exit", (dialog, which) -> finishAffinity())
                .setOnDismissListener(dialog -> finishAffinity())
                .show();
    }

    private void setupInitialFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (fragmentIntent.getExtras() != null) {
            if (fragmentIntent.hasExtra("id")) openAppFromLink = true;
            if (fragmentIntent.hasExtra("fragment_inbox")) {
                inboxFragment = InboxFragment.newInstance("inbox");
                ft.add(R.id.main_fragment, inboxFragment);
            } else if (fragmentIntent.hasExtra("fragment_profile")) {
                profileFragment = ProfileFragment.newInstance("profile", "");
                ft.add(R.id.main_fragment, profileFragment);
            } else if (fragmentIntent.hasExtra("fragment_search")) {
                searchFragment = SearchFragment.newInstance("search");
                ft.add(R.id.main_fragment, searchFragment);
            } else {
                videoFragment = VideoFragment.newInstance("fragment_video");
                ft.add(R.id.main_fragment, videoFragment);
            }
        } else {
            videoFragment = VideoFragment.newInstance("fragment_video");
            ft.add(R.id.main_fragment, videoFragment);
        }
        ft.commit();
        Log.d(TAG, "Initial fragment setup completed");
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

    private boolean isAppUsable() {
        return isAppUsable; // Returns cached status
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == btnSearch.getId()) {
            handleSearchClick();
        } else if (view.getId() == btnProfile.getId()) {
            handleProfileClick();
        } else if (view.getId() == btnAddVideo.getId()) {
            handleAddClick();
        } else if (view.getId() == btnHome.getId()) {
            handleHomeClick();
        } else if (view.getId() == btnInbox.getId()) {
            handleInboxClick();
        } else if (view.getId() == btnReferEarn.getId()) {
            initiateReferralFlow();
        }
    }

    private void handleSearchClick() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        searchFragment = (SearchFragment) getSupportFragmentManager().findFragmentByTag("SearchFragment");
        if (searchFragment == null) {
            searchFragment = SearchFragment.newInstance("search");
            ft.add(R.id.main_fragment, searchFragment, "SearchFragment");
        }
        showFragments(ft, 1);
        ft.commit();
    }

    private void handleProfileClick() {
        if (user == null) {
            Intent intent = new Intent(this, SignupChoiceActivity.class);
            startActivityForResult(intent, SIGN_IN_REQUEST_CODE);
            return;
        }
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        profileFragment = (ProfileFragment) getSupportFragmentManager().findFragmentByTag("ProfileFragment");
        if (profileFragment == null) {
            profileFragment = ProfileFragment.newInstance("profile", "");
            ft.add(R.id.main_fragment, profileFragment, "ProfileFragment");
        }
        showFragments(ft, 3);
        ft.commit();
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
        inboxFragment = (InboxFragment) getSupportFragmentManager().findFragmentByTag("InboxFragment");
        if (inboxFragment == null) {
            inboxFragment = InboxFragment.newInstance("inbox");
            ft.add(R.id.main_fragment, inboxFragment, "InboxFragment");
        }
        showFragments(ft, 2);
        ft.commit();
    }

    private void handleHomeClick() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (videoFragment == null) {
            videoFragment = VideoFragment.newInstance("fragment_video");
            ft.add(R.id.main_fragment, videoFragment);
        }
        showFragments(ft, 0);
        ft.commit();
    }

    private void initiateReferralFlow() {
        if (user == null) {
            showNiceDialogBox(this, null, "Sign in to join the referral system.");
            return;
        }
        String userId = user.getUid();
        Intent intent = getIntent();
        Uri data = intent.getData();
        Intent referralIntent = new Intent(this, ReferralActivity.class);
        if (data != null && data.getQueryParameter("code") != null) {
            String referralCode = data.getQueryParameter("code");
            Log.d(TAG, "Deep link detected with referral code: " + referralCode);
            referralIntent.putExtra("referralCode", referralCode);
        }
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
            user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String userId = user.getUid();
                userRef = db.collection("users").document(userId);
                userRef.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        phoneNumber = task.getResult().getString("phone");
                        Log.d(TAG, "Phone number after sign-in: " + phoneNumber);
                        checkDeadline();
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
        if (title == null) {
            title = getString(R.string.request_account_title);
        }
        if (message == null) {
            message = getString(R.string.request_account_message);
        }
        try {
            AlertDialog.Builder myBuilder = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
            myBuilder.setIcon(R.drawable.swipe)
                    .setTitle(title)
                    .setMessage(message)
                    .setNegativeButton("Cancel", (dialogInterface, i) -> {
                        if (context instanceof HomeScreenActivity) {
                            return;
                        }
                        Intent intent = new Intent(context, HomeScreenActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    })
                    .setPositiveButton("Sign up/Sign in", (dialog, whichOne) -> {
                        Intent intent = new Intent(context, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivityForResult(intent, SIGN_IN_REQUEST_CODE);
                    })
                    .show();
        } catch (Exception e) {
            Log.e("Error DialogBox", e.getMessage());
        }
    }

    private void showFragments(FragmentTransaction ft, int position) {
        if (videoFragment != null && videoFragment.isVisible()) {
            ft.hide(videoFragment);
            stopVideoFragment();
        }
        if (searchFragment != null && searchFragment.isVisible()) {
            ft.hide(searchFragment);
        }
        if (inboxFragment != null && inboxFragment.isVisible()) {
            ft.hide(inboxFragment);
        }
        if (profileFragment != null && profileFragment.isVisible()) {
            ft.hide(profileFragment);
        }

        switch (position) {
            case 0:
                if (videoFragment != null) {
                    ft.show(videoFragment);
                    continueVideoFragment();
                }
                break;
            case 1:
                if (searchFragment != null) {
                    ft.show(searchFragment);
                }
                break;
            case 2:
                if (inboxFragment != null) {
                    ft.show(inboxFragment);
                }
                break;
            case 3:
                if (profileFragment != null) {
                    ft.show(profileFragment);
                }
                break;
        }
        Log.d(TAG, "Showing fragment at position: " + position);
    }

    public void stopVideoFragment() {
        if (videoFragment != null) {
            videoFragment.stopVideo();
            Log.d(TAG, "Stopping video fragment");
        }
    }

    public void continueVideoFragment() {
        if (videoFragment != null) {
            videoFragment.continueVideo();
        } else {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            videoFragment = VideoFragment.newInstance("fragment_video");
            ft.add(R.id.main_fragment, videoFragment);
            ft.commit();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopVideoFragment();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isAppUsable()) {
            ensureVideoFragmentVisible();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopVideoFragment();
    }

    private void ensureVideoFragmentVisible() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (videoFragment == null || !videoFragment.isVisible()) {
            videoFragment = VideoFragment.newInstance("fragment_video");
            ft.replace(R.id.main_fragment, videoFragment);
            ft.commit();
            // Preload first video after fragment is added
            new Handler().postDelayed(() -> {
                if (videoFragment.videoAdapter != null && !videoFragment.videos.isEmpty()) {
                    videoFragment.videoAdapter.playVideo(0);
                    videoFragment.videoAdapter.pauseVideo(0);
                }
            }, 500);
        } else {
            showFragments(ft, 0);
            ft.commit();
        }
        continueVideoFragment();
        Log.d(TAG, "Ensuring VideoFragment is visible");
    }
}
