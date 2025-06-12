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
 */
public class CaseDetailsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ExecutorService executorService;
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

    private static final long PROCESSING_TIME_MILLIS = 3 * 24 * 60 * 60 * 1000L;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_case_details);

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

        int requestId = getIntent().getIntExtra("REQUEST_ID", -1);
        loggedInUsername = getIntent().getStringExtra("username");

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

        if (requestId != -1) {
            executorService.execute(() -> {
                Request request = dbHelper.getRequestById(requestId);
                String userRole = null;
                if (loggedInUsername != null) {
                    userRole = dbHelper.getUserRole(loggedInUsername);
                }

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

                        String status = request.getStatus();
                        if (statusTextView != null) {
                            statusTextView.setText("Status: " + status);
                        }
                        if (systemCommentsTextView != null) {
                            systemCommentsTextView.setText("System Comments: " + request.getSystemComments());
                        }

                        if (status != null && (
                                status.equals("אבידה נמצאה") ||
                                        status.equals("Found") ||
                                        status.equals(getString(R.string.status_found))
                        )) {
                            if (locationAddressLabel != null) locationAddressLabel.setVisibility(View.VISIBLE);
                            if (locationAddressTextView != null) {
                                locationAddressTextView.setVisibility(View.VISIBLE);
                                String addr = request.getLocationAddress();
                                if (addr != null && !addr.trim().isEmpty()) {
                                    locationAddressTextView.setText(addr);
                                } else {
                                    String toastMsg = "Lost & Found address not assigned yet.";
                                    locationAddressTextView.setText(toastMsg);
                                    Toast.makeText(CaseDetailsActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                                    speakIfTtsReady(toastMsg);
                                }
                            }
                        } else {
                            if (locationAddressLabel != null) locationAddressLabel.setVisibility(View.GONE);
                            if (locationAddressTextView != null) locationAddressTextView.setVisibility(View.GONE);
                        }

                        String address = request.getLocationAddress();
                        Double latitude = request.getLatitude();
                        Double longitude = request.getLongitude();
                        if (latitude != null && longitude != null && address != null) {
                            if (latLngLayout != null) latLngLayout.setVisibility(View.VISIBLE);
                            if (latitudeLabel != null) latitudeLabel.setVisibility(View.VISIBLE);
                            if (longitudeLabel != null) longitudeLabel.setVisibility(View.VISIBLE);
                            if (locationAddressLayout != null) locationAddressLayout.setVisibility(View.VISIBLE);
                            if (locationAddressLabel != null) locationAddressLabel.setVisibility(View.VISIBLE);
                            if (locationAddressLabel != null) locationAddressLabel.setText("Lost Found Department Address:"+ address);
                            if (locationAddressTextView != null) locationAddressTextView.setText(address);
                            if (latitudeTextView != null) latitudeTextView.setText(String.valueOf(latitude));
                            if (longitudeTextView != null) longitudeTextView.setText(String.valueOf(longitude));
                            if (showOnMapButton != null) {
                                showOnMapButton.setVisibility(View.VISIBLE);
                                showOnMapButton.setOnClickListener(v -> {
                                    String uri = "geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude + "(" + (request.getLocationAddress() != null ? request.getLocationAddress() : "") + ")";
                                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
                                    mapIntent.setPackage("com.google.android.apps.maps");
                                    try {
                                        startActivity(mapIntent);
                                    } catch (Exception e) {
                                        String toastMsg = "Google Maps not installed.";
                                        Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
                                        speakIfTtsReady(toastMsg);
                                    }
                                });
                            }
                        } else {
                            if (latLngLayout != null) latLngLayout.setVisibility(View.GONE);
                            if (showOnMapButton != null) showOnMapButton.setVisibility(View.GONE);
                        }

                        if (finalUserRole != null && finalUserRole.equals("admin")) {
                            editCaseButtonCard.setVisibility(View.VISIBLE);
                            editCaseButton.setOnClickListener(v -> {
                                Intent editIntent = new Intent(CaseDetailsActivity.this, AdminEditCaseActivity.class);
                                editIntent.putExtra("REQUEST_ID", request.getId());
                                startActivity(editIntent);
                            });
                        } else {
                            editCaseButtonCard.setVisibility(View.GONE);
                        }

                        readDetailsButton.setOnClickListener(v -> {
                            if (tts != null && isTtsReady) {
                                if (tts.isSpeaking()) {
                                    tts.stop();
                                }
                                String detailsToSpeak = "Case ID: " + request.getId() + ". " +
                                        "Item type: " + request.getItemType() + ". " +
                                        "Status: " + request.getStatus() + ". " +
                                        "System comments: " + request.getSystemComments() + ".";
                                if (status != null && (
                                        status.equals("אבידה נמצאה") ||
                                                status.equals("Found") ||
                                                status.equals(getString(R.string.status_found))
                                )) {
                                    String addr = request.getLocationAddress();
                                    if (addr != null && !addr.trim().isEmpty()) {
                                        detailsToSpeak += " The item is waiting at: " + addr + ".";
                                    }
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

                        deadlineMillis = request.getCreationTimestamp() + PROCESSING_TIME_MILLIS;
                        countdownHandler = new Handler();
                        countdownRunnable = new Runnable() {
                            @Override
                            public void run() {
                                updateCountdown();
                                countdownHandler.postDelayed(this, 1000);
                            }
                        };
                        countdownHandler.post(countdownRunnable);
                    } else {
                        String toastMsg = "Request not found.";
                        Toast.makeText(CaseDetailsActivity.this, toastMsg, Toast.LENGTH_LONG).show();
                        speakIfTtsReady(toastMsg);
                        finish();
                    }
                });
            });
        } else {
            String toastMsg = "No request ID provided.";
            Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
            speakIfTtsReady(toastMsg);
            finish();
        }
    }

    private void speakIfTtsReady(String text) {
        if (tts != null && isTtsReady) {
            if (tts.isSpeaking()) {
                tts.stop();
            }
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateCountdown() {
        long currentTime = System.currentTimeMillis();
        long timeLeft = deadlineMillis - currentTime;
        int requestId = getIntent().getIntExtra("REQUEST_ID", -1);
        Request request=dbHelper.getRequestById(requestId);
        String status=request.getStatus();
        if (!status.equals("IN_PROGRESS")){
            String toastMsg = "Case closed.";

            countdownTextView.setText(toastMsg);
            countdownTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            if (countdownHandler != null) {
                countdownHandler.removeCallbacks(countdownRunnable);
            }
        }
        else if (timeLeft <= 0) {
            String toastMsg = "Time left: Processing time elapsed.";

            countdownTextView.setText(toastMsg);
            countdownTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            if (countdownHandler != null) {
                countdownHandler.removeCallbacks(countdownRunnable);
            }
        }
        else {
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
        executorService.shutdown();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (countdownHandler != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
    }
}