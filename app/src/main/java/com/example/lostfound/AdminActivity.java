package com.example.lostfound;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

/**
 * מסך הבית למשתמשי אדמין. מאפשר מעבר לניהול פניות וניהול משתמשים.
 */
public class AdminActivity extends AppCompatActivity {

    private Button viewAllCasesButton;
    private Button manageUsersButton;
    private String username;

    /**
     * אתחול המסך, שליפת שם המשתמש, קישור כפתורים והגדרת פעולות מעבר.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        username = getIntent().getStringExtra("username");

        viewAllCasesButton = findViewById(R.id.viewAllCasesButton);
        manageUsersButton = findViewById(R.id.manageUsersButton);

        viewAllCasesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminActivity.this, AllCasesActivity.class);
                intent.putExtra("username", username);
                startActivity(intent);
            }
        });

        if (manageUsersButton != null) {
            manageUsersButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(AdminActivity.this, UserManagementActivity.class);
                    intent.putExtra("username", username);
                    startActivity(intent);
                }
            });
        }
    }
}