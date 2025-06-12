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
        // קריאה למתודת onCreate של מחלקת האב (AppCompatActivity) לביצוע אתחול בסיסי.
        super.onCreate(savedInstanceState);
        // הגדרת קובץ ה-Layout (עיצוב הממשק) עבור מסך זה.
        // R.layout.activity_registration מפנה לקובץ ה-XML שמגדיר את מבנה המסך.
        setContentView(R.layout.activity_registration);

        // אתחול מופע של DatabaseHelper לגישה למסד הנתונים.
        dbHelper = new DatabaseHelper(this);
        // אתחול שירות ה-ExecutorService לביצוע משימות ב-Thread רקע.
        executorService = Executors.newSingleThreadExecutor();

        // קישור רכיבי ממשק המשתמש (EditTexts ו-Button) מתוך קובץ ה-XML.
        // findViewById() מאתר את הרכיבים בקובץ ה-Layout לפי ה-ID שלהם.
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

        // הגדרת מאזין לחיצות (OnClickListener) עבור כפתור ההרשמה.
        // כאשר המשתמש לוחץ על כפתור זה, מתבצע תהליך ההרשמה.
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // קבלת שם המשתמש, הסיסמה ואימות הסיסמה משדות הקלט, תוך הסרת רווחים מיותרים.
                String username = usernameEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();
                String confirmPassword = confirmPasswordEditText.getText().toString().trim();

                // ולידציה בסיסית: ודא שכל שדות הקלט מלאים.
                if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
                    // הצגת הודעת שגיאה למשתמש.
                    Toast.makeText(RegistrationActivity.this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                    if (tts != null && isTtsReady) {
                        if (tts.isSpeaking()) {
                            tts.stop();
                        }
                        String detailsToSpeak = "Please fill in all fields.";
                        tts.speak(detailsToSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                    return; // יציאה מהמתודה.
                }

                // ולידציה: ודא שהסיסמאות שהוזנו תואמות.
                if (!password.equals(confirmPassword)) {
                    // הצגת הודעת שגיאה למשתמש.
                    Toast.makeText(RegistrationActivity.this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
                    if (tts != null && isTtsReady) {
                        if (tts.isSpeaking()) {
                            tts.stop();
                        }
                        String detailsToSpeak = "Passwords do not match.";
                        tts.speak(detailsToSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                    return; // יציאה מהמתודה.
                }

                // ולידציה: ודא שהסיסמה עומדת בדרישות (לפחות 6 תווים ומכילה לפחות אות אחת).
                if (password.length() < 6 || !password.matches(".*[a-zA-Z].*")) {
                    // הצגת הודעת שגיאה למשתמש.
                    Toast.makeText(RegistrationActivity.this, "Password must be at least 6 characters and contain at least one letter.", Toast.LENGTH_SHORT).show();
                    if (tts != null && isTtsReady) {
                        if (tts.isSpeaking()) {
                            tts.stop();
                        }
                        String detailsToSpeak = "Password must be at least 6 characters and contain at least one letter.";
                        tts.speak(detailsToSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                    return; // יציאה מהמתודה.
                }

                // ביצוע פעולות מסד הנתונים (בדיקת שם משתמש קיים והוספת משתמש) ב-Thread רקע.
                // זה מונע חסימת ממשק המשתמש בזמן שהאפליקציה מתקשרת עם מסד הנתונים.
                executorService.execute(() -> {
                    // בדיקה אם שם המשתמש כבר קיים במסד הנתונים.
                    if (dbHelper.checkUsername(username)) {
                        // חזרה ל-Thread הראשי (UI Thread) כדי להציג הודעת שגיאה.
                        runOnUiThread(() -> Toast.makeText(RegistrationActivity.this, "Username already exists.", Toast.LENGTH_SHORT).show());
                        return; // יציאה מהלמבדה (משימת הרקע) אם שם המשתמש קיים.
                    }

                    // קביעת תפקיד המשתמש בהתבסס על שם המשתמש שהוזן.
                    String role = "user"; // תפקיד ברירת מחדל הוא "user".
                    if (username.equals("admin")) {
                        role = "admin"; // אם שם המשתמש הוא "admin", התפקיד הוא "admin".
                    } else if (username.equals("South") || username.equals("North") || username.equals("Center") || username.equals("Jerusalem")) {
                        role = "representative"; // אם שם המשתמש הוא אחד מהמחוזות, התפקיד הוא "representative".
                    }

                    // הוספת המשתמש החדש למסד הנתונים.
                    if (dbHelper.addUser(username, password, role)) {
                        // אם ההרשמה הצליחה, חזור ל-Thread הראשי והצג הודעת הצלחה.
                        runOnUiThread(() -> {
                            Toast.makeText(RegistrationActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                            // יצירת Intent למעבר למסך UserDetailsActivity.
                            Intent intent = new Intent(RegistrationActivity.this, UserDetailsActivity.class);
                            // העברת שם המשתמש למסך UserDetailsActivity.
                            intent.putExtra("username", username);
                            // הפעלת המסך החדש.
                            startActivity(intent);
                            // סגירת RegistrationActivity לאחר הרשמה מוצלחת.
                            finish();
                        });
                    } else {
                        // אם ההרשמה נכשלה, חזור ל-Thread הראשי והצג הודעת כישלון.
                        runOnUiThread(() -> Toast.makeText(RegistrationActivity.this, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show());
                    }
                });
            }
        });
    }

    /**
     * מתודת מחזור החיים {@code onDestroy} נקראת כאשר האקטיביטי נהרס.
     * חשוב לכבות את שירות ה-ExecutorService כאן כדי לשחרר משאבים ולמנוע דליפות זיכרון.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown(); // כיבוי מסודר של ה-ExecutorService.
    }
}
