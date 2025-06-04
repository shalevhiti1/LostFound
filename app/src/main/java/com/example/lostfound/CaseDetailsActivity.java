package com.example.lostfound;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler; // NEW: ייבוא Handler
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech; // ייבוא TextToSpeech, לממשק טקסט לדיבור.


import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.text.SimpleDateFormat;
import java.util.Date; // ייבוא Date
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit; // NEW: ייבוא TimeUnit

public class CaseDetailsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ExecutorService executorService;
    private TextToSpeech tts; // TTS instance
    private boolean isTtsReady = false; // Flag for TTS initialization status

    // Declare TextViews for all details
    private TextView itemTypeTextView, colorTextView, brandTextView, ownerNameTextView, lossDescriptionTextView;
    private TextView tripDateTextView, tripTimeTextView, originTextView, destinationTextView, lineNumberTextView;
    private TextView fullNameTextView, idCardTextView, phoneNumberTextView, emailTextView, cityTextView;
    private TextView requestIdTextView;
    private TextView statusTextView;
    private TextView systemCommentsTextView;
    private TextView countdownTextView; // NEW: TextView for countdown

    private Button editCaseButton;
    private CardView editCaseButtonCard;
    private Button readDetailsButton; // NEW: Read Details button
    private CardView readDetailsButtonCard; // NEW: Read Details button card
    private String loggedInUsername;

    private Handler countdownHandler; // NEW: Handler for countdown updates
    private Runnable countdownRunnable; // NEW: Runnable for countdown logic
    private long deadlineMillis; // NEW: Stores the deadline in milliseconds

    // Define the processing time (3 business days)
    private static final long PROCESSING_TIME_MILLIS = 3 * 24 * 60 * 60 * 1000L; // 3 days in milliseconds

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_case_details);

        // Initialize TextViews
        itemTypeTextView = findViewById(R.id.itemTypeTextView);
        colorTextView = findViewById(R.id.colorTextView);
        brandTextView = findViewById(R.id.brandTextView);
        ownerNameTextView = findViewById(R.id.ownerNameTextView);
        lossDescriptionTextView = findViewById(R.id.lossDescriptionTextView);
        tripDateTextView = findViewById(R.id.tripDateTextView);
        tripTimeTextView = findViewById(R.id.tripTimeTextView);
        originTextView = findViewById(R.id.originTextView);
        destinationTextView = findViewById(R.id.destinationTextView);
        lineNumberTextView = findViewById(R.id.lineNumberTextView);
        fullNameTextView = findViewById(R.id.fullNameTextView);
        idCardTextView = findViewById(R.id.idCardTextView);
        phoneNumberTextView = findViewById(R.id.phoneNumberTextView);
        emailTextView = findViewById(R.id.emailTextView);
        cityTextView = findViewById(R.id.cityTextView);
        requestIdTextView = findViewById(R.id.requestIdTextView);
        statusTextView = findViewById(R.id.statusTextView);
        systemCommentsTextView = findViewById(R.id.systemCommentsTextView);
        countdownTextView = findViewById(R.id.countdownTextView); // NEW: Initialize countdown TextView

        // Initialize the edit button and its CardView
        editCaseButton = findViewById(R.id.editCaseButton);
        editCaseButtonCard = findViewById(R.id.editCaseButtonCard);

        // Initialize the read details button and its CardView
        readDetailsButton = findViewById(R.id.readDetailsButton);
        readDetailsButtonCard = findViewById(R.id.readDetailsButtonCard);

        // Initialize DatabaseHelper and ExecutorService
        dbHelper = new DatabaseHelper(this);
        executorService = Executors.newSingleThreadExecutor();

        // Get the request ID and logged-in username from the Intent
        int requestId = getIntent().getIntExtra("REQUEST_ID", -1);
        loggedInUsername = getIntent().getStringExtra("username");

        // Log the received username
        Log.d("CaseDetailsActivity", "Received username: " + (loggedInUsername != null ? loggedInUsername : "null"));

        // Initialize TextToSpeech engine
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "This Language is not supported or missing data.");
                        Toast.makeText(CaseDetailsActivity.this, "Language not supported for speech.", Toast.LENGTH_SHORT).show();
                        isTtsReady = false;
                    } else {
                        Log.d("TTS", "TTS Initialization Success. Language set to English.");
                        isTtsReady = true;
                    }
                } else {
                    Log.e("TTS", "TTS Initialization Failed! Status: " + status);
                    Toast.makeText(CaseDetailsActivity.this, "Speech initialization failed.", Toast.LENGTH_SHORT).show();
                    isTtsReady = false;
                }
            }
        });

        if (requestId != -1) {
            executorService.execute(() -> {
                Request request = dbHelper.getRequestById(requestId);
                String userRole = null;
                if (loggedInUsername != null) {
                    userRole = dbHelper.getUserRole(loggedInUsername);
                }

                Log.d("CaseDetailsActivity", "User role for " + loggedInUsername + ": " + (userRole != null ? userRole : "null"));

                String finalUserRole = userRole;
                runOnUiThread(() -> {
                    if (request != null) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                        if (requestIdTextView != null) {
                            requestIdTextView.setText("Case ID: " + request.getId());
                        }

                        itemTypeTextView.setText("Item Type: " + request.getItemType());
                        colorTextView.setText("Color: " + request.getColor());
                        brandTextView.setText("Brand: " + request.getBrand());
                        ownerNameTextView.setText("Owner Name: " + request.getOwnerName());
                        lossDescriptionTextView.setText("Loss Description: " + request.getLossDescription());
                        tripDateTextView.setText("Trip Date: " + dateFormat.format(request.getTripDate()));
                        tripTimeTextView.setText("Trip Time: " + request.getTripTime());
                        originTextView.setText("Origin: " + request.getOrigin());
                        destinationTextView.setText("Destination: " + request.getDestination());
                        lineNumberTextView.setText("Line Number: " + request.getLineNumber());
                        fullNameTextView.setText("Reporter Full Name: " + request.getFullName());
                        idCardTextView.setText("Reporter ID Card: " + request.getIdCard());
                        phoneNumberTextView.setText("Reporter Phone Number: " + request.getPhoneNumber());
                        emailTextView.setText("Reporter Email: " + request.getEmail());
                        cityTextView.setText("Reporter City: " + request.getCity());

                        if (statusTextView != null) {
                            statusTextView.setText("Status: " + request.getStatus());
                        }
                        if (systemCommentsTextView != null) {
                            systemCommentsTextView.setText("System Comments: " + request.getSystemComments());
                        }

                        // Show edit button only if the logged-in user is an admin
                        if (finalUserRole != null && finalUserRole.equals("admin")) {
                            Log.d("CaseDetailsActivity", "Showing edit button for admin.");
                            editCaseButtonCard.setVisibility(View.VISIBLE);
                            editCaseButton.setOnClickListener(v -> {
                                Intent editIntent = new Intent(CaseDetailsActivity.this, AdminEditCaseActivity.class);
                                editIntent.putExtra("REQUEST_ID", requestId);
                                startActivity(editIntent);
                            });
                        } else {
                            Log.d("CaseDetailsActivity", "Hiding edit button. User is not admin or role is null.");
                            editCaseButtonCard.setVisibility(View.GONE);
                        }

                        // Set OnClickListener for the Read Details button
                        readDetailsButton.setOnClickListener(v -> {
                            if (tts != null && isTtsReady) {
                                if (tts.isSpeaking()) {
                                    tts.stop();
                                }
                                String detailsToSpeak = "Case ID: " + request.getId() + ". " +
                                        "Item type: " + request.getItemType() + ". " +
                                        "Color: " + request.getColor() + ". " +
                                        "Brand: " + request.getBrand() + ". " +
                                        "Owner name: " + request.getOwnerName() + ". " +
                                        "Loss description: " + request.getLossDescription() + ". " +
                                        "Trip date: " + dateFormat.format(request.getTripDate()) + ". " +
                                        "Trip time: " + request.getTripTime() + ". " +
                                        "Origin: " + request.getOrigin() + ". " +
                                        "Destination: " + request.getDestination() + ". " +
                                        "Line number: " + request.getLineNumber() + ". " +
                                        "Reporter full name: " + request.getFullName() + ". " +
                                        "Reporter ID card: " + request.getIdCard() + ". " +
                                        "Reporter phone number: " + request.getPhoneNumber() + ". " +
                                        "Reporter email: " + request.getEmail() + ". " +
                                        "Reporter city: " + request.getCity() + ". " +
                                        "Status: " + request.getStatus() + ". " +
                                        "System comments: " + request.getSystemComments() + ".";
                                tts.speak(detailsToSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
                            } else {
                                Log.e("TTS", "TTS not ready to speak. isTtsReady: " + isTtsReady);
                                Toast.makeText(CaseDetailsActivity.this, "Speech not ready. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                        });

                        // NEW: Initialize countdown timer
                        deadlineMillis = request.getCreationTimestamp() + PROCESSING_TIME_MILLIS;
                        countdownHandler = new Handler();
                        countdownRunnable = new Runnable() {
                            @Override
                            public void run() {
                                updateCountdown();
                                countdownHandler.postDelayed(this, 1000); // Update every second
                            }
                        };
                        countdownHandler.post(countdownRunnable); // Start the countdown
                    } else {
                        Toast.makeText(CaseDetailsActivity.this, "Request not found.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            });
        } else {
            Toast.makeText(this, "No request ID provided.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * NEW: Updates the countdown TextView with the remaining time.
     * Calculates the difference between the deadline and the current time,
     * then formats and displays it. Stops the countdown if the deadline is passed.
     */
    @SuppressLint("SetTextI18n")
    private void updateCountdown() {
        long currentTime = System.currentTimeMillis();
        long timeLeft = deadlineMillis - currentTime;

        if (timeLeft <= 0) {
            countdownTextView.setText("Time left: Processing time elapsed.");
            countdownTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            if (countdownHandler != null) {
                countdownHandler.removeCallbacks(countdownRunnable); // Stop updating
            }
        } else {
            long days = TimeUnit.MILLISECONDS.toDays(timeLeft);
            timeLeft -= TimeUnit.DAYS.toMillis(days);
            long hours = TimeUnit.MILLISECONDS.toHours(timeLeft);
            timeLeft -= TimeUnit.HOURS.toMillis(hours);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft);
            timeLeft -= TimeUnit.MINUTES.toMillis(minutes);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeft);

            String countdownText = String.format(Locale.getDefault(),
                    "Time left: %d days, %d hours, %d minutes, %d seconds",
                    days, hours, minutes, seconds);
            countdownTextView.setText(countdownText);
            countdownTextView.setTextColor(getResources().getColor(R.color.orange_700)); // Using a color from resources
        }
    }


    /**
     * מתודת מחזור החיים {@code onDestroy} נקראת כאשר האקטיביטי נהרס.
     * חשוב לכבות את שירות ה-ExecutorService כאן כדי לשחרר משאבים ולמנוע דליפות זיכרון.
     * כמו כן, יש לכבות את מנוע ה-TextToSpeech כדי לשחרר את המשאבים שלו.
     * NEW: עוצר את הספירה לאחור כדי למנוע דליפות זיכרון.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown(); // כיבוי מסודר של ה-ExecutorService.
        // כיבוי מנוע ה-TTS כדי לשחרר משאבים.
        if (tts != null) {
            tts.stop(); // עצור כל דיבור מתבצע.
            tts.shutdown(); // שחרר את משאבי ה-TTS.
        }
        // NEW: עצירת הספירה לאחור
        if (countdownHandler != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
    }
}
