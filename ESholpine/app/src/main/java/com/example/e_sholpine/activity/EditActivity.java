package com.example.e_sholpine.activity;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.example.e_sholpine.R;
import com.example.e_sholpine.helper.StaticVariable;
import com.example.e_sholpine.helper.Validator;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditActivity extends Activity implements View.OnClickListener {

    private TextInputLayout layoutInput;
    private TextInputEditText edtInput;
    private TextView tvLabel;
    private ImageButton imbBack;
    private Button btnSave;
    private String mode;
    private String content;
    private Validator validator;
    private FirebaseFirestore db;
    private String userId;
    private boolean forReferral; // Flag to track if called for referral flow

    private final String TAG = "EditActivity";
    private Handler handler = new Handler();
    private String msg;
    private final Calendar myCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_left_to_right, R.anim.slide_right_to_left);
        setContentView(R.layout.activity_edit);

        layoutInput = findViewById(R.id.layoutInput);
        edtInput = findViewById(R.id.edtInput);
        tvLabel = findViewById(R.id.tvLabel);
        imbBack = findViewById(R.id.imbBack);
        btnSave = findViewById(R.id.btnSave);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        mode = bundle.getString("mode");
        content = bundle.getString("content");
        userId = bundle.getString("userId");
        forReferral = bundle.getBoolean("forReferral", false);

        validator = Validator.getInstance();
        db = FirebaseFirestore.getInstance();

        switch (mode) {
            case StaticVariable.USERNAME:
                handleUsername();
                break;
            case StaticVariable.BIRTHDATE:
                handleBirthdate();
                break;
            case StaticVariable.PHONE: // Added for phone editing
                handlePhoneNumber();
                break;
        }

        imbBack.setOnClickListener(this);
        btnSave.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == imbBack.getId()) {
            finishWithResult(); // Return to EditProfileActivity with result
        }
        if (view.getId() == btnSave.getId()) {
            saveField();
        }
    }

    private void handleUsername() {
        tvLabel.setText(R.string.username_label);
        edtInput.setHint("admin");
        edtInput.setText(content);

        edtInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                setEnableSave(true);
            }
            @Override
            public void afterTextChanged(Editable editable) {
                if (!validator.isValidUsername(editable.toString())) {
                    layoutInput.setError(getString(R.string.error_username));
                    setEnableSave(false);
                } else {
                    layoutInput.setError("");
                    if (content.equals(editable.toString())) {
                        setEnableSave(false);
                    } else {
                        setEnableSave(true);
                    }
                }
            }
        });

        btnSave.setOnClickListener(v -> {
            db.collection("users")
                    .whereEqualTo("username", edtInput.getText().toString())
                    .get()
                    .addOnCompleteListener(task -> {
                        msg = "FALSE";
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                if (document.exists()) {
                                    msg = "TRUE";
                                    break;
                                }
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }

                        if (msg.equals("FALSE")) {
                            setEnableSave(false);
                            updateUsername();
                        } else {
                            setEnableSave(false);
                            layoutInput.setError(getString(R.string.exist_username));
                        }
                    });
        });
    }

    private void handleBirthdate() {
        tvLabel.setText(R.string.birthdate_label);
        edtInput.setHint("01/01/2000");
        edtInput.setText(content);
        layoutInput.setStartIconDrawable(R.drawable.ic_calendar);

        layoutInput.setStartIconOnClickListener(v -> {
            DatePickerDialog.OnDateSetListener date = (datePicker, year, month, day) -> {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, month);
                myCalendar.set(Calendar.DAY_OF_MONTH, day);
                updateLabel();
            };
            new DatePickerDialog(this, date, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        edtInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void afterTextChanged(Editable editable) {
                if (!validator.isValidBirthdate(editable.toString())) {
                    layoutInput.setError(getString(R.string.error_birthdate));
                    setEnableSave(false);
                } else {
                    layoutInput.setError("");
                    if (content.equals(editable.toString())) {
                        setEnableSave(false);
                    } else {
                        setEnableSave(true);
                    }
                }
            }
        });
    }

    private void handlePhoneNumber() {
        tvLabel.setText(R.string.phone_number_label); // Add this string resource in strings.xml
        edtInput.setHint("256771234567");
        edtInput.setText(content);
        layoutInput.setStartIconDrawable(R.drawable.ic_phone); // Optional: Add phone icon

        edtInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                setEnableSave(true);
            }
            @Override
            public void afterTextChanged(Editable editable) {
                if (!validator.isValidPhone(editable.toString())) { // Add this method to Validator
                    layoutInput.setError(getString(R.string.error_phone_number)); // Add this string resource
                    setEnableSave(false);
                } else {
                    layoutInput.setError("");
                    if (content.equals(editable.toString())) {
                        setEnableSave(false);
                    } else {
                        setEnableSave(true);
                    }
                }
            }
        });
    }

    private void saveField() {
        String newValue = edtInput.getText().toString().trim();
        setEnableSave(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put(mode, newValue);

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "DocumentSnapshot successfully updated!");
                    finishWithResult(); // Return result to EditProfileActivity
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating document", e);
                    Toast.makeText(this, "Failed to update " + mode, Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUsername() {
        // This method can be removed or simplified since saveField handles updates
        saveField();
    }

    private void setEnableSave(boolean value) {
        if (value) {
            btnSave.setEnabled(true);
            btnSave.setTextColor(getResources().getColor(R.color.tiktok_red));
        } else {
            btnSave.setEnabled(false);
            btnSave.setTextColor(getResources().getColor(R.color.tiktok_grey_50));
        }
    }

    private void updateLabel() {
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat dateFormat = new SimpleDateFormat(myFormat, Locale.US);
        edtInput.setText(dateFormat.format(myCalendar.getTime()));
    }

    private void finishWithResult() {
        Intent result = new Intent();
        result.putExtra("userId", userId);
        result.putExtra("forReferral", forReferral);
        setResult(RESULT_OK, result);
        finish();
    }

    // Add to Validator.java if not already present
    public static boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && phoneNumber.matches("^256\\d{9}$");
    }
}
