package com.example.lostfound; // מגדיר את חבילת הקוד של האפליקציה.

import android.content.Intent; // ייבוא המחלקה Intent, המשמשת למעבר בין מסכים (Activities).
import android.os.Bundle; // ייבוא המחלקה Bundle, המשמשת לשמירה ושחזור מצב האקטיביטי.
import android.text.TextUtils; // ייבוא המחלקה TextUtils, המספקת שיטות עזר לבדיקת מחרוזות.
import android.util.Log; // Added for logging
import android.view.View; // ייבוא המחלקה View, הבסיס לכל רכיבי ממשק המשתמש.
import android.widget.Button; // ייבוא המחלקה Button, המשמשת ליצירת כפתורים.
import android.widget.EditText; // ייבוא המחלקה EditText, המשמשת לשדות קלט טקסט.
import android.widget.Toast; // ייבוא המחלקה Toast, המשמשת להצגת הודעות קצרות למשתמש.
import android.speech.tts.TextToSpeech; // ייבוא TextToSpeech, לממשק טקסט לדיבור.

import androidx.appcompat.app.AppCompatActivity; // ייבוא מחלקת הבסיס AppCompatActivity, המספקת תאימות לאחור.

import com.google.android.gms.tasks.Task; // Added for Firebase Tasks
import com.google.android.gms.tasks.Tasks; // Added for Firebase Tasks (e.g., Tasks.whenAllSuccess, though not used here directly)

import java.util.Locale;
import java.util.concurrent.ExecutorService; // ייבוא ExecutorService, לניהול Threads ברקע.
import java.util.concurrent.Executors; // ייבוא Executors, ליצירת מופעי ExecutorService.


/**
 * המחלקה {@code RegistrationActivity} אחראית על מסך ההרשמה של משתמשים חדשים באפליקציה.
 * היא מאפשרת למשתמשים להזין שם משתמש, סיסמה ואימות סיסמה, מבצעת ולידציות על הקלט,
 * ומוסיפה את המשתמש החדש למסד הנתונים (Firebase Firestore). קביעת תפקיד המשתמש (רגיל, אדמין, נציג) מתבצעת
 * בהתאם לשם המשתמש שהוזן. פעולות מסד הנתונים מבוצעות באופן אסינכרוני.
 */
public class RegistrationActivity extends AppCompatActivity {

    private static final String TAG = "RegistrationActivity"; // Added for logging

    private TextToSpeech tts; // TTS instance
    private boolean isTtsReady = false; // Flag for TTS initialization status

    // הצהרה על משתני ממשק הממשק (EditText ו-Button).
    private EditText usernameEditText, passwordEditText, confirmPasswordEditText; // שדות קלט לשם משתמש, סיסמה ואימות סיסמה.
    /**
     * כפתור ההרשמה, המפעיל את תהליך יצירת החשבון.
     */
    private Button registerButton;
    /**
     * מופע של {@code DatabaseHelper} לביצוע פעולות על מסד הנתונים (Firebase Firestore).
     */
    private DatabaseHelper dbHelper;
    /**
     * שירות לביצוע פעולות אסינכרוניות (ב-Thread רקע) כדי למנוע חסימת ממשק המשתמש.
     * אינו הכרחי לקריאות Firebase עצמן, אך נשמר אם ישנן פעולות רקע אחרות.
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

        // אתחול DatabaseHelper (כעת מבוסס Firebase) ו-ExecutorService
        dbHelper = new DatabaseHelper(this);
        executorService = Executors.newSingleThreadExecutor();

        // אתחול רכיבי ממשק הממשק
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);

        // אתחול מנוע TextToSpeech
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US); // שקול לשנות ל-Locale("iw") עבור עברית
                isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
                if (!isTtsReady) {
                    Log.e(TAG, "TTS language not supported or missing data.");
                }
            } else {
                isTtsReady = false;
                Log.e(TAG, "TTS initialization failed.");
            }
        });

        // הגדרת מאזין לחיצות לכפתור ההרשמה
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();
                String confirmPassword = confirmPasswordEditText.getText().toString().trim();

                // ולידציה בסיסית: ודא שכל השדות אינם ריקים.
                if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
                    String toastMsg = "Please fill in all fields.";
                    Toast.makeText(RegistrationActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                    speakIfTtsReady(toastMsg);
                    return;
                }

                // ולידציה: ודא שהסיסמאות תואמות.
                if (!password.equals(confirmPassword)) {
                    String toastMsg = "Passwords do not match.";
                    Toast.makeText(RegistrationActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                    speakIfTtsReady(toastMsg);
                    return;
                }

                // ולידציה: ודא שהסיסמה חזקה מספיק.
                if (password.length() < 6 || !password.matches(".*[a-zA-Z].*")) {
                    String toastMsg = "Password must be at least 6 characters and contain at least one letter.";
                    Toast.makeText(RegistrationActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                    speakIfTtsReady(toastMsg);
                    return;
                }

                // Call the asynchronous registration method
                registerUser(username, password);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // כיבוי שירות ה-Executor
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        // כיבוי TTS
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    /**
     * מבצע את תהליך הרישום של המשתמש באופן אסינכרוני מול Firebase Firestore.
     * בודק אם שם המשתמש קיים, קובע את תפקידו ומוסיף אותו למסד הנתונים.
     * @param username שם המשתמש.
     * @param password הסיסמה.
     */
    private void registerUser(String username, String password) {
        Log.d(TAG, "Attempting to register user: " + username);

        // שלב 1: בדוק אם שם המשתמש כבר קיים.
        dbHelper.checkUsername(username)
                .addOnSuccessListener(usernameExists -> {
                    if (usernameExists) {
                        runOnUiThread(() -> {
                            String toastMsg = "Username already exists.";
                            Toast.makeText(RegistrationActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                            speakIfTtsReady(toastMsg);
                            Log.d(TAG, "Registration failed: Username " + username + " already exists.");
                        });
                    } else {
                        // שלב 2: אם שם המשתמש פנוי, קבע תפקיד ונסה להוסיף את המשתמש.
                        String role;
                        if (username.equals("admin")) {
                            role = "admin";
                        } else if (username.equals("South") || username.equals("North") || username.equals("Center") || username.equals("Jerusalem")) {
                            role = "representative";
                        } else {
                            role = "user";
                        }

                        dbHelper.addUser(username, password, role)
                                .addOnSuccessListener(isAdded -> {
                                    if (isAdded) {
                                        runOnUiThread(() -> {
                                            String toastMsg = "Registration successful!";
                                            Toast.makeText(RegistrationActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                                            speakIfTtsReady(toastMsg);
                                            Log.i(TAG, "User " + username + " registered successfully with role: " + role);

                                            // מעבר למסך פרטי משתמש לאחר הרשמה מוצלחת.
                                            Intent intent = new Intent(RegistrationActivity.this, UserDetailsActivity.class);
                                            intent.putExtra("username", username);
                                            startActivity(intent);
                                            finish(); // סגור את מסך ההרשמה.
                                        });
                                    } else {
                                        runOnUiThread(() -> {
                                            String toastMsg = "Registration failed. Please try again.";
                                            Toast.makeText(RegistrationActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                                            speakIfTtsReady(toastMsg);
                                            Log.e(TAG, "Failed to add user " + username + " to Firestore.");
                                        });
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    // טיפול בשגיאה בהוספת המשתמש ל-Firestore.
                                    runOnUiThread(() -> {
                                        String toastMsg = "Registration failed: " + e.getMessage();
                                        Toast.makeText(RegistrationActivity.this, toastMsg, Toast.LENGTH_LONG).show();
                                        speakIfTtsReady(toastMsg);
                                        Log.e(TAG, "Error adding user " + username + ": " + e.getMessage(), e);
                                    });
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    // טיפול בשגיאה בבדיקת קיום שם המשתמש ב-Firestore.
                    runOnUiThread(() -> {
                        String toastMsg = "Registration failed: " + e.getMessage();
                        Toast.makeText(RegistrationActivity.this, toastMsg, Toast.LENGTH_LONG).show();
                        speakIfTtsReady(toastMsg);
                        Log.e(TAG, "Error checking username existence for " + username + ": " + e.getMessage(), e);
                    });
                });
    }

    /**
     * משמיע הודעה קולית אם TTS מוכן.
     * @param text ההודעה להשמעה.
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
}