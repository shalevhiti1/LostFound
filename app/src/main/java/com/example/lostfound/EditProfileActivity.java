package com.example.lostfound;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.speech.tts.TextToSpeech; // ייבוא TextToSpeech

import java.util.Locale; // ייבוא Locale ל-TTS

/**
 * מסך עריכת פרטי משתמש: מאפשר עדכון שם, טלפון, אימייל ועיר.
 */
public class EditProfileActivity extends AppCompatActivity {

    private EditText editUsername, editFullName, editIdCard, editPhoneNumber, editEmail, editCity;
    private Button saveProfileButton;
    private DatabaseHelper dbHelper;
    private String currentUsername;
    private ExecutorService executorService;

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

        dbHelper = new DatabaseHelper(this);
        executorService = Executors.newSingleThreadExecutor();

        // אתחול TTS
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

        currentUsername = getIntent().getStringExtra("username");
        if (currentUsername == null || currentUsername.isEmpty()) {
            String errorMsg = "error: username not found.";
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            speakIfTtsReady(errorMsg);
            finish();
            return;
        }

        editUsername = findViewById(R.id.editUsername);
        editFullName = findViewById(R.id.editFullName);
        editIdCard = findViewById(R.id.editIdCard);
        editPhoneNumber = findViewById(R.id.editPhoneNumber);
        editEmail = findViewById(R.id.editEmail);
        editCity = findViewById(R.id.editCity);
        saveProfileButton = findViewById(R.id.saveProfileButton);

        loadUserDetails();

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
        executorService.shutdown();
        // כיבוי TTS
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    /**
     * טוען את פרטי המשתמש ומציג אותם בשדות.
     */
    private void loadUserDetails() {
        executorService.execute(() -> {
            final String fullName = dbHelper.getUserFullName(currentUsername);
            final String idCard = dbHelper.getUserIdCard(currentUsername);
            final String phoneNumber = dbHelper.getUserPhoneNumber(currentUsername);
            final String email = dbHelper.getUserEmail(currentUsername);
            final String city = dbHelper.getUserCity(currentUsername);

            runOnUiThread(() -> {
                editUsername.setText(currentUsername);
                editFullName.setText(fullName);
                editIdCard.setText(idCard);
                editPhoneNumber.setText(phoneNumber);
                editEmail.setText(email);
                editCity.setText(city);
            });
        });
    }

    /**
     * שומר את השינויים על המשתמש במסד הנתונים.
     */
    private void saveUserDetails() {
        String fullName = editFullName.getText().toString().trim();
        String idCard = editIdCard.getText().toString().trim();
        String phoneNumber = editPhoneNumber.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String city = editCity.getText().toString().trim();

        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(idCard) ||
                TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(email) || TextUtils.isEmpty(city)) {
            String toastMsg = "please fill all fields.";
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

        executorService.execute(() -> {
            boolean isUpdated = dbHelper.updateUserDetails(currentUsername, fullName, idCard, phoneNumber, email, city);

            runOnUiThread(() -> {
                if (isUpdated) {
                    String toastMsg = "saves successfully!";
                    Toast.makeText(EditProfileActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                    speakIfTtsReady(toastMsg);
                    finish();
                } else {
                    String toastMsg = "שמירת הפרטים נכשלה. נסה שוב.";
                    Toast.makeText(EditProfileActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                    speakIfTtsReady(toastMsg);
                }
            });
        });
    }

    /**
     * משמיע הודעה קולית אם TTS מוכן.
     */
    private void speakIfTtsReady(String text) {
        if (tts != null && isTtsReady) {
            if (tts.isSpeaking()) {
                tts.stop();
            }
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }
}