package com.example.lostfound;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.speech.tts.TextToSpeech;

import static com.example.lostfound.NotificationUtils.showSimpleNotification;

/**
 * מסך התחברות ראשי. כולל בדיקת הרשאות התראות, ערוץ התראות, TTS, ומעבר לאדמין/משתמש.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button registerButton;
    private TextToSpeech tts;
    private boolean isTtsReady = false;

    private DatabaseHelper dbHelper;
    private ExecutorService executorService;

    public static final String DEFAULT_CHANNEL_ID = "default_app_channel";

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Notification permission denied. Some features might not work.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        dbHelper = new DatabaseHelper(this);
        executorService = Executors.newSingleThreadExecutor();

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                    isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
                } else {
                    isTtsReady = false;
                }
            }
        });

        createNotificationChannel();
        requestNotificationPermission();

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please enter username and password.", Toast.LENGTH_SHORT).show();
                    if (tts != null && isTtsReady) {
                        if (tts.isSpeaking()) tts.stop();
                        tts.speak("Please enter username and password.", TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                    return;
                }

                executorService.execute(() -> {
                    boolean isValidUser = dbHelper.checkUser(username, password);
                    if (isValidUser) {
                        String role = dbHelper.getUserRole(username);
                        runOnUiThread(() -> {
                            if (role != null) {
                                Intent intent;
                                if (role.equals("user")) {
                                    intent = new Intent(LoginActivity.this, UserActivity.class);
                                } else if (role.equals("admin")) {
                                    intent = new Intent(LoginActivity.this, AdminActivity.class);
                                }  else {
                                    Toast.makeText(LoginActivity.this, "Unknown user role.", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                intent.putExtra("username", username);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this,"Invalid username or password.", Toast.LENGTH_SHORT).show();
                                if (tts != null && isTtsReady) {
                                    if (tts.isSpeaking()) tts.stop();
                                    tts.speak("Invalid username or password.", TextToSpeech.QUEUE_FLUSH, null, null);
                                }
                                return;
                            }
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "Invalid username or password.", Toast.LENGTH_SHORT).show();
                            if (tts != null && isTtsReady) {
                                if (tts.isSpeaking()) tts.stop();
                                tts.speak("Invalid username or password.", TextToSpeech.QUEUE_FLUSH, null, null);
                            }
                            return;
                        });
                    }
                });
            }
        });

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
        executorService.shutdown();
    }

    /**
     * יוצר ערוץ התראות לאפליקציה.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.default_channel_name);
            String description = getString(R.string.default_channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(DEFAULT_CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * מבקש הרשאה לשליחת התראות (API 33+).
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}