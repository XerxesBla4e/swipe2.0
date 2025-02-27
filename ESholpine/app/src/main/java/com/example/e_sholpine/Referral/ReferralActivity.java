package com.example.e_sholpine.Referral;

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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.e_sholpine.R;
import com.example.e_sholpine.activity.EditProfileActivity;
import com.example.e_sholpine.activity.HomeScreenActivity;
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

public class ReferralActivity extends AppCompatActivity {

    private static final int PAYMENT_AMOUNT = 3000; // Payment amount for referral (3,000 UGX)
    private static final int READ_SMS_PERMISSION_REQUEST_CODE = 101;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private TextView referralCodeText;
    private Button shareButton, joinButton;
    private EditText referralCodeInput;
    private Button applyCodeButton;
    private String userId;
    private String phoneNumber;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_referral);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();

        referralCodeText = findViewById(R.id.referral_code);
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
        DocumentReference userRef = db.collection("users").document(userId);
        getUserPhoneNumber(userRef);

        // Handle deep link (if opened via referral link)
        handleDeepLink(getIntent());

        // Check referral status and update UI
        checkReferralStatusAndUpdateUI();

        shareButton.setOnClickListener(v -> shareReferralCode());
        joinButton.setOnClickListener(v -> promptReferralPayment(userId));
        applyCodeButton.setOnClickListener(v -> applyReferralCode());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleDeepLink(intent);
    }

    private void handleDeepLink(Intent intent) {
        Uri data = intent.getData();
        if (data != null && data.getQueryParameter("code") != null) {
            String referralCode = data.getQueryParameter("code");
            applyReferralCodeFromLink(referralCode);
        }
    }

    private void applyReferralCodeFromLink(String referralCode) {
        if (referralCode == null || referralCode.isEmpty()) {
            Toast.makeText(this, "Invalid referral code from link", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("referrals").whereEqualTo("referralCode", referralCode).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String referrerId = querySnapshot.getDocuments().get(0).getString("userId");
                        if (referrerId != null && !referrerId.equals(userId)) {
                            // Update referrer's referredUsers list
                            DocumentReference referrerRef = db.collection("referrals").document(referrerId);
                            referrerRef.update("referredUsers", FieldValue.arrayUnion(Map.of("userId", userId, "timestamp", FieldValue.serverTimestamp())))
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Referral code applied successfully from link!", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to apply referral code from link", Toast.LENGTH_SHORT).show());
                        } else {
                            Toast.makeText(this, "Invalid or your own referral code", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Invalid referral code from link", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error checking referral code from link", Toast.LENGTH_SHORT).show());
    }

    private void getUserPhoneNumber(DocumentReference userRef) {
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    phoneNumber = document.getString("phone");
                    if (phoneNumber == null || phoneNumber.isEmpty()) {
                        Toast.makeText(this, "Please update your phone number in your profile.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, EditProfileActivity.class);
                        intent.putExtra("userId", userId);
                        intent.putExtra("forReferral", true);
                        startActivityForResult(intent, 1);
                    }
                } else {
                    showError("User document does not exist.");
                }
            } else {
                showError("Failed to retrieve user data.");
            }
        });
    }

    private void checkReferralStatusAndUpdateUI() {
        if (user == null) return;

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Boolean joinedReferral = documentSnapshot.getBoolean("joinedReferral");
                    if (joinedReferral != null && joinedReferral) {
                        // User has joined, show referral code and sharing options
                        loadReferralCode();
                        shareButton.setVisibility(View.VISIBLE);
                        joinButton.setVisibility(View.GONE);
                        referralCodeInput.setVisibility(View.GONE);
                        applyCodeButton.setVisibility(View.GONE);
                    } else {
                        // User hasn’t joined, show join button and allow code application
                        referralCodeText.setText("You haven’t joined the referral system yet.");
                        shareButton.setVisibility(View.GONE);
                        joinButton.setVisibility(View.VISIBLE);
                        referralCodeInput.setVisibility(View.VISIBLE);
                        applyCodeButton.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error checking referral status", Toast.LENGTH_SHORT).show());
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
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading referral code", Toast.LENGTH_SHORT).show());
    }

    private void shareReferralCode() {
        String referralCode = referralCodeText.getText().toString();
        if (referralCode.isEmpty() || referralCode.equals("Loading...")) {
            Toast.makeText(this, "Referral code not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String deepLink = "swipe://referral?code=" + referralCode;
        String shareMessage = "Join me on TikTok Clone and earn rewards! Use my referral code: " + referralCode + "\n" +
                "Open this link: " + deepLink + "\nDownload here: [Your App Link]";
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        startActivity(Intent.createChooser(shareIntent, "Share Referral Code"));
    }

    private void promptReferralPayment(String userId) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Please update your phone number in your profile.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, EditProfileActivity.class);
            intent.putExtra("userId", userId);
            intent.putExtra("forReferral", true);
            startActivityForResult(intent, 1);
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Join Referral System")
                .setMessage("Join the referral system for 3,000 UGX to start inviting friends and earn 1,000 UGX per referral!")
                .setPositiveButton("Pay Now", (dialog, which) -> processZengaPayPayment(userId))
                .setNegativeButton("Later", (dialog, which) -> dialog.dismiss())
                .show();
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
                        Toast.makeText(ReferralActivity.this, "Payment processed successfully", Toast.LENGTH_SHORT).show();
                        checkForPaymentConfirmation(userId);
                    } else {
                        Log.e("API_ERROR", "Payment not accepted: " + zengaPayResponse.getMessage());
                        Toast.makeText(ReferralActivity.this, "Payment failed: " + zengaPayResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("API_ERROR", "Response Code: " + response.code());
                    Toast.makeText(ReferralActivity.this, "Payment processing failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ZengaPayResponse> call, @NonNull Throwable t) {
                Log.e("API_ERROR", "Failure: " + t.getMessage());
                Toast.makeText(ReferralActivity.this, "Payment failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkForPaymentConfirmation(String userId) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Verifying Payment");
        progressDialog.setMessage("Please wait while we verify your payment...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        if (!hasReadSmsPermission()) {
            Toast.makeText(this, "SMS permission required to confirm payment.", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }

        new Thread(() -> {
            boolean paymentVerified = checkSmsForPayment();

            runOnUiThread(() -> {
                progressDialog.dismiss();
                if (paymentVerified) {
                    Toast.makeText(ReferralActivity.this, "Payment verified successfully.", Toast.LENGTH_SHORT).show();
                    updateReferralStatusAndGenerateCode(userId);
                } else {
                    Toast.makeText(ReferralActivity.this, "Payment verification failed.", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private boolean hasReadSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
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
                    checkReferralStatusAndUpdateUI(); // Update UI to show referral code
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update referral status", Toast.LENGTH_SHORT).show();
                    Log.e("ReferralActivity", "Error updating referral status: " + e.getMessage());
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
                    referralCodeText.setText(referralCode);
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

        db.collection("referrals").whereEqualTo("referralCode", inputCode).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String referrerId = querySnapshot.getDocuments().get(0).getString("userId");
                        if (referrerId != null && !referrerId.equals(userId)) {
                            // Update referrer's referredUsers list
                            DocumentReference referrerRef = db.collection("referrals").document(referrerId);
                            referrerRef.update("referredUsers", FieldValue.arrayUnion(Map.of("userId", userId, "timestamp", FieldValue.serverTimestamp())))
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Referral code applied successfully!", Toast.LENGTH_SHORT).show();
                                        referralCodeInput.setText("");
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
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.e("ReferralActivity", message);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            String userId = data.getStringExtra("userId");
            boolean forReferral = data.getBooleanExtra("forReferral", false);
            if (forReferral) {
                checkReferralStatusAndUpdateUI(); // Re-check status after phone update
            }
        }
    }
}
