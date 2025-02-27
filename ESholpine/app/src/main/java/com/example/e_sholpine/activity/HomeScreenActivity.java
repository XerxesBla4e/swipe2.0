package com.example.e_sholpine.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
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
import com.example.e_sholpine.Referral.ReferralActivity;
import com.example.e_sholpine.fragment.InboxFragment;
import com.example.e_sholpine.fragment.ProfileFragment;
import com.example.e_sholpine.fragment.SearchFragment;
import com.example.e_sholpine.fragment.VideoFragment;
import com.example.e_sholpine.utils.RetrofitClient;
import com.example.e_sholpine.utils.ZengaPayRequest;
import com.example.e_sholpine.utils.ZengaPayResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeScreenActivity extends FragmentActivity implements View.OnClickListener {

    private static final int PAYMENT_AMOUNT = 3000; // Payment amount for referral (3,000 UGX)
    private static final int READ_SMS_PERMISSION_REQUEST_CODE = 101;
    private VideoFragment videoFragment;
    private SearchFragment searchFragment;
    private ProfileFragment profileFragment;
    private InboxFragment inboxFragment;
    private long pressedTime;
    private String message = "";
    private Button btnHome, btnAddVideo, btnInbox, btnProfile, btnSearch, btnReferEarn;
    private FirebaseUser user;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static long pressedBackTime = 0;
    private final static String TAG = "NavigationFragment";
    private Intent fragmentIntent = null;
    private boolean openAppFromLink = false;
    private String phoneNumber;
    private ProgressDialog progressDialog;
    private DocumentReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_TikTokCloneProject);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);
        fragmentIntent = getIntent();

        // Initial fragment transaction for setup
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (fragmentIntent.getExtras() != null) {
            if (fragmentIntent.hasExtra("id")) {
                openAppFromLink = true;
            }
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

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();

        btnHome = findViewById(R.id.btnHome);
        btnAddVideo = findViewById(R.id.btnAddVideo);
        btnInbox = findViewById(R.id.btnInbox);
        btnProfile = findViewById(R.id.btnProfile);
        btnSearch = findViewById(R.id.btnSearch);
        btnReferEarn = findViewById(R.id.btnReferandEarn); // New button for "Refer & Earn"

        btnHome.setOnClickListener(this);
        btnAddVideo.setOnClickListener(this);
        btnInbox.setOnClickListener(this);
        btnProfile.setOnClickListener(this);
        btnSearch.setOnClickListener(this);
        btnReferEarn.setOnClickListener(this); // Add listener for "Refer & Earn"

        // Retrieve phone number for payment
        if (user != null) {
            String userId = user.getUid();
            userRef = db.collection("users").document(userId);
            getUserPhoneNumber();

            // Check and update user data
            checkAndUpdateUserData();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
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
    protected void onPause() {
        super.onPause();
        stopVideoFragment();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopVideoFragment();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == btnSearch.getId()) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            if (searchFragment == null) {
                searchFragment = SearchFragment.newInstance("search");
                ft.add(R.id.main_fragment, searchFragment);
            }
            showFragments(ft, 1);
            ft.commit();
        }
        if (view.getId() == btnProfile.getId()) {
            handleProfileClick();
        }
        if (view.getId() == btnAddVideo.getId()) {
            handleAddClick();
        }
        if (view.getId() == btnHome.getId()) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            if (videoFragment == null) {
                videoFragment = VideoFragment.newInstance("fragment_video");
                ft.add(R.id.main_fragment, videoFragment);
            }
            showFragments(ft, 0);
            ft.commit();
        }
        if (view.getId() == btnInbox.getId()) {
            handleInboxClick();
        }
        if (view.getId() == btnReferEarn.getId()) {
            initiateReferralFlow();
        }
    }

    private void handleProfileClick() {
        if (user == null) {
            Intent intent = new Intent(this, SignupChoiceActivity.class);
            startActivity(intent);
            return;
        }

        if (openAppFromLink) {
            Intent intent = new Intent(this, EditProfileActivity.class); // Updated to use EditProfileActivity
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
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String phoneNumber = documentSnapshot.getString("phone"); // Adjusted to match your field name

                    // Check phone number first, regardless of joinedReferral status
                    if (phoneNumber == null || phoneNumber.isEmpty()) {
                        promptPhoneNumberUpdate(userId, true); // Use a flag to indicate it's for referral flow
                    } else {
                        // Proceed based on joinedReferral status
                        Boolean joinedReferral = documentSnapshot.getBoolean("joinedReferral");
                        if (joinedReferral != null && joinedReferral) {
                            // User already joined, proceed to ReferralActivity
                            startActivity(new Intent(this, ReferralActivity.class));
                        } else {
                            // User hasn’t joined yet, prompt for payment
                            promptReferralPayment(userId);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error checking profile", Toast.LENGTH_SHORT).show());
    }

    private void promptPhoneNumberUpdate(String userId, boolean forReferral) {
        new AlertDialog.Builder(this)
                .setTitle("Update Phone Number")
                .setMessage("Please update your phone number to proceed with the referral system.")
                .setPositiveButton("Update", (dialog, which) -> {
                    Intent intent = new Intent(this, EditProfileActivity.class); // Updated to use EditProfileActivity
                    intent.putExtra("userId", userId);
                    intent.putExtra("forReferral", forReferral); // Pass flag to track context
                    startActivityForResult(intent, 1);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    private void promptReferralPayment(String userId) {
        new AlertDialog.Builder(this)
                .setTitle("Join Referral System")
                .setMessage("Join the referral system for 3,000 UGX to start inviting friends and earn 2,000 UGX per referral!")
                .setPositiveButton("Pay Now", (dialog, which) -> processPaymentAndGenerateCode(userId))
                .setNegativeButton("Later", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void processPaymentAndGenerateCode(String userId) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Phone number not available. Please update your profile.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (hasReadSmsPermission()) {
            processZengaPayPayment(userId);
        } else {
            requestReadSmsPermission();
        }
    }

    private boolean hasReadSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestReadSmsPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, READ_SMS_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String userId = user.getUid();
                processZengaPayPayment(userId);
            } else {
                Toast.makeText(this, "SMS permission denied. Unable to verify payment.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getUserPhoneNumber() {
        if (user != null) {
            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        phoneNumber = document.getString("phone"); // Matches your PaymentActivity field name
                        if (phoneNumber != null && !phoneNumber.isEmpty()) {
                            // No need to enable/disable button here as it's handled in the flow
                        } else {
                            showError("Phone number not found. Please update your profile.");
                        }
                    } else {
                        showError("User document does not exist.");
                    }
                } else {
                    showError("Failed to retrieve user data.");
                }
            });
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.e("HomeScreenActivity", message);
    }

    private void processZengaPayPayment(String userId) {
        String uniqueExternalReference = String.valueOf(System.currentTimeMillis());
        ZengaPayRequest request = new ZengaPayRequest(
                phoneNumber, PAYMENT_AMOUNT, uniqueExternalReference, "Referral Join - " + uniqueExternalReference, false);

        RetrofitClient.getInstance().getApi().collectPayment(request).enqueue(new Callback<ZengaPayResponse>() {
            @Override
            public void onResponse(@NonNull Call<ZengaPayResponse> call, @NonNull Response<ZengaPayResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ZengaPayResponse zengaPayResponse = response.body();
                    Log.d("API_SUCCESS", "Status: " + zengaPayResponse.getStatus());
                    Log.d("API_SUCCESS", "Message: " + zengaPayResponse.getMessage());

                    if ("accepted".equalsIgnoreCase(zengaPayResponse.getStatus())) {
                        String transactionReference = zengaPayResponse.getTransactionReference();
                        Toast.makeText(HomeScreenActivity.this, "Payment processed successfully", Toast.LENGTH_SHORT).show();
                        checkForPaymentConfirmation(userId);
                    } else {
                        Log.e("API_ERROR", "Payment not accepted: " + zengaPayResponse.getMessage());
                        Toast.makeText(HomeScreenActivity.this, "Payment failed: " + zengaPayResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("API_ERROR", "Response Code: " + response.code());
                    Toast.makeText(HomeScreenActivity.this, "Payment processing failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ZengaPayResponse> call, @NonNull Throwable t) {
                Log.e("API_ERROR", "Failure: " + t.getMessage());
                Toast.makeText(HomeScreenActivity.this, "Payment failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkForPaymentConfirmation(String userId) {
        showProgressDialog("Verifying payment...");

        if (!hasReadSmsPermission()) {
            Toast.makeText(this, "SMS permission required to confirm payment.", Toast.LENGTH_SHORT).show();
            dismissProgressDialog();
            return;
        }

        new Thread(() -> {
            boolean paymentVerified = checkSmsForPayment();

            runOnUiThread(() -> {
                dismissProgressDialog();
                if (paymentVerified) {
                    Toast.makeText(HomeScreenActivity.this, "Payment verified successfully.", Toast.LENGTH_SHORT).show();
                    updateReferralStatusAndGenerateCode(userId);
                } else {
                    Toast.makeText(HomeScreenActivity.this, "Payment verification failed.", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    @SuppressLint("Range")
    private boolean checkSmsForPayment() {
        Uri smsUri = Telephony.Sms.CONTENT_URI;
        ContentResolver contentResolver = getContentResolver();
        String[] projection = {Telephony.Sms.BODY, Telephony.Sms.DATE};

        String selection = Telephony.Sms.BODY + " LIKE ? OR " + Telephony.Sms.BODY + " LIKE ? OR " + Telephony.Sms.BODY + " LIKE ?";
        String[] selectionArgs = {"%deducted%", "%ZENGAPAY%", "%Charge%"};

        long currentTime = System.currentTimeMillis();
        long recentThreshold = 2 * 60 * 1000; // 2 minutes

        boolean paymentVerified = false;

        Cursor cursor = contentResolver.query(smsUri, projection, selection, selectionArgs, Telephony.Sms.DATE + " DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String messageBody = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY));
                long messageDate = cursor.getLong(cursor.getColumnIndex(Telephony.Sms.DATE));

                if (currentTime - messageDate <= recentThreshold) {
                    if ((messageBody.contains("ZENGAPAY") && (messageBody.contains("deducted") || messageBody.contains("Charge"))) ||
                            (messageBody.contains("PAID") && messageBody.contains("Charge"))) {
                        paymentVerified = true;
                        break;
                    }
                }
            }
            cursor.close();
        }

        return paymentVerified;
    }

    private void updateReferralStatusAndGenerateCode(String userId) {
        db.collection("users").document(userId)
                .update("joinedReferral", true)
                .addOnSuccessListener(aVoid -> {
                    generateAndStoreReferralCode(userId);
                    Toast.makeText(this, "Joined referral system successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update referral status", Toast.LENGTH_SHORT).show();
                    Log.e("HomeScreenActivity", "Error updating referral status: " + e.getMessage());
                });
    }

    private void generateAndStoreReferralCode(String userId) {
        String referralCode = generateReferralCode(userId);
        Map<String, Object> referralData = new HashMap<>();
        referralData.put("userId", userId);
        referralData.put("referralCode", referralCode);
        referralData.put("referredUsers", new ArrayList<Map<String, Object>>());

        db.collection("referrals").document(userId)
                .set(referralData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Your referral code: " + referralCode, Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, ReferralActivity.class));
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to generate referral code", Toast.LENGTH_SHORT).show());
    }

    private String generateReferralCode(String userId) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder(userId.substring(0, 3).toUpperCase());
        for (int i = 0; i < 6; i++) {
            code.append(characters.charAt(random.nextInt(characters.length())));
        }
        return code.toString();
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
            myBuilder.setIcon(R.drawable.splash_background)
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
                        startActivity(intent);
                    })
                    .show();
        } catch (Exception e) {
            Log.e("Error DialogBox", e.getMessage());
        }
    }

    private void showFragments(FragmentTransaction ft, int position) {
        if (position == 0) {
            if (videoFragment != null && !videoFragment.isVisible()) {
                ft.show(videoFragment);
                continueVideoFragment();
            }
        }

        if (position == 1) {
            if (searchFragment != null && !searchFragment.isVisible()) {
                ft.show(searchFragment);
            }
        }

        if (position == 2) {
            if (inboxFragment != null && !inboxFragment.isVisible()) {
                ft.show(inboxFragment);
            }
        }

        if (position == 3) {
            if (profileFragment != null && !profileFragment.isVisible()) {
                ft.show(profileFragment);
            }
        }


        if (videoFragment != null && position != 0) {
            if (videoFragment.isVisible()) {
                ft.hide(videoFragment);
                stopVideoFragment();
            }
        }

        if (searchFragment != null && position != 1) {
            if (searchFragment.isVisible()) {
                ft.hide(searchFragment);
            }
        }

        if (inboxFragment != null && position != 2) {
            if (inboxFragment.isVisible()) {
                ft.hide(inboxFragment);
            }
        }

        if (profileFragment != null && position != 3) {
            if (profileFragment.isVisible()) {
                ft.hide(profileFragment);
            }
        }
    }

    public void stopVideoFragment() {
        if (videoFragment != null) {
            videoFragment.pauseVideo();
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

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
            progressDialog.setMessage(message);
        }
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
