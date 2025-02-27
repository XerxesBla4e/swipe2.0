package com.example.e_sholpine.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.e_sholpine.R;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void signUpPage(View v) {
        Intent intent = new Intent(MainActivity.this, com.example.e_sholpine.activity.SignupChoiceActivity.class);

        startActivity(intent);

    }

    public void signInPage(View v) {
        Intent intent = new Intent(MainActivity.this, com.example.e_sholpine.activity.SigninChoiceActivity.class);
        startActivity(intent);
    }
}