package com.example.lostfound;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class UserActivity extends AppCompatActivity {
    private Button openNewCaseButton;
    private Button viewMyCasesButton;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        username = getIntent().getStringExtra("username"); // Retrieve the username
        openNewCaseButton = findViewById(R.id.openNewCaseButton);
        viewMyCasesButton = findViewById(R.id.viewMyCasesButton);

        openNewCaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UserActivity.this, NewCaseActivity.class);
                intent.putExtra("username", username);
                startActivity(intent);
            }
        });
        viewMyCasesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UserActivity.this, MyCasesActivity.class);
                intent.putExtra("username", username);
                startActivity(intent);
            }
        });
    }
}