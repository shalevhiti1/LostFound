package com.example.lostfound;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log; // Import for logging

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable; // Not directly used in this code, but keep if needed elsewhere
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * מסך עריכת פנייה עבור אדמין. כולל עדכון סטטוס, כתובת מחלקה, קואורדינטות, הערות מערכת ואפשרות לבחור מיקום במפה.
 * קודכן לעבודה עם Firebase Firestore.
 */
public class AdminEditCaseActivity extends AppCompatActivity {

    private static final String TAG = "AdminEditCaseActivity"; // TAG for logging

    private TextView adminEditCaseIdTextView, adminEditItemTypeTextView, adminEditReporterTextView;
    private Spinner statusSpinner;
    private EditText systemCommentsEditText;
    private EditText locationAddressEditText;
    private EditText latitudeEditText;
    private EditText longitudeEditText;
    private Button saveAdminChangesButton;
    private Button pickLocationButton;

    private DatabaseHelper dbHelper;
    private String firestoreRequestId; // CHANGED: Changed from int requestId to String firestoreRequestId
    private Request currentRequest;
    private ExecutorService executorService; // Used for background tasks, though Firebase Tasks handle threading.
    // Can be simplified or removed if only using Firebase Tasks.
    private ActivityResultLauncher<Intent> placePickerLauncher;

    private static final String SYSTEM_MESSAGE_FOUND = "We are happy to inform you that your lost item has been found. It can be picked up from our lost and found department Sunday through Thursday between the hours of 8:00 AM and 4:00 PM. To navigate to the exact location of the department, click the button below.";
    private static final String SYSTEM_MESSAGE_REJECTED = "Unfortunately, the details provided in your request are incomplete. In order for us to handle your request on the merits and efficiently, we ask that you provide us with all the details requested in the loss report form in a subsequent request, without fictitious or inaccurate details.";
    private static final String SYSTEM_MESSAGE_NOT_FOUND = "Unfortunately, your loss was not found. Thank you for contacting us.";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_edit_case);

        // Ensure Places API is initialized
        if (!Places.isInitialized()) {
            // IMPORTANT: Replace "YOUR_API_KEY" with your actual Google Cloud API Key
            // You should secure this key and restrict its usage to your app's package name
            Places.initialize(getApplicationContext(), "AIzaSyC-pDMqYGHqgu4cr-r2Ah87RSE_a4EGd7I");
        }

        dbHelper = new DatabaseHelper(this);
        // ExecutorService is still useful for other non-Firebase background tasks if any,
        // but Firebase Task API manages its own threading.
        executorService = Executors.newSingleThreadExecutor();

        // Initialize UI components
        adminEditCaseIdTextView = findViewById(R.id.adminEditCaseIdTextView);
        adminEditItemTypeTextView = findViewById(R.id.adminEditItemTypeTextView);
        adminEditReporterTextView = findViewById(R.id.adminEditReporterTextView);
        statusSpinner = findViewById(R.id.statusSpinner);
        systemCommentsEditText = findViewById(R.id.systemCommentsEditText);
        saveAdminChangesButton = findViewById(R.id.saveAdminChangesButton);
        locationAddressEditText = findViewById(R.id.locationAddressEditText);
        latitudeEditText = findViewById(R.id.latitudeEditText);
        longitudeEditText = findViewById(R.id.longitudeEditText);
        pickLocationButton = findViewById(R.id.pickLocationButton);

        // Initialize ActivityResultLauncher for Place Picker
        placePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Place place = Autocomplete.getPlaceFromIntent(result.getData());
                        LatLng latLng = place.getLatLng();
                        String address = place.getAddress();
                        if (address != null) locationAddressEditText.setText(address);
                        if (latLng != null) {
                            latitudeEditText.setText(String.valueOf(latLng.latitude));
                            longitudeEditText.setText(String.valueOf(latLng.longitude));
                        }
                    } else if (result.getResultCode() == AutocompleteActivity.RESULT_ERROR && result.getData() != null) {
                        Status status = Autocomplete.getStatusFromIntent(result.getData());
                        Log.e(TAG, "Place picker error: " + (status != null ? status.getStatusMessage() : "Unknown error"));
                        Toast.makeText(this, "Error picking location: " + (status != null ? status.getStatusMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // CHANGED: Get Firestore Request ID from Intent as String
        firestoreRequestId = getIntent().getStringExtra("REQUEST_ID");

        if (firestoreRequestId == null || firestoreRequestId.isEmpty()) {
            Toast.makeText(this, "Error: No case ID provided.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No Firestore Request ID provided in Intent.");
            finish();
            return;
        }

        // Setup Status Spinner Adapter
        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.case_statuses, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);

        // Set Listener for Spinner selection to update comments and location visibility
        statusSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedStatus = statusSpinner.getSelectedItem().toString();
                // Check if the selected status matches the "Found" string resource or its Hebrew equivalent
                boolean showLocation = selectedStatus.equals(getString(R.string.status_found)) || selectedStatus.equals("אבידה נמצאה");
                if (showLocation) {
                    locationAddressEditText.setVisibility(View.VISIBLE);
                    latitudeEditText.setVisibility(View.VISIBLE);
                    longitudeEditText.setVisibility(View.VISIBLE);
                    pickLocationButton.setVisibility(View.VISIBLE);
                } else {
                    locationAddressEditText.setVisibility(View.GONE);
                    locationAddressEditText.setText(""); // Clear text when hidden
                    latitudeEditText.setVisibility(View.GONE);
                    latitudeEditText.setText(""); // Clear text when hidden
                    longitudeEditText.setVisibility(View.GONE);
                    longitudeEditText.setText(""); // Clear text when hidden
                    pickLocationButton.setVisibility(View.GONE);
                }

                // Update system comments based on selected status
                if (selectedStatus.equals(getString(R.string.status_found)) || selectedStatus.equals("אבידה נמצאה")) {
                    systemCommentsEditText.setText(SYSTEM_MESSAGE_FOUND);
                } else if (selectedStatus.equals(getString(R.string.status_rejected)) || selectedStatus.equals("Rejected") || selectedStatus.equals("פנייה נדחתה")) { // Added Hebrew for Rejected
                    systemCommentsEditText.setText(SYSTEM_MESSAGE_REJECTED);
                } else if (selectedStatus.equals(getString(R.string.status_not_found)) || selectedStatus.equals("Not found") || selectedStatus.equals("אבידה לא נמצאה")) { // Added Hebrew for Not found
                    systemCommentsEditText.setText(SYSTEM_MESSAGE_NOT_FOUND);
                } else {
                    // For "In Progress" or other statuses, potentially keep existing comments or clear them.
                    // If you want to clear for other statuses, uncomment the next line:
                    // systemCommentsEditText.setText("");
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Set OnClickListener for buttons
        pickLocationButton.setOnClickListener(v -> openPlacePicker());
        saveAdminChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAdminChanges();
            }
        });

        // Load case details from Firebase
        loadCaseDetails();
    }

    /**
     * Opens the Google Places Autocomplete picker for location selection.
     */
    private void openPlacePicker() {
        List<Place.Field> fields = Arrays.asList(Place.Field.LAT_LNG, Place.Field.ADDRESS);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                .build(this);
        placePickerLauncher.launch(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shut down the executor service to prevent memory leaks
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    /**
     * Loads the case details from Firebase Firestore using the new DatabaseHelper methods.
     * Updates UI on the main thread.
     */
    private void loadCaseDetails() {
        // Firebase operations are asynchronous, so we call the Task and handle its result.
        // No explicit executorService.execute() needed here for the Firebase call itself.
        dbHelper.getRequestById(firestoreRequestId)
                .addOnSuccessListener(request -> {
                    if (request != null) {
                        currentRequest = request; // Store the fetched request
                        // Update UI on the main thread
                        runOnUiThread(() -> {
                            // CHANGED: Display Firestore ID instead of old int ID
                            adminEditCaseIdTextView.setText("Case ID: " + currentRequest.getFirestoreId());
                            adminEditItemTypeTextView.setText("Item Type: " + currentRequest.getItemType());
                            adminEditReporterTextView.setText("Reporter: " + currentRequest.getFullName() + " (" + currentRequest.getUsername() + ")");
                            systemCommentsEditText.setText(currentRequest.getSystemComments());

                            // Set spinner selection based on current status
                            ArrayAdapter<CharSequence> statusAdapter = (ArrayAdapter<CharSequence>) statusSpinner.getAdapter();
                            if (currentRequest.getStatus() != null && statusAdapter != null) {
                                int spinnerPosition = statusAdapter.getPosition(currentRequest.getStatus());
                                if (spinnerPosition == -1) {
                                    // Fallback if the status string from DB doesn't match spinner items
                                    spinnerPosition = statusAdapter.getPosition(currentRequest.getStatusEnum().getDisplayName());
                                    if (spinnerPosition == -1) {
                                        spinnerPosition = 0; // Default to first item if still not found
                                    }
                                }
                                statusSpinner.setSelection(spinnerPosition);
                            }

                            // Show/hide location fields based on current status
                            String currentStatusString = currentRequest.getStatus();
                            boolean showLocation = currentStatusString.equals(getString(R.string.status_found)) || currentStatusString.equals("אבידה נמצאה");
                            if (showLocation) {
                                locationAddressEditText.setVisibility(View.VISIBLE);
                                locationAddressEditText.setText(currentRequest.getLocationAddress() != null ? currentRequest.getLocationAddress() : "");
                                latitudeEditText.setVisibility(View.VISIBLE);
                                longitudeEditText.setVisibility(View.VISIBLE);
                                latitudeEditText.setText(currentRequest.getLatitude() != null ? String.valueOf(currentRequest.getLatitude()) : "");
                                longitudeEditText.setText(currentRequest.getLongitude() != null ? String.valueOf(currentRequest.getLongitude()) : "");
                                pickLocationButton.setVisibility(View.VISIBLE);
                            } else {
                                locationAddressEditText.setVisibility(View.GONE);
                                locationAddressEditText.setText("");
                                latitudeEditText.setVisibility(View.GONE);
                                latitudeEditText.setText("");
                                longitudeEditText.setVisibility(View.GONE);
                                longitudeEditText.setText("");
                                pickLocationButton.setVisibility(View.GONE);
                            }
                        });
                    } else {
                        // Request not found (null result)
                        runOnUiThread(() -> {
                            Toast.makeText(AdminEditCaseActivity.this, "Error: Case not found.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Request not found for ID: " + firestoreRequestId);
                            finish();
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle error during Firebase fetch
                    runOnUiThread(() -> {
                        Toast.makeText(AdminEditCaseActivity.this, "Failed to load case: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error loading case details for ID: " + firestoreRequestId, e);
                        finish();
                    });
                });
    }

    /**
     * Saves the admin's changes to the request in Firebase Firestore.
     * Handles input validation and updates the UI based on success/failure.
     */
    private void saveAdminChanges() {
        if (currentRequest == null) {
            Toast.makeText(this, "Error: No case to save.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Attempted to save changes but currentRequest is null.");
            return;
        }

        String newStatus = statusSpinner.getSelectedItem().toString();
        String newSystemComments = systemCommentsEditText.getText().toString().trim();
        String locationAddress = locationAddressEditText.getText().toString().trim();
        String latitudeString = latitudeEditText.getText().toString().trim();
        String longitudeString = longitudeEditText.getText().toString().trim();

        boolean showLocation = newStatus.equals(getString(R.string.status_found)) || newStatus.equals("אבידה נמצאה");

        // Input validation
        if (showLocation && TextUtils.isEmpty(locationAddress)) {
            Toast.makeText(this, "You must enter a location address for 'אבידה נמצאה'.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(newSystemComments)) {
            Toast.makeText(this, "System comments cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update the currentRequest object
        currentRequest.setStatus(newStatus); // This setter also updates statusEnum internally
        currentRequest.setSystemComments(newSystemComments);

        if (showLocation) {
            currentRequest.setLocationAddress(locationAddress);

            Double latitude = null, longitude = null;
            if (!TextUtils.isEmpty(latitudeString)) {
                try {
                    latitude = Double.parseDouble(latitudeString);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid latitude format: " + latitudeString, e);
                    Toast.makeText(this, "Invalid latitude format.", Toast.LENGTH_SHORT).show();
                    return; // Prevent saving with bad data
                }
            }
            if (!TextUtils.isEmpty(longitudeString)) {
                try {
                    longitude = Double.parseDouble(longitudeString);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid longitude format: " + longitudeString, e);
                    Toast.makeText(this, "Invalid longitude format.", Toast.LENGTH_SHORT).show();
                    return; // Prevent saving with bad data
                }
            }
            currentRequest.setLatitude(latitude);
            currentRequest.setLongitude(longitude);
        } else {
            // Clear location data if status is not "Found"
            currentRequest.setLocationAddress(null);
            currentRequest.setLatitude(null);
            currentRequest.setLongitude(null);
        }

        // Call the asynchronous update method in DatabaseHelper
        // No explicit executorService.execute() needed for the Firebase call itself.
        dbHelper.updateRequest(currentRequest)
                .addOnSuccessListener(isSuccess -> {
                    // This isSuccess will be true if the Firestore operation completed successfully.
                    // dbHelper.updateRequest now returns Task<Boolean>.
                    if (isSuccess) {
                        runOnUiThread(() -> {
                            Toast.makeText(AdminEditCaseActivity.this, "Case updated successfully!", Toast.LENGTH_SHORT).show();
                            finish(); // Close activity on success
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(AdminEditCaseActivity.this, "Failed to update case. Please try again.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Firestore update operation reported as not successful for request ID: " + currentRequest.getFirestoreId());
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle any exceptions during the Firestore update process
                    runOnUiThread(() -> {
                        Toast.makeText(AdminEditCaseActivity.this, "Failed to update case: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error updating case details for ID: " + currentRequest.getFirestoreId(), e);
                    });
                });
    }
}