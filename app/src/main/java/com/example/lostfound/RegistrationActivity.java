package com.example.lostfound; // מגדיר את חבילת הקוד של האפליקציה.

import android.content.Intent; // ייבוא המחלקה Intent, המשמשת למעבר בין מסכים (Activities).
import android.os.Bundle; // ייבוא המחלקה Bundle, המשמשת לשמירה ושחזור מצב האקטיביטי.
import android.text.TextUtils; // ייבוא המחלקה TextUtils, המספקת שיטות עזר לבדיקת מחרוזות.
import android.view.View; // ייבוא המחלקה View, הבסיס לכל רכיבי ממשק המשתמש.
import android.widget.Button; // ייבוא המחלקה Button, המשמשת ליצירת כפתורים.
import android.widget.EditText; // ייבוא המחלקה EditText, המשמשת לשדות קלט טקסט.
import android.widget.Toast; // ייבוא המחלקה Toast, המשמשת להצגת הודעות קצרות למשתמש.

import androidx.appcompat.app.AppCompatActivity; // ייבוא מחלקת הבסיס AppCompatActivity, המספקת תאימות לאחור.

import java.util.Locale;
import java.util.concurrent.ExecutorService; // ייבוא ExecutorService, לניהול Threads ברקע.
import java.util.concurrent.Executors; // ייבוא Executors, ליצירת מופעי ExecutorService.
import android.speech.tts.TextToSpeech; // ייבוא TextToSpeech, לממשק טקסט לדיבור.


/**
 * המחלקה {@code RegistrationActivity} אחראית על מסך ההרשמה של משתמשים חדשים באפליקציה.
 * היא מאפשרת למשתמשים להזין שם משתמש, סיסמה ואימות סיסמה, מבצעת ולידציות על הקלט,
 * ומוסיפה את המשתמש החדש למסד הנתונים. קביעת תפקיד המשתמש (רגיל, אדמין, נציג) מתבצעת
 * בהתאם לשם המשתמש שהוזן. פעולות מסד הנתונים מבוצעות ב-Thread רקע.
 */
public class RegistrationActivity extends AppCompatActivity {
    private TextToSpeech tts; // TTS instance
    private boolean isTtsReady = false; // Flag for TTS initialization status

    // הצהרה על משתני ממשק המשתמש (EditText ו-Button).
    private EditText usernameEditText, passwordEditText, confirmPasswordEditText; // שדות קלט לשם משתמש, סיסמה ואימות סיסמה.
    /**
     * כפתור ההרשמה, המפעיל את תהליך יצירת החשבון.
     */
    private Button registerButton;
    /**
     * מופע של {@code DatabaseHelper} לביצוע פעולות על מסד הנתונים.
     */
    private DatabaseHelper dbHelper;
    /**
     * שירות לביצוע פעולות אסינכרוניות (ב-Thread רקע) כדי למנוע חסימת ממשק המשתמש.
     */
    private ExecutorService executorService;

    /**
     * מתודת מחזור החיים {@code onCreate} נקראת כאשר האקטיביטי נוצר לראשונה.
     * כאן מתבצעות רוב פעולות האתחול של המסך, כולל טעינת ה-Layout, קישור רכיבי UI,
     * אתחול מסד הנתונים ושירות ה-ExecutorService, והגדרת מאזין לחיצות לכפתור ההרשמה.
     *
     * @param savedInstanceState אובייקט {@code Bundle} המכיל את הנתונים שנשמרו ממצב קודם של האקטיביטי, אם קיים.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        dbHelper = new DatabaseHelper(this);
        executorService = Executors.newSingleThreadExecutor();

        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
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

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();
                String confirmPassword = confirmPasswordEditText.getText().toString().trim();

                if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
                    String toastMsg = "Please fill in all fields.";
                    Toast.makeText(RegistrationActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                    if (tts != null && isTtsReady) {
                        if (tts.isSpeaking()) {
                            tts.stop();
                        }
                        tts.speak(toastMsg, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    String toastMsg = "Passwords do not match.";
                    Toast.makeText(RegistrationActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                    if (tts != null && isTtsReady) {
                        if (tts.isSpeaking()) {
                            tts.stop();
                        }
                        tts.speak(toastMsg, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                    return;
                }

                if (password.length() < 6 || !password.matches(".*[a-zA-Z].*")) {
                    String toastMsg = "Password must be at least 6 characters and contain at least one letter.";
                    Toast.makeText(RegistrationActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                    if (tts != null && isTtsReady) {
                        if (tts.isSpeaking()) {
                            tts.stop();
                        }
                        tts.speak(toastMsg, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                    return;
                }

                executorService.execute(() -> {
                    if (dbHelper.checkUsername(username)) {
                        runOnUiThread(() -> {
                            String toastMsg = "Username already exists.";
                            Toast.makeText(RegistrationActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                            if (tts != null && isTtsReady) {
                                if (tts.isSpeaking()) {
                                    tts.stop();
                                }
                                tts.speak(toastMsg, TextToSpeech.QUEUE_FLUSH, null, null);
                            }
                        });
                        return;
                    }

                    String role = "user";
                    if (username.equals("admin")) {
                        role = "admin";
                    } else if (username.equals("South") || username.equals("North") || username.equals("Center") || username.equals("Jerusalem")) {
                        role = "representative";
                    }

                    if (dbHelper.addUser(username, password, role)) {
                        runOnUiThread(() -> {
                            String toastMsg = "Registration successful!";
                            Toast.makeText(RegistrationActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                            if (tts != null && isTtsReady) {
                                if (tts.isSpeaking()) {
                                    tts.stop();
                                }
                                tts.speak(toastMsg, TextToSpeech.QUEUE_FLUSH, null, null);
                            }
                            Intent intent = new Intent(RegistrationActivity.this, UserDetailsActivity.class);
                            intent.putExtra("username", username);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        runOnUiThread(() -> {
                            String toastMsg = "Registration failed. Please try again.";
                            Toast.makeText(RegistrationActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                            if (tts != null && isTtsReady) {
                                if (tts.isSpeaking()) {
                                    tts.stop();
                                }
                                tts.speak(toastMsg, TextToSpeech.QUEUE_FLUSH, null, null);
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}