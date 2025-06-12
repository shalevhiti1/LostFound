package com.example.lostfound;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * מסך הזנת פרטי משתמש חדשים (שם, ת"ז, טלפון, אימייל, עיר).
 */
public class UserDetailsActivity extends AppCompatActivity {

    private EditText fullNameEditText, idCardEditText, phoneNumberEditText, emailEditText, cityEditText;
    private Button saveButton;
    private DatabaseHelper dbHelper;
    private String username;
    private ExecutorService executorService;

    /**
     * אתחול המסך, שליפת שם משתמש, קישור שדות, שמירה וניווט.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        dbHelper = new DatabaseHelper(this);
        executorService = Executors.newSingleThreadExecutor();

        username = getIntent().getStringExtra("username");

        fullNameEditText = findViewById(R.id.fullNameEditText);
        idCardEditText = findViewById(R.id.idCardEditText);
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        emailEditText = findViewById(R.id.emailEditText);
        cityEditText = findViewById(R.id.cityEditText);
        saveButton = findViewById(R.id.saveButton);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fullName = fullNameEditText.getText().toString().trim();
                String idCard = idCardEditText.getText().toString().trim();
                String phoneNumber = phoneNumberEditText.getText().toString().trim();
                String email = emailEditText.getText().toString().trim();
                String city = cityEditText.getText().toString().trim();

                if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(idCard) || TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(email) || TextUtils.isEmpty(city)) {
                    Toast.makeText(UserDetailsActivity.this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                    return;
                }

                executorService.execute(() -> {
                    boolean isUpdated = dbHelper.updateUserDetails(username, fullName, idCard, phoneNumber, email, city);

                    runOnUiThread(() -> {
                        if (isUpdated) {
                            Toast.makeText(UserDetailsActivity.this, "User details saved successfully!", Toast.LENGTH_SHORT).show();

                            String role = dbHelper.getUserRole(username);
                            Intent intent;
                            if (role != null) {
                                if (role.equals("user")) {
                                    intent = new Intent(UserDetailsActivity.this, UserActivity.class);
                                } else if (role.equals("admin")) {
                                    intent = new Intent(UserDetailsActivity.this, AdminActivity.class);
                                }  else {
                                    intent = new Intent(UserDetailsActivity.this, LoginActivity.class);
                                }
                                intent.putExtra("username", username);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(UserDetailsActivity.this, "Error retrieving user role. Please login again.", Toast.LENGTH_LONG).show();
                                startActivity(new Intent(UserDetailsActivity.this, LoginActivity.class));
                                finish();
                            }
                        } else {
                            Toast.makeText(UserDetailsActivity.this, "Failed to save user details. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}