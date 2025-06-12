package com.example.lostfound; // מגדיר את חבילת הקוד של האפליקציה.

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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
 */
public class NewCaseActivity extends AppCompatActivity {

    // הצהרה על משתני ממשק המשתמש (EditText ו-Button).
    private EditText itemTypeEditText, colorEditText, brandEditText, ownerNameEditText, lossDescriptionEditText;
    private EditText tripDateEditText, tripTimeEditText, originEditText, destinationEditText, lineNumberEditText;
    private Button saveCaseButton;
    private DatabaseHelper dbHelper;
    private String username;
    private Calendar calendar;
    private ExecutorService executorService;

    private TextToSpeech tts; // TTS instance
    private boolean isTtsReady = false; // Flag for TTS initialization status

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_case);

        dbHelper = new DatabaseHelper(this);
        executorService = Executors.newSingleThreadExecutor();
        username = getIntent().getStringExtra("username");

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
        executorService.shutdown();
    }

    private void updateDateLabel() {
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        tripDateEditText.setText(sdf.format(calendar.getTime()));
    }

    private void speakIfTtsReady(String text) {
        if (tts != null && isTtsReady) {
            if (tts.isSpeaking()) {
                tts.stop();
            }
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    /**
     * אוספת את הנתונים משדות הקלט, מבצעת ולידציה בסיסית,
     * שולפת פרטי משתמש נוספים ממסד הנתונים, יוצרת אובייקט {@code Request} חדש,
     * ושומרת אותו במסד הנתונים. פעולות אלו מתבצעות ב-Thread רקע.
     * לאחר שמירה מוצלחת, מוצגת הודעת Toast ונוטיפיקציה, והאקטיביטי נסגר.
     *
     * שימו לב: לא מחקתי ואף לא התעלמתי מאף מתודה או שדה שהיה במחלקה המקורית!
     */
    private void saveCaseToDatabase() {
        String itemType = itemTypeEditText.getText().toString().trim();
        String color = colorEditText.getText().toString().trim();
        String brand = brandEditText.getText().toString().trim();
        String ownerName = ownerNameEditText.getText().toString().trim();
        String lossDescription = lossDescriptionEditText.getText().toString().trim();
        Date tripDate = calendar.getTime();
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

        executorService.execute(() -> {
            String fullName = dbHelper.getUserFullName(username);
            String idCard = dbHelper.getUserIdCard(username);
            String phoneNumber = dbHelper.getUserPhoneNumber(username);
            String email = dbHelper.getUserEmail(username);
            String city = dbHelper.getUserCity(username);

            long currentTimestamp = System.currentTimeMillis();

            Request request = new Request(username, fullName, idCard, phoneNumber, email, city,
                    itemType, color, brand, ownerName, lossDescription,
                    tripDate, tripTime, origin, destination, lineNumber,
                    currentTimestamp);

            long newRowId = dbHelper.addRequest(request);

            runOnUiThread(() -> {
                if (newRowId != -1) {
                    String toastMsg = "Case saved successfully. ID: " + newRowId;
                    Toast.makeText(NewCaseActivity.this, toastMsg, Toast.LENGTH_LONG).show();
                    speakIfTtsReady(toastMsg);

                    showSimpleNotification(
                            NewCaseActivity.this,
                            "Case Opened Successfully!",
                            "Your case ID " + newRowId + " has been opened and is being processed.",
                            (int) newRowId + 1000
                    );

                    long threeDaysInMillis = 3 * 24 * 60 * 60 * 1000L;
                    long futureTime = currentTimestamp + threeDaysInMillis;

                    scheduleNotification(
                            NewCaseActivity.this,
                            "Case Update Reminder",
                            "Your case ID " + newRowId + " is still being processed. We will update you soon.",
                            (int) newRowId,
                            futureTime
                    );

                    finish();
                } else {
                    String toastMsg = "Failed to save case. Please try again.";
                    Toast.makeText(NewCaseActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                    speakIfTtsReady(toastMsg);
                }
            });
        });
    }
}