package com.example.lostfound;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log; // Added for logging
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task; // Added for Firebase Tasks

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * מסך הזנת פרטי משתמש חדשים (שם, ת"ז, טלפון, אימייל, עיר).
 * קודכן לעבודה עם Firebase Firestore.
 */
public class UserDetailsActivity extends AppCompatActivity {

    private static final String TAG = "UserDetailsActivity"; // Added TAG for logging

    private EditText fullNameEditText, idCardEditText, phoneNumberEditText, emailEditText, cityEditText;
    private Button saveButton;
    private DatabaseHelper dbHelper;
    private String username;
    private ExecutorService executorService; // ExecutorService is not strictly needed for Firebase Tasks, but kept if other async ops are planned.

    /**
     * אתחול המסך, שליפת שם משתמש, קישור שדות, שמירה וניווט.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        // Initialize DatabaseHelper (now Firebase-based) and ExecutorService
        dbHelper = new DatabaseHelper(this);
        executorService = Executors.newSingleThreadExecutor(); // Kept for general background tasks if needed

        // Get username from Intent
        username = getIntent().getStringExtra("username");

        if (username == null || username.isEmpty()) {
            Log.e(TAG, "Username is null or empty in onCreate. Cannot proceed.");
            Toast.makeText(this, "Error: Username not found. Please log in again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(UserDetailsActivity.this, LoginActivity.class)); // Redirect to login
            finish();
            return;
        }

        // Initialize UI components
        fullNameEditText = findViewById(R.id.fullNameEditText);
        idCardEditText = findViewById(R.id.idCardEditText);
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        emailEditText = findViewById(R.id.emailEditText);
        cityEditText = findViewById(R.id.cityEditText);
        saveButton = findViewById(R.id.saveButton);

        // Set OnClickListener for save button
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserDetails(); // Call the asynchronous save method
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shut down the executor service
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    /**
     * אוספת את הנתונים משדות הקלט, מבצעת ולידציה בסיסית,
     * ושומרת את פרטי המשתמש המעודכנים ב-Firebase Firestore.
     * פעולה זו היא אסינכרונית.
     */
    private void saveUserDetails() {
        String fullName = fullNameEditText.getText().toString().trim();
        String idCard = idCardEditText.getText().toString().trim();
        String phoneNumber = phoneNumberEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String city = cityEditText.getText().toString().trim();

        // Input validation
        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(idCard) || TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(email) || TextUtils.isEmpty(city)) {
            Toast.makeText(UserDetailsActivity.this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Attempting to save user details for: " + username);

        // Call the asynchronous updateUserDetails method from DatabaseHelper
        dbHelper.updateUserDetails(username, fullName, idCard, phoneNumber, email, city)
                .addOnSuccessListener(isUpdated -> {
                    // This isUpdated will be true if the Firestore operation completed successfully.
                    if (isUpdated) {
                        Log.i(TAG, "User details updated successfully for: " + username);
                        Toast.makeText(UserDetailsActivity.this, "User details saved successfully!", Toast.LENGTH_SHORT).show();

                        // Now, fetch the user role to determine where to navigate
                        dbHelper.getUserRole(username)
                                .addOnSuccessListener(role -> {
                                    runOnUiThread(() -> {
                                        Intent intent;
                                        if (role != null) {
                                            if (role.equals("user")) {
                                                intent = new Intent(UserDetailsActivity.this, UserActivity.class);
                                            } else if (role.equals("admin")) {
                                                intent = new Intent(UserDetailsActivity.this, AdminActivity.class);
                                            } else {
                                                // Default if role is 'representative' or unexpected, redirect to login
                                                Log.w(TAG, "Unexpected role '" + role + "' for user " + username + ". Redirecting to Login.");
                                                Toast.makeText(UserDetailsActivity.this, "Role not recognized. Please login again.", Toast.LENGTH_LONG).show();
                                                intent = new Intent(UserDetailsActivity.this, LoginActivity.class);
                                            }
                                            intent.putExtra("username", username);
                                            startActivity(intent);
                                            finish(); // Close UserDetailsActivity
                                        } else {
                                            // Role was null - highly unlikely if update succeeded, but handle defensively
                                            Log.e(TAG, "User role is null after update for user: " + username);
                                            Toast.makeText(UserDetailsActivity.this, "Error retrieving user role. Please login again.", Toast.LENGTH_LONG).show();
                                            startActivity(new Intent(UserDetailsActivity.this, LoginActivity.class));
                                            finish();
                                        }
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    // Error fetching user role after successful update details
                                    Log.e(TAG, "Error fetching user role after details update for user: " + username, e);
                                    runOnUiThread(() -> {
                                        Toast.makeText(UserDetailsActivity.this, "Error retrieving user role: " + e.getMessage() + ". Please login again.", Toast.LENGTH_LONG).show();
                                        startActivity(new Intent(UserDetailsActivity.this, LoginActivity.class));
                                        finish();
                                    });
                                });
                    } else {
                        // isUpdated was false (e.g., Firestore operation didn't report success)
                        Log.e(TAG, "Failed to save user details for " + username + ": Firestore operation reported non-success.");
                        runOnUiThread(() -> {
                            Toast.makeText(UserDetailsActivity.this, "Failed to save user details. Please try again.", Toast.LENGTH_SHORT).show();
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle any exceptions during the Firestore update process (e.g., network error)
                    Log.e(TAG, "Error saving user details for " + username, e);
                    runOnUiThread(() -> {
                        Toast.makeText(UserDetailsActivity.this, "Failed to save user details: " + e.getMessage() + ". Please try again.", Toast.LENGTH_LONG).show();
                    });
                });
    }
}