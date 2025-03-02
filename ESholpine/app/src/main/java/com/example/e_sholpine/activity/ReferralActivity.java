package com.example.e_sholpine.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.e_sholpine.R;
import com.example.e_sholpine.utils.RetrofitClient;
import com.example.e_sholpine.utils.ZengaPayRequest;
import com.example.e_sholpine.utils.ZengaPayResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReferralActivity extends AppCompatActivity {

    private static final int PAYMENT_AMOUNT = 3000;
    private static final int READ_SMS_PERMISSION_REQUEST_CODE = 101;
    private static final int EDIT_PROFILE_REQUEST_CODE = 1;
    private static final String TAG = "ReferralActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private TextView referralCodeText, earningsText; // Added earningsText
    private Button shareButton, joinButton;
    private EditText referralCodeInput;
    private Button applyCodeButton;
    private String userId;
    private String phoneNumber;
    private ProgressDialog progressDialog;
    private DocumentReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_referral);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();

        referralCodeText = findViewById(R.id.referral_code);
        earningsText = findViewById(R.id.earned_points); // Initialize earningsText
        shareButton = findViewById(R.id.share_button);
        joinButton = findViewById(R.id.join_button);
        referralCodeInput = findViewById(R.id.referral_code_input);
        applyCodeButton = findViewById(R.id.apply_code_button);

        if (user == null) {
            Toast.makeText(this, "Please sign in to access referrals", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = user.getUid();
        userRef = db.collection("users").document(userId);
        getUserPhoneNumber();

        String referralCodeFromIntent = getIntent().getStringExtra("referralCode");
        if (referralCodeFromIntent != null) {
            referralCodeInput.setText(referralCodeFromIntent);
        } else {
            handleDeepLink(getIntent());
        }

        checkReferralStatusAndUpdateUI();

        shareButton.setOnClickListener(v -> shareReferralCode());
        joinButton.setOnClickListener(v -> promptReferralPayment(userId));
        applyCodeButton.setOnClickListener(v -> applyReferralCode());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }

    private void handleDeepLink(Intent intent) {
        Uri data = intent.getData();
        if (data != null && data.getQueryParameter("code") != null) {
            String referralCode = data.getQueryParameter("code");
            referralCodeInput.setText(referralCode);
            applyReferralCodeFromLink(referralCode);
        }
    }

    private void getUserPhoneNumber() {
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    phoneNumber = document.getString("phone");
                    Log.d(TAG, "Retrieved phone number: " + phoneNumber);
                    if (phoneNumber == null || phoneNumber.isEmpty()) {
                        showError("Phone number not found. Please update your profile.");
                        promptPhoneNumberUpdate(userId);
                    }
                } else {
                    showError("User document does not exist.");
                }
            } else {
                showError("Failed to retrieve user data: " + task.getException().getMessage());
            }
        });
    }

    private void promptPhoneNumberUpdate(String userId) {
        new AlertDialog.Builder(this)
                .setTitle("Update Phone Number")
                .setMessage("Please update your phone number to proceed with the referral system.")
                .setPositiveButton("Update", (dialog, which) -> {
                    Intent intent = new Intent(this, EditProfileActivity.class);
                    intent.putExtra("userId", userId);
                    intent.putExtra("forReferral", true);
                    startActivityForResult(intent, EDIT_PROFILE_REQUEST_CODE);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    private void checkReferralStatusAndUpdateUI() {
        if (user == null) return;

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Boolean joinedReferral = documentSnapshot.getBoolean("joinedReferral");
                    if (joinedReferral != null && joinedReferral) {
                        loadReferralCode();
                        shareButton.setVisibility(View.VISIBLE);
                        joinButton.setVisibility(View.GONE);
                        referralCodeInput.setVisibility(View.GONE);
                        applyCodeButton.setVisibility(View.GONE);
                        // Fetch and display earnings
                        db.collection("referrals").document(userId).get()
                                .addOnSuccessListener(doc -> {
                                    Long earnings = doc.getLong("earnings");
                                    earningsText.setText("Earnings: " + (earnings != null ? earnings : 0) + " UGX");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error fetching earnings: " + e.getMessage());
                                    earningsText.setText("Earnings: 0 UGX");
                                });
                    } else {
                        referralCodeText.setText("Enter a referral code or join to get yours!");
                        earningsText.setText("Earnings: 0 UGX"); // No earnings until joined
                        shareButton.setVisibility(View.GONE);
                        joinButton.setVisibility(View.VISIBLE);
                        referralCodeInput.setVisibility(View.VISIBLE);
                        applyCodeButton.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error checking referral status: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadReferralCode() {
        db.collection("referrals").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String referralCode = documentSnapshot.getString("referralCode");
                        if (referralCode != null) {
                            referralCodeText.setText(referralCode);
                        } else {
                            generateAndStoreReferralCode(userId);
                        }
                    } else {
                        generateAndStoreReferralCode(userId);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading referral code: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void shareReferralCode() {
        String referralCode = referralCodeText.getText().toString();
        if (referralCode.isEmpty() || referralCode.equals("Enter a referral code or join to get yours!")) {
            Toast.makeText(this, "Referral code not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String deepLink = "swipe://referral?code=" + referralCode;
        String shareMessage = "Join me on Swipe and earn rewards! Use my referral code: " + referralCode + "\n" +
                "Open this link: " + deepLink + "\nDownload here: [Your App Link]";
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        startActivity(Intent.createChooser(shareIntent, "Share Referral Code"));
    }

    private void promptReferralPayment(String userId) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Phone number not available. Please update your profile.", Toast.LENGTH_SHORT).show();
            promptPhoneNumberUpdate(userId);
            return;
        }

        if (!hasReadSmsPermission()) {
            requestReadSmsPermission();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Join Referral System")
                .setMessage("Join the referral system for 3,000 UGX to start inviting friends and earn 1,500 UGX per referral!")
                .setPositiveButton("Pay Now", (dialog, which) -> processZengaPayPayment(userId))
                .setNegativeButton("Later", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void processZengaPayPayment(String userId) {
        Log.d(TAG, "Starting payment process for userId: " + userId + " with phone: " + phoneNumber);
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Phone number is missing, cannot process payment", Toast.LENGTH_SHORT).show();
            return;
        }

        String uniqueExternalReference = String.valueOf(System.currentTimeMillis());
        ZengaPayRequest request = new ZengaPayRequest(phoneNumber, PAYMENT_AMOUNT, uniqueExternalReference, "Referral Join - " + uniqueExternalReference, false);
        Log.d(TAG, "Payment Request: phone=" + phoneNumber + ", amount=" + PAYMENT_AMOUNT + ", ref=" + uniqueExternalReference);

        RetrofitClient.getInstance().getApi().collectPayment(request).enqueue(new Callback<ZengaPayResponse>() {
            @Override
            public void onResponse(@NonNull Call<ZengaPayResponse> call, @NonNull Response<ZengaPayResponse> response) {
                Log.d(TAG, "Payment Response Code: " + response.code());
                Log.d(TAG, "Payment Response Body: " + (response.body() != null ? response.body().toString() : "null"));
                if (response.isSuccessful() && response.body() != null) {
                    ZengaPayResponse zengaPayResponse = response.body();
                    Log.d(TAG, "Payment Status: " + zengaPayResponse.getStatus() + ", Message: " + zengaPayResponse.getMessage());
                    if ("accepted".equalsIgnoreCase(zengaPayResponse.getStatus())) {
                        Toast.makeText(ReferralActivity.this, "Payment request sent. Please enter your PIN on your phone.", Toast.LENGTH_LONG).show();
                        Log.d(TAG, "Payment accepted by API, initiating SMS verification");
                        checkForPaymentConfirmation(userId);
                    } else {
                        String failureMessage = zengaPayResponse.getMessage() != null ? zengaPayResponse.getMessage().toLowerCase() : "";
                        if (failureMessage.contains("insufficient") || failureMessage.contains("balance")) {
                            Toast.makeText(ReferralActivity.this, "Insufficient funds. Please ensure you have at least 3,000 UGX in your mobile money account.", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Payment failed due to insufficient funds: " + failureMessage);
                        } else {
                            Toast.makeText(ReferralActivity.this, "Payment failed: " + zengaPayResponse.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Payment failed: " + zengaPayResponse.getStatus() + " - " + zengaPayResponse.getMessage());
                        }
                    }
                } else {
                    String errorMsg = "Payment processing failed";
                    if (response.code() >= 400 && response.code() < 500) {
                        errorMsg += " - Invalid request (Code: " + response.code() + ")";
                    } else if (response.code() >= 500) {
                        errorMsg += " - Server error (Code: " + response.code() + ")";
                    }
                    Log.e(TAG, "Payment failed - HTTP Code: " + response.code() + ", Error Body: " + (response.errorBody() != null ? response.errorBody().toString() : "null"));
                    Toast.makeText(ReferralActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ZengaPayResponse> call, @NonNull Throwable t) {
                String errorMsg = "Payment failed: " + t.getMessage();
                if (t instanceof java.io.IOException) {
                    errorMsg = "Payment failed: Network issue - check your connection";
                }
                Log.e(TAG, "Payment failed with exception: " + t.getMessage());
                Toast.makeText(ReferralActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void checkForPaymentConfirmation(String userId) {
        Log.d(TAG, "Starting SMS verification for payment");
        showProgressDialog("Please complete payment on your phone...");
        new Thread(() -> {
            final int MAX_WAIT_TIME_SECONDS = 60;
            final int CHECK_INTERVAL_MS = 5000;
            long startTime = System.currentTimeMillis();
            final boolean[] paymentVerified = {false};

            while (!paymentVerified[0] && (System.currentTimeMillis() - startTime) < MAX_WAIT_TIME_SECONDS * 1000) {
                paymentVerified[0] = checkSmsForPayment();
                if (paymentVerified[0]) {
                    Log.d(TAG, "Payment verified via SMS");
                    break;
                }
                try {
                    Thread.sleep(CHECK_INTERVAL_MS);
                    Log.d(TAG, "Checking SMS... Elapsed time: " + (System.currentTimeMillis() - startTime) / 1000 + " seconds");
                } catch (InterruptedException e) {
                    Log.e(TAG, "SMS polling interrupted: " + e.getMessage());
                    break;
                }
            }

            runOnUiThread(() -> {
                dismissProgressDialog();
                if (paymentVerified[0]) {
                    Toast.makeText(this, "Payment verified successfully.", Toast.LENGTH_SHORT).show();
                    updateReferralStatusAndGenerateCode(userId);
                    markReferredUserAsPaid(userId);
                } else {
                    Toast.makeText(this, "Payment verification failed. Please ensure you have minimum balance of 3200 on line", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Payment verification timed out after " + MAX_WAIT_TIME_SECONDS + " seconds");
                }
            });
        }).start();
    }

    private void markReferredUserAsPaid(String paidUserId) {
        db.collection("referrals")
                .whereArrayContains("referredUsers", Map.of("userId", paidUserId, "paid", false))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot referrerDoc = querySnapshot.getDocuments().get(0);
                        String referrerId = referrerDoc.getString("userId");
                        Log.d(TAG, "Found referrer " + referrerId + " for paid user " + paidUserId);

                        assert referrerId != null;
                        DocumentReference referrerRef = db.collection("referrals").document(referrerId);
                        referrerRef.get().addOnSuccessListener(doc -> {
                            ArrayList<Map<String, Object>> referredUsers = (ArrayList<Map<String, Object>>) doc.get("referredUsers");
                            if (referredUsers != null) {
                                for (int i = 0; i < referredUsers.size(); i++) {
                                    Map<String, Object> userEntry = referredUsers.get(i);
                                    if (paidUserId.equals(userEntry.get("userId")) && Boolean.FALSE.equals(userEntry.get("paid"))) {
                                        userEntry.put("paid", true);
                                        referredUsers.set(i, userEntry);
                                        referrerRef.update("referredUsers", referredUsers)
                                                .addOnSuccessListener(aVoid -> {
                                                    Log.d(TAG, "Marked " + paidUserId + " as paid for referrer " + referrerId);
                                                    // Increment earnings by 1500
                                                    referrerRef.update("earnings", FieldValue.increment(1500))
                                                            .addOnSuccessListener(aVoid1 -> {
                                                                Log.d(TAG, "Incremented earnings by 1500 for " + referrerId);
                                                                // Update UI with new earnings
                                                                referrerRef.get().addOnSuccessListener(updatedDoc -> {
                                                                    Long earnings = updatedDoc.getLong("earnings");
                                                                    earningsText.setText("Earnings: " + (earnings != null ? earnings : 0) + " UGX");
                                                                });
                                                            })
                                                            .addOnFailureListener(e -> Log.e(TAG, "Failed to increment earnings: " + e.getMessage()));
                                                })
                                                .addOnFailureListener(e -> Log.e(TAG, "Failed to mark user as paid: " + e.getMessage()));
                                        break;
                                    }
                                }
                            }
                        });
                    } else {
                        Log.d(TAG, "No referrer found for user " + paidUserId + " or already marked as paid");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error finding referrer for paid user: " + e.getMessage()));
    }

    private boolean hasReadSmsPermission() {
        boolean hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "SMS Permission Status: " + hasPermission);
        return hasPermission;
    }

    private void requestReadSmsPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, READ_SMS_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "SMS permission granted, proceeding with payment");
                processZengaPayPayment(userId);
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_SMS)) {
                    new AlertDialog.Builder(this)
                            .setTitle("Permission Required")
                            .setMessage("SMS permission is needed to verify your payment. Please grant it to proceed.")
                            .setPositiveButton("Retry", (dialog, which) -> requestReadSmsPermission())
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("Permission Denied")
                            .setMessage("SMS permission was denied. Please enable it in app settings to verify payments.")
                            .setPositiveButton("Go to Settings", (dialog, which) -> {
                                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            }
        }
    }

    @SuppressLint("Range")
    private boolean checkSmsForPayment() {
        if (!hasReadSmsPermission()) {
            Log.e(TAG, "SMS permission not granted during verification");
            return false;
        }

        Uri smsUri = Telephony.Sms.CONTENT_URI;
        ContentResolver contentResolver = getContentResolver();
        String[] projection = {Telephony.Sms.BODY, Telephony.Sms.DATE};
        String selection = Telephony.Sms.BODY + " LIKE ? OR " + Telephony.Sms.BODY + " LIKE ? OR " + Telephony.Sms.BODY + " LIKE ?";
        String[] selectionArgs = {"%deducted%", "%ZENGAPAY%", "%Charge%"};
        long currentTime = System.currentTimeMillis();
        long recentThreshold = 5 * 60 * 1000;

        Cursor cursor = contentResolver.query(smsUri, projection, selection, selectionArgs, Telephony.Sms.DATE + " DESC");
        if (cursor == null) {
            Log.e(TAG, "SMS cursor is null");
            return false;
        }

        Log.d(TAG, "Checking SMS - Found " + cursor.getCount() + " messages");
        while (cursor.moveToNext()) {
            String messageBody = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY));
            long messageDate = cursor.getLong(cursor.getColumnIndex(Telephony.Sms.DATE));
            Log.d(TAG, "SMS: " + messageBody + " | Date: " + messageDate);
            if (currentTime - messageDate <= recentThreshold) {
                if ((messageBody.contains("ZENGAPAY") && (messageBody.contains("deducted") || messageBody.contains("Charge"))) ||
                        (messageBody.contains("PAID") && messageBody.contains("Charge"))) {
                    Log.d(TAG, "Payment SMS found: " + messageBody);
                    cursor.close();
                    return true;
                }
            }
        }
        cursor.close();
        Log.d(TAG, "No matching payment SMS found in this check");
        return false;
    }

    private void updateReferralStatusAndGenerateCode(String userId) {
        db.collection("users").document(userId)
                .update("joinedReferral", true)
                .addOnSuccessListener(aVoid -> {
                    generateAndStoreReferralCode(userId);
                    checkReferralStatusAndUpdateUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update referral status", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating referral status: " + e.getMessage());
                });
    }

    private void generateAndStoreReferralCode(String userId) {
        String referralCode = generateReferralCode(userId);
        Map<String, Object> referralData = new HashMap<>();
        referralData.put("userId", userId);
        referralData.put("referralCode", referralCode);
        referralData.put("referredUsers", new ArrayList<Map<String, Object>>());
        referralData.put("earnings", 0); // Initialize earnings field

        db.collection("referrals").document(userId)
                .set(referralData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Your referral code: " + referralCode, Toast.LENGTH_LONG).show();
                    referralCodeText.setText(referralCode);
                    earningsText.setText("Earnings: 0 UGX"); // Initial display
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

    private void applyReferralCode() {
        String inputCode = referralCodeInput.getText().toString().trim();
        if (inputCode.isEmpty()) {
            Toast.makeText(this, "Please enter a referral code", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Boolean joinedReferral = documentSnapshot.getBoolean("joinedReferral");
                    if (joinedReferral != null && joinedReferral) {
                        Toast.makeText(this, "You’ve already joined the referral system.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.collection("referrals").whereEqualTo("referralCode", inputCode).get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (!querySnapshot.isEmpty()) {
                                    String referrerId = querySnapshot.getDocuments().get(0).getString("userId");
                                    if (referrerId != null && !referrerId.equals(userId)) {
                                        DocumentReference referrerRef = db.collection("referrals").document(referrerId);
                                        Map<String, Object> referralEntry = new HashMap<>();
                                        referralEntry.put("userId", userId);
                                        referralEntry.put("timestamp", FieldValue.serverTimestamp());
                                        referralEntry.put("paid", false);
                                        referrerRef.update("referredUsers", FieldValue.arrayUnion(referralEntry))
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(this, "Referral code applied successfully!", Toast.LENGTH_SHORT).show();
                                                    referralCodeInput.setText("");
                                                    promptToJoinAfterReferral(userId);
                                                })
                                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to apply referral code", Toast.LENGTH_SHORT).show());
                                    } else {
                                        Toast.makeText(this, "Invalid or your own referral code", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(this, "Invalid referral code", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Error checking referral code", Toast.LENGTH_SHORT).show());
                });
    }

    private void applyReferralCodeFromLink(String referralCode) {
        if (referralCode == null || referralCode.isEmpty()) {
            Toast.makeText(this, "Invalid referral code", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Boolean joinedReferral = documentSnapshot.getBoolean("joinedReferral");
                    if (joinedReferral != null && joinedReferral) {
                        Toast.makeText(this, "You’ve already joined the referral system.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.collection("referrals").whereEqualTo("referralCode", referralCode).get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (!querySnapshot.isEmpty()) {
                                    String referrerId = querySnapshot.getDocuments().get(0).getString("userId");
                                    if (referrerId != null && !referrerId.equals(userId)) {
                                        DocumentReference referrerRef = db.collection("referrals").document(referrerId);
                                        Map<String, Object> referralEntry = new HashMap<>();
                                        referralEntry.put("userId", userId);
                                        referralEntry.put("timestamp", FieldValue.serverTimestamp());
                                        referralEntry.put("paid", false);
                                        referrerRef.update("referredUsers", FieldValue.arrayUnion(referralEntry))
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(this, "Referral code applied successfully!", Toast.LENGTH_SHORT).show();
                                                    referralCodeInput.setText("");
                                                    promptToJoinAfterReferral(userId);
                                                })
                                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to apply referral code", Toast.LENGTH_SHORT).show());
                                    } else {
                                        Toast.makeText(this, "Invalid or your own referral code", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(this, "Invalid referral code", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Error checking referral code", Toast.LENGTH_SHORT).show());
                });
    }

    private void promptToJoinAfterReferral(String userId) {
        new AlertDialog.Builder(this)
                .setTitle("Join the Referral Program")
                .setMessage("You’ve applied a referral code! Join now for 3,000 UGX to get your own code and earn rewards.")
                .setPositiveButton("Join Now", (dialog, which) -> promptReferralPayment(userId))
                .setNegativeButton("Maybe Later", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.e(TAG, message);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_PROFILE_REQUEST_CODE && resultCode == RESULT_OK) {
            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    phoneNumber = task.getResult().getString("phone");
                    Log.d(TAG, "Phone number after edit: " + phoneNumber);
                    checkReferralStatusAndUpdateUI();
                }
            });
        }
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
            progressDialog.setMessage(message);
        } else {
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
