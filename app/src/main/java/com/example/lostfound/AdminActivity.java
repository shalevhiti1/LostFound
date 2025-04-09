package com.example.lostfound;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class AdminActivity extends AppCompatActivity {

    private Button viewAllCasesButton;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        username = getIntent().getStringExtra("username"); // Retrieve the username

        viewAllCasesButton = findViewById(R.id.viewAllCasesButton);

        viewAllCasesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminActivity.this, AllCasesActivity.class);
                startActivity(intent);
            }
        });
    }
}