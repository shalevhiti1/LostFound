package com.example.lostfound;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * מסך פרטי פנייה: הצגת כל פרטי הבקשה, כתובת, קואורדינטות, טיימר, TTS ואפשרות עריכה לאדמין.
 * קודכן לעבודה עם Firebase Firestore.
 */
public class CaseDetailsActivity extends AppCompatActivity {

    private static final String TAG = "CaseDetailsActivity"; // Added TAG for logging

    private DatabaseHelper dbHelper;
    private ExecutorService executorService; // Used for general background tasks if needed, Firebase Tasks handle their own threading.
    private TextToSpeech tts;
    private boolean isTtsReady = false;

    private TextView itemTypeTextView, colorTextView, brandTextView, ownerNameTextView, lossDescriptionTextView;
    private TextView tripDateTextView, tripTimeTextView, originTextView, destinationTextView, lineNumberTextView;
    private TextView fullNameTextView, idCardTextView, phoneNumberTextView, emailTextView, cityTextView;
    private TextView requestIdTextView;
    private TextView statusTextView;
    private TextView systemCommentsTextView;
    private TextView countdownTextView;
    private TextView locationAddressLabel;
    private TextView locationAddressTextView;
    private TextView latitudeLabel, latitudeTextView, longitudeLabel, longitudeTextView;
    private View latLngLayout;
    private View locationAddressLayout;
    private Button showOnMapButton;
    private Button editCaseButton;
    private CardView editCaseButtonCard;
    private Button readDetailsButton;
    private CardView readDetailsButtonCard;
    private String loggedInUsername;

    private Handler countdownHandler;
    private Runnable countdownRunnable;
    private long deadlineMillis;
    private Request currentRequest; // Added to hold the fetched request object

    // Assuming this constant represents 3 days in milliseconds
    private static final long PROCESSING_TIME_MILLIS = 3 * 24 * 60 * 60 * 1000L;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_case_details);

        // Initialize UI components
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
        countdownTextView = findViewById(R.id.countdownTextView);
        locationAddressLayout = findViewById(R.id.locationAddressLayout);

        locationAddressLabel = findViewById(R.id.locationAddressLabel);
        locationAddressTextView = findViewById(R.id.locationAddressTextView);

        latLngLayout = findViewById(R.id.latLngLayout);
        latitudeLabel = findViewById(R.id.latitudeLabel);
        latitudeTextView = findViewById(R.id.latitudeTextView);
        longitudeLabel = findViewById(R.id.longitudeLabel);
        longitudeTextView = findViewById(R.id.longitudeTextView);
        showOnMapButton = findViewById(R.id.showOnMapButton);

        editCaseButton = findViewById(R.id.editCaseButton);
        editCaseButtonCard = findViewById(R.id.editCaseButtonCard);
        readDetailsButton = findViewById(R.id.readDetailsButton);
        readDetailsButtonCard = findViewById(R.id.readDetailsButtonCard);

        dbHelper = new DatabaseHelper(this);
        executorService = Executors.newSingleThreadExecutor();

        // CHANGED: Get Firestore Request ID from Intent as String
        String firestoreRequestId = getIntent().getStringExtra("REQUEST_ID");
        loggedInUsername = getIntent().getStringExtra("username");

        // Initialize TextToSpeech
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // Consider changing to Locale("iw") for Hebrew if primary language is Hebrew
                int result = tts.setLanguage(Locale.US);
                isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
                if (!isTtsReady) {
                    Log.e(TAG, "TTS language not supported or missing data.");
                }
            } else {
                isTtsReady = false;
                Log.e(TAG, "TTS initialization failed.");
            }
        });

        // Load case details based on the received Firestore ID
        if (firestoreRequestId != null && !firestoreRequestId.isEmpty()) {
            loadCaseDetails(firestoreRequestId);
        } else {
            String toastMsg = "Error: No request ID provided.";
            Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
            speakIfTtsReady(toastMsg);
            Log.e(TAG, "No Firestore Request ID provided in Intent.");
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // If the request was successfully loaded, restart the countdown
        if (currentRequest != null && countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
            countdownHandler.post(countdownRunnable);
        }
        // Re-load details in case they were updated by AdminEditCaseActivity
        // (This assumes AdminEditCaseActivity finishes and returns here)
        String firestoreRequestId = getIntent().getStringExtra("REQUEST_ID");
        if (firestoreRequestId != null && !firestoreRequestId.isEmpty()) {
            loadCaseDetails(firestoreRequestId);
        }
    }

    /**
     * Loads the request details from Firebase Firestore and updates the UI.
     * This method is asynchronous.
     * @param firestoreRequestId The Firestore document ID of the request to load.
     */
    private void loadCaseDetails(String firestoreRequestId) {
        Log.d(TAG, "Loading case details for Firestore ID: " + firestoreRequestId);
        dbHelper.getRequestById(firestoreRequestId)
                .addOnSuccessListener(request -> {
                    if (request != null) {
                        currentRequest = request; // Store the fetched request object
                        Log.d(TAG, "Request loaded: " + request.getFirestoreId());
                        // Fetch user role (still using dbHelper, which is now Firebase-based)
                        dbHelper.getUserRole(loggedInUsername)
                                .addOnSuccessListener(userRole -> {
                                    updateUIWithRequestDetails(request, userRole);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to get user role for " + loggedInUsername, e);
                                    // Proceed to update UI, but without admin privileges
                                    updateUIWithRequestDetails(request, null);
                                    Toast.makeText(CaseDetailsActivity.this, "Could not get user role.", Toast.LENGTH_SHORT).show();
                                });

                    } else {
                        String toastMsg = "Request not found for ID: " + firestoreRequestId;
                        Toast.makeText(CaseDetailsActivity.this, toastMsg, Toast.LENGTH_LONG).show();
                        speakIfTtsReady(toastMsg);
                        Log.e(TAG, toastMsg);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    String toastMsg = "Failed to load request: " + e.getMessage();
                    Toast.makeText(CaseDetailsActivity.this, toastMsg, Toast.LENGTH_LONG).show();
                    speakIfTtsReady(toastMsg);
                    Log.e(TAG, "Error loading request details for ID: " + firestoreRequestId, e);
                    finish();
                });
    }

    @SuppressLint("SetTextI18n")
    private void updateUIWithRequestDetails(Request request, String userRole) {
        runOnUiThread(() -> {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

            // Display common request details
            if (requestIdTextView != null) {
                // CHANGED: Display Firestore ID instead of old int ID
                requestIdTextView.setText("Case ID: " + request.getFirestoreId());
            }

            itemTypeTextView.setText("Item Type: " + (request.getItemType() != null ? request.getItemType() : "N/A"));
            colorTextView.setText("Color: " + (request.getColor() != null ? request.getColor() : "N/A"));
            brandTextView.setText("Brand: " + (request.getBrand() != null ? request.getBrand() : "N/A"));
            ownerNameTextView.setText("Owner Name: " + (request.getOwnerName() != null ? request.getOwnerName() : "N/A"));
            lossDescriptionTextView.setText("Loss Description: " + (request.getLossDescription() != null ? request.getLossDescription() : "N/A"));

            // Handle Date and Time, ensure Date is not null before formatting
            if (request.getTripDate() != null) {
                tripDateTextView.setText("Trip Date: " + dateFormat.format(new Date(String.valueOf(request.getTripDate()))));
            } else {
                tripDateTextView.setText("Trip Date: N/A");
            }
            tripTimeTextView.setText("Trip Time: " + (request.getTripTime() != null ? request.getTripTime() : "N/A"));

            originTextView.setText("Origin: " + (request.getOrigin() != null ? request.getOrigin() : "N/A"));
            destinationTextView.setText("Destination: " + (request.getDestination() != null ? request.getDestination() : "N/A"));
            lineNumberTextView.setText("Line Number: " + (request.getLineNumber() != null ? request.getLineNumber() : "N/A"));

            fullNameTextView.setText("Reporter Full Name: " + (request.getFullName() != null ? request.getFullName() : "N/A"));
            idCardTextView.setText("Reporter ID Card: " + (request.getIdCard() != null ? request.getIdCard() : "N/A"));
            phoneNumberTextView.setText("Reporter Phone Number: " + (request.getPhoneNumber() != null ? request.getPhoneNumber() : "N/A"));
            emailTextView.setText("Reporter Email: " + (request.getEmail() != null ? request.getEmail() : "N/A"));
            cityTextView.setText("Reporter City: " + (request.getCity() != null ? request.getCity() : "N/A"));

            String status = request.getStatus();
            if (statusTextView != null) {
                statusTextView.setText("Status: " + (status != null ? status : "N/A"));
            }
            if (systemCommentsTextView != null) {
                systemCommentsTextView.setText("System Comments: " + (request.getSystemComments() != null ? request.getSystemComments() : "N/A"));
            }

            // --- Handle Location (address, coordinates, map button) ---
            String address = request.getLocationAddress();
            Double latitude = request.getLatitude();
            Double longitude = request.getLongitude();

            boolean isFoundStatus = status != null && (
                    status.equals("אבידה נמצאה") ||
                            status.equals("FOUND") ||
                            status.equals(getString(R.string.status_found)) // Keep for compatibility if used from resources
            );

            if (isFoundStatus) {
                if (locationAddressLayout != null) locationAddressLayout.setVisibility(View.VISIBLE);
                if (locationAddressLabel != null) locationAddressLabel.setVisibility(View.VISIBLE);
                if (locationAddressTextView != null) locationAddressTextView.setVisibility(View.VISIBLE);

                if (address != null && !address.trim().isEmpty()) {
                    locationAddressLabel.setText("Lost Found Department Address:"); // Changed label text
                    locationAddressTextView.setText(address);
                } else {
                    String toastMsg = "Lost & Found address not assigned yet.";
                    locationAddressLabel.setText("Lost Found Department Address:"); // Ensure label shows
                    locationAddressTextView.setText(toastMsg);
                    Toast.makeText(CaseDetailsActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                    speakIfTtsReady(toastMsg);
                }

                if (latitude != null && longitude != null) {
                    if (latLngLayout != null) latLngLayout.setVisibility(View.VISIBLE);
                    if (latitudeLabel != null) latitudeLabel.setVisibility(View.VISIBLE);
                    if (longitudeLabel != null) longitudeLabel.setVisibility(View.VISIBLE);
                    if (latitudeTextView != null) latitudeTextView.setText(String.valueOf(latitude));
                    if (longitudeTextView != null) longitudeTextView.setText(String.valueOf(longitude));
                    if (showOnMapButton != null) {
                        showOnMapButton.setVisibility(View.VISIBLE);
                        showOnMapButton.setOnClickListener(v -> {
                            String uri = "geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude + "(" + (request.getLocationAddress() != null ? request.getLocationAddress() : "Lost and Found") + ")";
                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
                            mapIntent.setPackage("com.google.android.apps.maps");
                            try {
                                startActivity(mapIntent);
                            } catch (Exception e) {
                                String toastMsg = "Google Maps not installed or error opening map.";
                                Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
                                speakIfTtsReady(toastMsg);
                                Log.e(TAG, toastMsg, e);
                            }
                        });
                    }
                } else {
                    // Hide map related views if coordinates are missing, even if address is present
                    if (latLngLayout != null) latLngLayout.setVisibility(View.GONE);
                    if (showOnMapButton != null) showOnMapButton.setVisibility(View.GONE);
                }
            } else {
                // If status is not "Found", hide all location related views
                if (locationAddressLayout != null) locationAddressLayout.setVisibility(View.GONE);
                if (locationAddressLabel != null) locationAddressLabel.setVisibility(View.GONE);
                if (locationAddressTextView != null) locationAddressTextView.setVisibility(View.GONE);
                if (latLngLayout != null) latLngLayout.setVisibility(View.GONE);
                if (showOnMapButton != null) showOnMapButton.setVisibility(View.GONE);
            }

            // --- Admin Edit Button Visibility ---
            if (userRole != null && userRole.equals("admin")) {
                editCaseButtonCard.setVisibility(View.VISIBLE);
                editCaseButton.setOnClickListener(v -> {
                    Intent editIntent = new Intent(CaseDetailsActivity.this, AdminEditCaseActivity.class);
                    // CHANGED: Pass firestoreId to AdminEditCaseActivity
                    editIntent.putExtra("REQUEST_ID", request.getFirestoreId());
                    startActivity(editIntent);
                });
            } else {
                editCaseButtonCard.setVisibility(View.GONE);
            }

            // --- Read Details Button Listener ---
            readDetailsButton.setOnClickListener(v -> {
                if (tts != null && isTtsReady) {
                    if (tts.isSpeaking()) {
                        tts.stop();
                    }
                    String detailsToSpeak = "Case ID: " + request.getFirestoreId() + ". " +
                            "Item type: " + (request.getItemType() != null ? request.getItemType() : "N/A") + ". " +
                            "Status: " + (request.getStatus() != null ? request.getStatus() : "N/A") + ". " +
                            "System comments: " + (request.getSystemComments() != null ? request.getSystemComments() : "No comments.") + ".";

                    if (isFoundStatus && address != null && !address.trim().isEmpty()) {
                        detailsToSpeak += " The item is waiting at: " + address + ".";
                    }
                    if (latitude != null && longitude != null) {
                        detailsToSpeak += " Location coordinates: latitude " + latitude + ", longitude " + longitude + ".";
                    }
                    tts.speak(detailsToSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
                } else {
                    String toastMsg = "Speech not ready. Please try again.";
                    Toast.makeText(CaseDetailsActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                    speakIfTtsReady(toastMsg);
                }
            });

            // --- Countdown Timer Setup ---
            // Ensure creationTimestamp is valid before setting up deadline
            if (request.getCreationTimestamp() != -1 && request.getCreationTimestamp() > 0) {
                deadlineMillis = request.getCreationTimestamp() + PROCESSING_TIME_MILLIS;
                if (countdownHandler == null) { // Initialize handler and runnable only once
                    countdownHandler = new Handler();
                    countdownRunnable = new Runnable() {
                        @Override
                        public void run() {
                            updateCountdown();
                            countdownHandler.postDelayed(this, 1000); // Update every second
                        }
                    };
                } else {
                    countdownHandler.removeCallbacks(countdownRunnable); // Stop any existing callbacks
                }
                countdownHandler.post(countdownRunnable); // Start or restart countdown
            } else {
                countdownTextView.setText("Processing time not available.");
                countdownTextView.setTextColor(getResources().getColor(android.R.color.darker_gray));
                if (countdownHandler != null) {
                    countdownHandler.removeCallbacks(countdownRunnable);
                }
                Log.w(TAG, "Creation timestamp is null or invalid for request: " + request.getFirestoreId());
            }
        });
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

    @SuppressLint("SetTextI18n")
    private void updateCountdown() {
        if (currentRequest == null) {
            Log.w(TAG, "updateCountdown called with null currentRequest.");
            if (countdownHandler != null) {
                countdownHandler.removeCallbacks(countdownRunnable);
            }
            return;
        }

        String status = currentRequest.getStatus(); // Get status from currentRequest object

        // Normalize status for comparison, assuming IN_PROGRESS is the status for active timer
        String normalizedStatus = status != null ? status.toUpperCase(Locale.ROOT) : "";

        // Stop timer if status is not "IN_PROGRESS"
        if (!normalizedStatus.equals("IN_PROGRESS") && !normalizedStatus.equals("פנייה בטיפול")) { // Check both English and Hebrew "In Progress"
            String toastMsg = "Case status: " + status + ". Timer stopped.";
            countdownTextView.setText(toastMsg);
            countdownTextView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark)); // Use a neutral color for closed/non-progress
            if (countdownHandler != null) {
                countdownHandler.removeCallbacks(countdownRunnable);
            }
            return; // Exit if not in progress
        }

        // If status is IN_PROGRESS, proceed with countdown logic
        long currentTime = System.currentTimeMillis();
        long timeLeft = deadlineMillis - currentTime;

        if (timeLeft <= 0) {
            String toastMsg = "Time left: Processing time elapsed.";
            countdownTextView.setText(toastMsg);
            countdownTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            if (countdownHandler != null) {
                countdownHandler.removeCallbacks(countdownRunnable);
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
            countdownTextView.setTextColor(getResources().getColor(R.color.orange_700));
        }
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
        // Remove any pending countdown callbacks to prevent leaks
        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
    }
}