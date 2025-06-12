package com.example.lostfound;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

/**
 * מסך בית למשתמש רגיל. מאפשר פתיחת דיווח, צפייה בפניות ועריכת פרופיל.
 */
public class UserActivity extends AppCompatActivity {

    private Button openNewCaseButton;
    private Button viewMyCasesButton;
    private Button editProfileButton;
    private String username;

    /**
     * אתחול המסך, שליפת שם משתמש, קישור כפתורים והגדרת ניווטים.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        username = getIntent().getStringExtra("username");

        openNewCaseButton = findViewById(R.id.openNewCaseButton);
        viewMyCasesButton = findViewById(R.id.viewMyCasesButton);
        editProfileButton = findViewById(R.id.editProfileButton);

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

        if (editProfileButton != null) {
            editProfileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(UserActivity.this, EditProfileActivity.class);
                    intent.putExtra("username", username);
                    startActivity(intent);
                }
            });
        }
    }
}