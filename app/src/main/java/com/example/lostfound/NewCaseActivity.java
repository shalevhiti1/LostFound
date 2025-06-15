package com.example.lostfound;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log; // Added for logging

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task; // Added for Firebase Tasks
import com.google.android.gms.tasks.Tasks; // Added for Firebase Tasks (e.g., Tasks.whenAllSuccess)

import java.text.SimpleDateFormat;
import java.util.ArrayList; // Added for collecting Tasks
import java.util.Calendar;
import java.util.Date;
import java.util.List; // Added for collecting Tasks
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.speech.tts.TextToSpeech; // ייבוא TextToSpeech, לממשק טקסט לדיבור.

// ייבוא לפונקציה הסטטית של הנוטיפיקציה
import static com.example.lostfound.NotificationUtils.scheduleNotification;
import static com.example.lostfound.NotificationUtils.showSimpleNotification;

/**
 * המחלקה {@code NewCaseActivity} מאפשרת למשתמשים רגילים לפתוח דיווח חדש על אבידה.
 * היא מציגה שדות קלט למילוי פרטים מקיפים על הפריט שאבד ועל הנסיעה שבה האבידה התרחשה.
 * לאחר מילוי הפרטים ושמירתם, המידע נשמר במסד הנתונים, והמשתמש מקבל אישור (כולל נוטיפיקציה).
 * טעינת הנתונים ושמירתם מתבצעות ב-Thread רקע כדי למנוע תקיעות בממשק הממשק.
 * קודכן לעבודה עם Firebase Firestore.
 */
public class NewCaseActivity extends AppCompatActivity {

    private static final String TAG = "NewCaseActivity"; // Added TAG for logging

    // הצהרה על משתני ממשק המשתמש (EditText ו-Button).
    private EditText itemTypeEditText, colorEditText, brandEditText, ownerNameEditText, lossDescriptionEditText;
    private EditText tripDateEditText, tripTimeEditText, originEditText, destinationEditText, lineNumberEditText;
    private Button saveCaseButton;
    private DatabaseHelper dbHelper;
    private String username;
    private Calendar calendar;
    private ExecutorService executorService; // ExecutorService is not strictly needed for Firebase Tasks, but kept if other async ops are planned.

    private TextToSpeech tts; // TTS instance
    private boolean isTtsReady = false; // Flag for TTS initialization status

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_case);

        // Initialize DatabaseHelper (now Firebase-based) and ExecutorService
        dbHelper = new DatabaseHelper(this);
        executorService = Executors.newSingleThreadExecutor();
        username = getIntent().getStringExtra("username");

        // Initialize UI components
        itemTypeEditText = findViewById(R.id.itemTypeEditText);
        colorEditText = findViewById(R.id.colorEditText);
        brandEditText = findViewById(R.id.brandEditText);
        ownerNameEditText = findViewById(R.id.ownerNameEditText);
        lossDescriptionEditText = findViewById(R.id.lossDescriptionEditText);
        tripDateEditText = findViewById(R.id.tripDateEditText);
        tripTimeEditText = findViewById(R.id.tripTimeEditText);
        originEditText = findViewById(R.id.originEditText);
        destinationEditText = findViewById(R.id.destinationEditText);
        lineNumberEditText = findViewById(R.id.lineNumberEditText);
        saveCaseButton = findViewById(R.id.saveCaseButton);
        calendar = Calendar.getInstance();

        // Initialize TextToSpeech engine
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

        // Set up DatePickerDialog for tripDateEditText
        DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, monthOfYear);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateLabel();
            }
        };

        tripDateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(NewCaseActivity.this, date, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        // Set OnClickListener for Save Case button
        saveCaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCaseToDatabase();
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
     * Updates the text of the tripDateEditText with the selected date from the Calendar.
     */
    private void updateDateLabel() {
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        tripDateEditText.setText(sdf.format(calendar.getTime()));
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
     * אוספת את הנתונים משדות הקלט, מבצעת ולידציה בסיסית,
     * שולפת פרטי משתמש נוספים ממסד הנתונים (Firebase Firestore), יוצרת אובייקט {@code Request} חדש,
     * ושומרת אותו במסד הנתונים. פעולות אלו מתבצעות באופן אסינכרוני.
     * לאחר שמירה מוצלחת, מוצגת הודעת Toast ונוטיפיקציה, והאקטיביטי נסגר.
     */
    private void saveCaseToDatabase() {
        String itemType = itemTypeEditText.getText().toString().trim();
        String color = colorEditText.getText().toString().trim();
        String brand = brandEditText.getText().toString().trim();
        String ownerName = ownerNameEditText.getText().toString().trim();
        String lossDescription = lossDescriptionEditText.getText().toString().trim();
        Date tripDate = calendar.getTime(); // Get the selected date
        String tripTime = tripTimeEditText.getText().toString().trim();
        String origin = originEditText.getText().toString().trim();
        String destination = destinationEditText.getText().toString().trim();
        String lineNumber = lineNumberEditText.getText().toString().trim();

        // ולידציה בסיסית: ודא שכל שדות החובה אינם ריקים.
        if (TextUtils.isEmpty(itemType) || TextUtils.isEmpty(color) || TextUtils.isEmpty(brand) ||
                TextUtils.isEmpty(ownerName) || TextUtils.isEmpty(lossDescription) || TextUtils.isEmpty(tripTime) ||
                TextUtils.isEmpty(origin) || TextUtils.isEmpty(destination) || TextUtils.isEmpty(lineNumber)) {
            String toastMsg = "Please fill in all fields.";
            Toast.makeText(NewCaseActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
            speakIfTtsReady(toastMsg);
            return;
        }

        Log.d(TAG, "Attempting to save new case for user: " + username);

        // Fetch user details first, then proceed to save the request
        List<Task<String>> fetchUserDetailsTasks = new ArrayList<>();
        fetchUserDetailsTasks.add(dbHelper.getUserFullName(username));
        fetchUserDetailsTasks.add(dbHelper.getUserIdCard(username));
        fetchUserDetailsTasks.add(dbHelper.getUserPhoneNumber(username));
        fetchUserDetailsTasks.add(dbHelper.getUserEmail(username));
        fetchUserDetailsTasks.add(dbHelper.getUserCity(username));

        // Use Tasks.whenAllSuccess to wait for all user detail fetches to complete
        Tasks.whenAllSuccess(fetchUserDetailsTasks)
                .addOnSuccessListener(results -> {
                    // results will contain the String values in the order they were added to fetchUserDetailsTasks
                    String fullName = (String) results.get(0);
                    String idCard = (String) results.get(1);
                    String phoneNumber = (String) results.get(2);
                    String email = (String) results.get(3);
                    String city = (String) results.get(4);

                    long currentTimestamp = System.currentTimeMillis();

                    // Create a new Request object
                    Request newRequest = new Request(username, fullName, idCard, phoneNumber, email, city,
                            itemType, color, brand, ownerName, lossDescription,
                            tripDate, tripTime, origin, destination, lineNumber, // tripDate.getTime() to get long timestamp
                            currentTimestamp);

                    // Now, add the request to Firebase Firestore
                    dbHelper.addRequest(newRequest)
                            .addOnSuccessListener(firestoreId -> {
                                runOnUiThread(() -> {
                                    if (firestoreId != null) { // firestoreId is the String document ID
                                        String toastMsg = "Case saved successfully. ID: " + firestoreId;
                                        Toast.makeText(NewCaseActivity.this, toastMsg, Toast.LENGTH_LONG).show();
                                        speakIfTtsReady(toastMsg);
                                        Log.i(TAG, "New case saved with Firestore ID: " + firestoreId);

                                        // Note: For notification IDs, an int is usually preferred.
                                        // You might need a consistent way to map firestoreId (String) to an int,
                                        // or use a random int for simplicity if unique notification ID per request isn't strictly required.
                                        // Using a simple hash for now as a placeholder, but this might lead to collisions.
                                        int notificationId = firestoreId.hashCode(); // Potentially problematic for uniqueness
                                        if (notificationId < 0) notificationId = -notificationId; // Ensure positive

                                        showSimpleNotification(
                                                NewCaseActivity.this,
                                                "Case Opened Successfully!",
                                                "Your case ID " + firestoreId + " has been opened and is being processed.",
                                                notificationId + 1000 // Offset to avoid collision with scheduleNotification
                                        );

                                        long threeDaysInMillis = 3 * 24 * 60 * 60 * 1000L;
                                        long futureTime = currentTimestamp + threeDaysInMillis;

                                        scheduleNotification(
                                                NewCaseActivity.this,
                                                "Case Update Reminder",
                                                "Your case ID " + firestoreId + " is still being processed. We will update you soon.",
                                                notificationId, // Use the base hashcode for scheduled notification
                                                futureTime
                                        );

                                        finish(); // Close activity after successful save and notifications
                                    } else {
                                        String toastMsg = "Failed to save case. Firestore ID was null.";
                                        Toast.makeText(NewCaseActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                                        speakIfTtsReady(toastMsg);
                                        Log.e(TAG, "addRequest returned null Firestore ID.");
                                    }
                                });
                            })
                            .addOnFailureListener(e -> {
                                // Error adding the request to Firestore
                                runOnUiThread(() -> {
                                    String toastMsg = "Failed to save case: " + e.getMessage();
                                    Toast.makeText(NewCaseActivity.this, toastMsg, Toast.LENGTH_LONG).show();
                                    speakIfTtsReady(toastMsg);
                                    Log.e(TAG, "Error adding request to Firestore for user: " + username, e);
                                });
                            });
                })
                .addOnFailureListener(e -> {
                    // Error fetching user details
                    runOnUiThread(() -> {
                        String toastMsg = "Failed to get user details: " + e.getMessage();
                        Toast.makeText(NewCaseActivity.this, toastMsg, Toast.LENGTH_LONG).show();
                        speakIfTtsReady(toastMsg);
                        Log.e(TAG, "Error fetching user details for " + username + " when creating new case.", e);
                    });
                });
    }
}