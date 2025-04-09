package com.example.lostfound;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Start the LoginActivity
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish(); // Close MainActivity so the user can't go back to it
    }
}