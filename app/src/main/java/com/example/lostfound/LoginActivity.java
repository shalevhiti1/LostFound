package com.example.lostfound;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log; // Added for logging
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.speech.tts.TextToSpeech; // ייבוא TextToSpeech

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Task; // Added for Firebase Tasks
import com.google.android.gms.tasks.Tasks; // Added for Firebase Tasks

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.lostfound.NotificationUtils.showSimpleNotification; // Retained from original

/**
 * מסך התחברות ראשי. כולל בדיקת הרשאות התראות, ערוץ התראות, TTS, ומעבר לאדמין/משתמש.
 * קודכן לעבודה עם Firebase Firestore.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity"; // Added TAG for logging

    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button registerButton;
    private TextToSpeech tts;
    private boolean isTtsReady = false;

    private DatabaseHelper dbHelper;
    private ExecutorService executorService; // ExecutorService is not strictly needed for Firebase Tasks, but kept if other async ops are planned.

    public static final String DEFAULT_CHANNEL_ID = "default_app_channel";

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Notification permission granted.");
                } else {
                    Toast.makeText(this, "Notification permission denied. Some features might not work.", Toast.LENGTH_LONG).show();
                    Log.w(TAG, "Notification permission denied.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize UI components
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);

        // Initialize DatabaseHelper (now Firebase-based) and ExecutorService
        dbHelper = new DatabaseHelper(this);
        executorService = Executors.newSingleThreadExecutor(); // Kept for general background tasks if needed

        // Initialize TextToSpeech
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US); // Consider changing to Locale("iw") for Hebrew
                isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
                if (!isTtsReady) {
                    Log.e(TAG, "TTS language not supported or missing data.");
                }
            } else {
                isTtsReady = false;
                Log.e(TAG, "TTS initialization failed.");
            }
        });

        // Create notification channel and request permission
        createNotificationChannel();
        requestNotificationPermission();

        // Set OnClickListener for Login button
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    String toastMsg = "Please enter username and password.";
                    Toast.makeText(LoginActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                    speakIfTtsReady(toastMsg);
                    return;
                }

                attemptLogin(username, password); // Call the new asynchronous login method
            }
        });

        // Set OnClickListener for Register button
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                startActivity(intent);
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
        // Shut down TextToSpeech
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    /**
     * Attempts to log in the user by checking credentials against Firebase Firestore.
     * This method is asynchronous.
     * @param username The entered username.
     * @param password The entered password.
     */
    private void attemptLogin(String username, String password) {
        Log.d(TAG, "Attempting login for user: " + username);

        // First, check if the user exists and the password matches
        dbHelper.checkUser(username, password)
                .addOnSuccessListener(isValidUser -> {
                    if (isValidUser) {
                        Log.d(TAG, "User " + username + " credentials valid. Fetching role...");
                        // If credentials are valid, fetch the user's role
                        dbHelper.getUserRole(username)
                                .addOnSuccessListener(role -> {
                                    runOnUiThread(() -> {
                                        if (role != null) {
                                            Intent intent;
                                            if (role.equals("user")) {
                                                intent = new Intent(LoginActivity.this, UserActivity.class);
                                            } else if (role.equals("admin")) {
                                                intent = new Intent(LoginActivity.this, AdminActivity.class);
                                            } else {
                                                String toastMsg = "Unknown user role.";
                                                Toast.makeText(LoginActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                                                speakIfTtsReady(toastMsg);
                                                Log.w(TAG, "Unknown role for user: " + username + " -> " + role);
                                                return;
                                            }
                                            intent.putExtra("username", username);
                                            startActivity(intent);
                                            finish(); // Finish LoginActivity after successful login
                                            Log.i(TAG, "Login successful for user: " + username + " (Role: " + role + ")");
                                        } else {
                                            // Role was null, despite checkUser returning true - unlikely scenario if user exists
                                            String toastMsg = "Invalid username or password."; // Generic error
                                            Toast.makeText(LoginActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                                            speakIfTtsReady(toastMsg);
                                            Log.e(TAG, "User " + username + " found but role is null.");
                                        }
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    // Error fetching user role
                                    runOnUiThread(() -> {
                                        String toastMsg = "Login failed: " + e.getMessage();
                                        Toast.makeText(LoginActivity.this, toastMsg, Toast.LENGTH_LONG).show();
                                        speakIfTtsReady(toastMsg);
                                        Log.e(TAG, "Error fetching user role for " + username, e);
                                    });
                                });
                    } else {
                        // Invalid username or password (checkUser returned false)
                        runOnUiThread(() -> {
                            String toastMsg = "Invalid username or password.";
                            Toast.makeText(LoginActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                            speakIfTtsReady(toastMsg);
                            Log.d(TAG, "Invalid credentials for user: " + username);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    // Error during the checkUser operation (e.g., network issue, Firestore error)
                    runOnUiThread(() -> {
                        String toastMsg = "Login failed: " + e.getMessage();
                        Toast.makeText(LoginActivity.this, toastMsg, Toast.LENGTH_LONG).show();
                        speakIfTtsReady(toastMsg);
                        Log.e(TAG, "Error checking user credentials for " + username, e);
                    });
                });
    }


    /**
     * יוצר ערוץ התראות לאפליקציה (נדרש עבור אנדרואיד 8.0 ומעלה).
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.default_channel_name); // Assuming this resource exists
            String description = getString(R.string.default_channel_description); // Assuming this resource exists
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(DEFAULT_CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created: " + DEFAULT_CHANNEL_ID);
            }
        }
    }

    /**
     * מבקש הרשאה לשליחת התראות (עבור API 33+).
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting POST_NOTIFICATIONS permission.");
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                Log.d(TAG, "POST_NOTIFICATIONS permission already granted.");
            }
        }
    }

    /**
     * משמיע הודעה קולית אם TTS מוכן.
     * @param text ההודעה להשמעה.
     */
    private void speakIfTtsReady(String text) {
        if (tts != null && isTtsReady) {
            if (tts.isSpeaking()) {
                tts.stop();
            }
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            Log.w(TAG, "TTS not ready or initialized, cannot speak: " + text);
        }
    }
}