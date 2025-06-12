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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
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
 */
public class AdminEditCaseActivity extends AppCompatActivity {

    private TextView adminEditCaseIdTextView, adminEditItemTypeTextView, adminEditReporterTextView;
    private Spinner statusSpinner;
    private EditText systemCommentsEditText;
    private EditText locationAddressEditText;
    private EditText latitudeEditText;
    private EditText longitudeEditText;
    private Button saveAdminChangesButton;
    private Button pickLocationButton;

    private DatabaseHelper dbHelper;
    private int requestId;
    private Request currentRequest;
    private ExecutorService executorService;
    private ActivityResultLauncher<Intent> placePickerLauncher;

    private static final String SYSTEM_MESSAGE_FOUND = "We are happy to inform you that your lost item has been found. It can be picked up from our lost and found department Sunday through Thursday between the hours of 8:00 AM and 4:00 PM. To navigate to the exact location of the department, click the button below.";
    private static final String SYSTEM_MESSAGE_REJECTED = "Unfortunately, the details provided in your request are incomplete. In order for us to handle your request on the merits and efficiently, we ask that you provide us with all the details requested in the loss report form in a subsequent request, without fictitious or inaccurate details.";
    private static final String SYSTEM_MESSAGE_NOT_FOUND = "Unfortunately, your loss was not found. Thank you for contacting us.";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_edit_case);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyC-pDMqYGHqgu4cr-r2Ah87RSE_a4EGd7I");
        }

        dbHelper = new DatabaseHelper(this);
        executorService = Executors.newSingleThreadExecutor();

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
                        Toast.makeText(this, "Error: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );

        requestId = getIntent().getIntExtra("REQUEST_ID", -1);

        if (requestId == -1) {
            Toast.makeText(this, "Error: No case ID provided.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.case_statuses, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);

        statusSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedStatus = statusSpinner.getSelectedItem().toString();
                boolean showLocation = selectedStatus.equals(getString(R.string.status_found)) || selectedStatus.equals("אבידה נמצאה");
                if (showLocation) {
                    locationAddressEditText.setVisibility(View.VISIBLE);
                    latitudeEditText.setVisibility(View.VISIBLE);
                    longitudeEditText.setVisibility(View.VISIBLE);
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

                if (selectedStatus.equals(getString(R.string.status_found)) || selectedStatus.equals("אבידה נמצאה")) {
                    systemCommentsEditText.setText(SYSTEM_MESSAGE_FOUND);
                } else if (selectedStatus.equals(getString(R.string.status_rejected)) || selectedStatus.equals("Rejected")) {
                    systemCommentsEditText.setText(SYSTEM_MESSAGE_REJECTED);
                } else if (selectedStatus.equals(getString(R.string.status_not_found)) || selectedStatus.equals("Not found")) {
                    systemCommentsEditText.setText(SYSTEM_MESSAGE_NOT_FOUND);
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        pickLocationButton.setOnClickListener(v -> openPlacePicker());

        saveAdminChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAdminChanges();
            }
        });

        loadCaseDetails();
    }

    private void openPlacePicker() {
        List<Place.Field> fields = Arrays.asList(Place.Field.LAT_LNG, Place.Field.ADDRESS);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                .build(this);
        placePickerLauncher.launch(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    /**
     * טוען את פרטי הפנייה, ממלא את השדות ומציג כתובת/קואורדינטות רק אם צריך.
     */
    private void loadCaseDetails() {
        executorService.execute(() -> {
            currentRequest = dbHelper.getRequestById(requestId);

            runOnUiThread(() -> {
                if (currentRequest != null) {
                    adminEditCaseIdTextView.setText("Case ID: " + currentRequest.getId());
                    adminEditItemTypeTextView.setText("Item Type: " + currentRequest.getItemType());
                    adminEditReporterTextView.setText("Reporter: " + currentRequest.getFullName() + " (" + currentRequest.getUsername() + ")");
                    systemCommentsEditText.setText(currentRequest.getSystemComments());

                    ArrayAdapter<CharSequence> statusAdapter = (ArrayAdapter<CharSequence>) statusSpinner.getAdapter();
                    if (currentRequest.getStatus() != null && statusAdapter != null) {
                        int spinnerPosition = statusAdapter.getPosition(currentRequest.getStatus());
                        if (spinnerPosition == -1) spinnerPosition = 0;
                        statusSpinner.setSelection(spinnerPosition);
                    }

                    String currentStatus = currentRequest.getStatus();
                    boolean showLocation = currentStatus.equals(getString(R.string.status_found)) || currentStatus.equals("אבידה נמצאה");
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
                } else {
                    Toast.makeText(AdminEditCaseActivity.this, "Error: Case not found.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    /**
     * שומר את השינויים שביצע האדמין בפנייה.
     */
    private void saveAdminChanges() {
        if (currentRequest == null) {
            Toast.makeText(this, "Error: No case to save.", Toast.LENGTH_SHORT).show();
            return;
        }

        String newStatus = statusSpinner.getSelectedItem().toString();
        String newSystemComments = systemCommentsEditText.getText().toString().trim();
        String locationAddress = locationAddressEditText.getText().toString().trim();
        String latitudeString = latitudeEditText.getText().toString().trim();
        String longitudeString = longitudeEditText.getText().toString().trim();

        boolean showLocation = newStatus.equals(getString(R.string.status_found)) || newStatus.equals("אבידה נמצאה");

        if (showLocation && TextUtils.isEmpty(locationAddress)) {
            Toast.makeText(this, "You must enter a location address for 'אבידה נמצאה'.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(newSystemComments)) {
            Toast.makeText(this, "System comments cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        currentRequest.setStatus(newStatus);
        currentRequest.setSystemComments(newSystemComments);

        if (showLocation) {
            currentRequest.setLocationAddress(locationAddress);

            Double latitude = null, longitude = null;
            if (!TextUtils.isEmpty(latitudeString)) {
                try {
                    latitude = Double.parseDouble(latitudeString);
                } catch (NumberFormatException ignored) {}
            }
            if (!TextUtils.isEmpty(longitudeString)) {
                try {
                    longitude = Double.parseDouble(longitudeString);
                } catch (NumberFormatException ignored) {}
            }
            currentRequest.setLatitude(latitude);
            currentRequest.setLongitude(longitude);
        } else {
            currentRequest.setLocationAddress(null);
            currentRequest.setLatitude(null);
            currentRequest.setLongitude(null);
        }

        executorService.execute(() -> {
            boolean isUpdated = dbHelper.updateRequest(currentRequest);

            runOnUiThread(() -> {
                if (isUpdated) {
                    Toast.makeText(AdminEditCaseActivity.this, "Case updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AdminEditCaseActivity.this, "Failed to update case. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}