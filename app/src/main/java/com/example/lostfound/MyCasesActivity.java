package com.example.lostfound;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // Added for logging
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech; // ייבוא TextToSpeech

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task; // Added for Firebase Tasks

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * מסך הצגת כל הפניות של המשתמש הנוכחי. כולל TTS, טעינה ברקע וניווט לפרטי פנייה.
 * קודכן לעבודה עם Firebase Firestore.
 */
public class MyCasesActivity extends AppCompatActivity {

    private static final String TAG = "MyCasesActivity"; // Added TAG for logging

    private ListView myCasesListView;
    private TextView noCasesTextView;
    private DatabaseHelper dbHelper;
    private String username;
    private ExecutorService executorService; // ExecutorService is not strictly needed for Firebase Tasks, but kept if other async ops are planned.

    private TextToSpeech tts;
    private boolean isTtsReady = false;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_cases);

        // Initialize UI components
        myCasesListView = findViewById(R.id.myCasesListView);
        noCasesTextView = findViewById(R.id.noCasesTextView);

        // Initialize DatabaseHelper (now Firebase-based) and ExecutorService
        dbHelper = new DatabaseHelper(this);
        executorService = Executors.newSingleThreadExecutor(); // Kept for general background tasks if needed

        // Get username from Intent
        username = getIntent().getStringExtra("username");

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

        if (username == null || username.isEmpty()) {
            String toastMsg = "Error: Username not found.";
            noCasesTextView.setText(toastMsg);
            noCasesTextView.setVisibility(View.VISIBLE);
            myCasesListView.setVisibility(View.GONE); // Ensure list is hidden
            Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
            speakIfTtsReady(toastMsg);
            Log.e(TAG, "Username is null or empty in onCreate.");
            // Consider finishing the activity or redirecting if username is critical and missing
            return;
        }

        // Load requests for the current user
        loadMyCases();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload data whenever the activity resumes (e.g., after returning from CaseDetailsActivity)
        loadMyCases();
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
     * Helper method to speak text using TTS if ready.
     * @param text The text to speak.
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

    /**
     * טוען את כל הפניות של המשתמש הנוכחי מ-Firebase Firestore ומעדכן את ה-ListView.
     * פעולה זו היא אסינכרונית.
     */
    @SuppressLint("SetTextI18n")
    private void loadMyCases() {
        Log.d(TAG, "loadMyCases called for username: " + username);

        // Call the asynchronous getRequestsByUsername method from DatabaseHelper
        dbHelper.getRequestsByUsername(username)
                .addOnSuccessListener(myRequests -> {
                    // This block executes when the data is successfully fetched from Firebase
                    runOnUiThread(() -> {
                        if (myRequests == null) {
                            // This scenario is less likely with addOnSuccessListener but added for robustness.
                            Log.e(TAG, "getRequestsByUsername returned null in success listener!");
                            String toastMsg = "Error loading requests.";
                            noCasesTextView.setText(toastMsg);
                            noCasesTextView.setVisibility(View.VISIBLE);
                            myCasesListView.setVisibility(View.GONE);
                            Toast.makeText(MyCasesActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                            speakIfTtsReady(toastMsg);
                            return;
                        }

                        if (myRequests.isEmpty()) {
                            Log.d(TAG, "No requests found for username: " + username);
                            String toastMsg = "You have not opened any requests with us yet.";
                            noCasesTextView.setText(toastMsg);
                            noCasesTextView.setVisibility(View.VISIBLE);
                            myCasesListView.setVisibility(View.GONE);
                            Toast.makeText(MyCasesActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                            speakIfTtsReady(toastMsg);
                        } else {
                            Log.d(TAG, "Found " + myRequests.size() + " requests for username: " + username);
                            noCasesTextView.setVisibility(View.GONE);
                            myCasesListView.setVisibility(View.VISIBLE);

                            // Create and set the adapter
                            RequestAdapter adapter = new RequestAdapter(
                                    MyCasesActivity.this,
                                    myRequests
                            );
                            myCasesListView.setAdapter(adapter);

                            // Set OnItemClickListener for the ListView
                            myCasesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    Request selectedRequest = (Request) parent.getItemAtPosition(position);

                                    Intent intent = new Intent(MyCasesActivity.this, CaseDetailsActivity.class);
                                    // CHANGED: Pass firestoreId (String) instead of old int ID
                                    // IMPORTANT: CaseDetailsActivity must be updated to expect String REQUEST_ID
                                    intent.putExtra("REQUEST_ID", selectedRequest.getFirestoreId());
                                    intent.putExtra("username", username);
                                    startActivity(intent);
                                }
                            });
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    // This block executes if there's an error fetching data from Firebase
                    runOnUiThread(() -> {
                        String toastMsg = "Error loading requests: " + e.getMessage();
                        Toast.makeText(MyCasesActivity.this, toastMsg, Toast.LENGTH_LONG).show();
                        speakIfTtsReady(toastMsg);
                        Log.e(TAG, "Error loading requests for user " + username + " from Firebase", e);
                        noCasesTextView.setText("Failed to load requests. Please check your network connection.");
                        noCasesTextView.setVisibility(View.VISIBLE);
                        myCasesListView.setVisibility(View.GONE);
                    });
                });
        // The executorService.execute() wrapping the dbHelper call is no longer needed
        // because Firebase Tasks handle their own background threading.
    }
}