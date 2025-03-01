package com.example.e_sholpine.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.e_sholpine.R;
import com.example.e_sholpine.activity.EditActivity;
import com.example.e_sholpine.helper.StaticVariable;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditProfileActivity extends Activity implements View.OnClickListener {

    private TextView tvUsername, tvPhone, tvEmail, tvBirthdate;
    private ImageButton imbPhoto, imbSelect, imbUsername, imbPhone, imbBirthdate; // Added imbPhone
    private LinearLayout llEditProfile, llChangePhoto, llPhone, llEmail;
    private FirebaseFirestore db;
    private Uri avatarUri;
    private final int SELECT_IMAGE_CODE = 10;
    private ImageView imvBackToProfile;
    private Dialog dialog;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private FirebaseUser user;
    private String userId;
    private boolean forReferral; // Flag to track if called for referral flow

    private final String TAG = "EditProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        llEditProfile = findViewById(R.id.llEditProfile);
        llChangePhoto = llEditProfile.findViewById(R.id.llChangePhoto);
        llPhone = llEditProfile.findViewById(R.id.llPhone);
        llEmail = llEditProfile.findViewById(R.id.llEmail);
        tvUsername = llEditProfile.findViewById(R.id.tvUsername);
        tvPhone = llEditProfile.findViewById(R.id.tvPhone);
        tvEmail = llEditProfile.findViewById(R.id.tvEmail);
        tvBirthdate = llEditProfile.findViewById(R.id.tvBirthdate);
        imbPhoto = llEditProfile.findViewById(R.id.imbPhoto);
        imbSelect = llEditProfile.findViewById(R.id.imbSelect);
        imbUsername = llEditProfile.findViewById(R.id.imbUsername);
        imbBirthdate = llEditProfile.findViewById(R.id.imbBirthdate);
        imbPhone = llEditProfile.findViewById(R.id.imbPhone); // Added for phone editing
        imvBackToProfile = findViewById(R.id.imvBackToProfile);

        imbSelect.setVisibility(View.GONE);

        // Set click listeners
        imbPhoto.setOnClickListener(this);
        imbSelect.setOnClickListener(this);
        imvBackToProfile.setOnClickListener(this);
        imbUsername.setOnClickListener(this);
        imbBirthdate.setOnClickListener(this);
        imbPhone.setOnClickListener(this); // Added listener for phone

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        // Get userId and forReferral from intent (from HomeScreenActivity or referral flow)
        userId = getIntent().getStringExtra("userId");
        forReferral = getIntent().getBooleanExtra("forReferral", false);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setView(R.layout.dialog_progress);
        dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (user != null) {
            userId = user.getUid(); // Ensure userId is set

            DocumentReference docRef = db.collection("users").document(userId);
            docRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        tvUsername.setText(getData(document.get("username")));
                        tvPhone.setText(getData(document.get("phone"))); // Matches PaymentActivity field
                        tvEmail.setText(getData(document.get("email")));
                        tvBirthdate.setText(getData(document.get("birthdate")));
                        dialog.dismiss();
                        Log.d(TAG, "DocumentSnapshot data: " + document.get("following"));
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            });
        }
    }

    private String getData(Object data) {
        return data == null ? "" : data.toString();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_IMAGE_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            avatarUri = data.getData();
            uploadAvatar();
        }
    }

    private void uploadAvatar() {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Please Wait...");
        progress.setCancelable(false);
        progress.show();

        StorageReference upload = storageReference.child("user_avatars").child(user.getUid());
        upload.putFile(avatarUri)
                .addOnSuccessListener(taskSnapshot -> {
                    progress.dismiss();
                    Toast.makeText(this, "Image Uploaded", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progress.dismiss();
                    Toast.makeText(this, "Image Failed", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == imbPhoto.getId()) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Photo"), SELECT_IMAGE_CODE);
        }
        if (v.getId() == imvBackToProfile.getId()) {
            finishWithResult(); // Return to HomeScreenActivity with result
        }
        if (v.getId() == imbUsername.getId()) {
            moveToEdit(StaticVariable.USERNAME, tvUsername.getText().toString());
        }
        if (v.getId() == imbBirthdate.getId()) {
            moveToEdit(StaticVariable.BIRTHDATE, tvBirthdate.getText().toString());
        }
        if (v.getId() == imbPhone.getId()) {
            moveToEdit(StaticVariable.PHONE, tvPhone.getText().toString()); // Added for phone editing
        }
    }

    private void moveToEdit(String mode, String content) {
        Intent intent = new Intent(this, EditActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("mode", mode);
        bundle.putString("content", content);
        bundle.putString("userId", userId); // Pass userId
        bundle.putBoolean("forReferral", forReferral); // Pass forReferral
        intent.putExtras(bundle);
        startActivityForResult(intent, 1); // Use startActivityForResult to get results back
    }

    private void finishWithResult() {
        Intent result = new Intent();
        result.putExtra("userId", userId);
        result.putExtra("forReferral", forReferral);
        setResult(RESULT_OK, result);
        finish();
    }
}