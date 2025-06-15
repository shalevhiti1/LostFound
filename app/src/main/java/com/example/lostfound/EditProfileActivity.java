package com.example.lostfound;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;
import android.util.Log; // Added for logging

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task; // Added for Firebase Tasks
import com.google.android.gms.tasks.Tasks; // Added for Firebase Tasks (e.g., Tasks.whenAllSuccess)

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * מסך עריכת פרטי משתמש: מאפשר עדכון שם, טלפון, אימייל ועיר.
 * קודכן לעבודה עם Firebase Firestore.
 */
public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity"; // Added TAG for logging

    private EditText editUsername, editFullName, editIdCard, editPhoneNumber, editEmail, editCity;
    private Button saveProfileButton;
    private DatabaseHelper dbHelper;
    private String currentUsername;
    private ExecutorService executorService; // Kept for general background tasks if needed, Firebase Tasks handle their own threading.

    // משתני TTS
    private TextToSpeech tts;
    private boolean isTtsReady = false;

    /**
     * אתחול המסך, בדיקת שם משתמש, טעינת פרטי המשתמש והגדרת שמירה.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        dbHelper = new DatabaseHelper(this); // Initialize DatabaseHelper with Context
        executorService = Executors.newSingleThreadExecutor(); // Initialize ExecutorService

        // אתחול TTS
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

        // Get current username from Intent
        currentUsername = getIntent().getStringExtra("username");
        if (currentUsername == null || currentUsername.isEmpty()) {
            String errorMsg = "Error: Username not found.";
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            speakIfTtsReady(errorMsg);
            Log.e(TAG, errorMsg);
            finish();
            return;
        }

        // Initialize UI components
        editUsername = findViewById(R.id.editUsername);
        editFullName = findViewById(R.id.editFullName);
        editIdCard = findViewById(R.id.editIdCard);
        editPhoneNumber = findViewById(R.id.editPhoneNumber);
        editEmail = findViewById(R.id.editEmail);
        editCity = findViewById(R.id.editCity);
        saveProfileButton = findViewById(R.id.saveProfileButton);

        // Load user details from Firebase
        loadUserDetails();

        // Set OnClickListener for save button
        saveProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserDetails();
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
     * טוען את פרטי המשתמש מ-Firebase Firestore ומציג אותם בשדות.
     * פעולה זו היא אסינכרונית.
     */
    private void loadUserDetails() {
        Log.d(TAG, "Loading user details for: " + currentUsername);

        // Create a list of Tasks to fetch all user details concurrently
        List<Task<String>> fetchTasks = new ArrayList<>();
        fetchTasks.add(dbHelper.getUserFullName(currentUsername));
        fetchTasks.add(dbHelper.getUserIdCard(currentUsername));
        fetchTasks.add(dbHelper.getUserPhoneNumber(currentUsername));
        fetchTasks.add(dbHelper.getUserEmail(currentUsername));
        fetchTasks.add(dbHelper.getUserCity(currentUsername));

        // Use Tasks.whenAllSuccess to wait for all fetch operations to complete
        Tasks.whenAllSuccess(fetchTasks)
                .addOnSuccessListener(results -> {
                    // results will contain the String values in the order they were added to fetchTasks
                    String fullName = (String) results.get(0);
                    String idCard = (String) results.get(1);
                    String phoneNumber = (String) results.get(2);
                    String email = (String) results.get(3);
                    String city = (String) results.get(4);

                    // Update UI on the main thread
                    runOnUiThread(() -> {
                        editUsername.setText(currentUsername); // Username is typically not editable
                        editFullName.setText(fullName != null ? fullName : "");
                        editIdCard.setText(idCard != null ? idCard : "");
                        editPhoneNumber.setText(phoneNumber != null ? phoneNumber : "");
                        editEmail.setText(email != null ? email : "");
                        editCity.setText(city != null ? city : "");
                        Log.d(TAG, "User details loaded successfully for: " + currentUsername);
                    });
                })
                .addOnFailureListener(e -> {
                    // Handle error if any of the fetch operations fail
                    runOnUiThread(() -> {
                        String toastMsg = "Failed to load user details: " + e.getMessage();
                        Toast.makeText(EditProfileActivity.this, toastMsg, Toast.LENGTH_LONG).show();
                        speakIfTtsReady(toastMsg);
                        Log.e(TAG, "Error loading user details for " + currentUsername, e);
                    });
                });
    }

    /**
     * שומר את השינויים על המשתמש ב-Firebase Firestore.
     * פעולה זו היא אסינכרונית.
     */
    private void saveUserDetails() {
        String fullName = editFullName.getText().toString().trim();
        String idCard = editIdCard.getText().toString().trim();
        String phoneNumber = editPhoneNumber.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String city = editCity.getText().toString().trim();

        // Input validation
        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(idCard) ||
                TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(email) || TextUtils.isEmpty(city)) {
            String toastMsg = "Please fill all fields.";
            Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
            speakIfTtsReady(toastMsg);
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            String toastMsg = "Invalid email format.";
            Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
            speakIfTtsReady(toastMsg);
            return;
        }

        Log.d(TAG, "Saving user details for: " + currentUsername);
        // Call the asynchronous update method from DatabaseHelper
        dbHelper.updateUserDetails(currentUsername, fullName, idCard, phoneNumber, email, city)
                .addOnSuccessListener(isSuccess -> {
                    // This isSuccess will be true if the Firestore operation completed successfully.
                    if (isSuccess) {
                        runOnUiThread(() -> {
                            String toastMsg = "Saved successfully!";
                            Toast.makeText(EditProfileActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                            speakIfTtsReady(toastMsg);
                            finish(); // Close activity on success
                        });
                    } else {
                        runOnUiThread(() -> {
                            String toastMsg = "Saving details failed. Please try again."; // Changed Hebrew message
                            Toast.makeText(EditProfileActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                            speakIfTtsReady(toastMsg);
                            Log.e(TAG, "Firestore update operation reported as not successful for user: " + currentUsername);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle any exceptions during the Firestore update process
                    runOnUiThread(() -> {
                        String toastMsg = "Saving details failed: " + e.getMessage();
                        Toast.makeText(EditProfileActivity.this, toastMsg, Toast.LENGTH_LONG).show();
                        speakIfTtsReady(toastMsg);
                        Log.e(TAG, "Error saving user details for " + currentUsername, e);
                    });
                });
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