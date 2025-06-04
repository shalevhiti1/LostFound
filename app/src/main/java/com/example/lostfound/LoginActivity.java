package com.example.lostfound; // מגדיר את חבילת הקוד של האפליקציה.

import android.Manifest; // ייבוא המחלקה Manifest, המשמשת לגישה להרשאות מערכת.
import android.app.NotificationChannel; // ייבוא NotificationChannel, המשמש ליצירת ערוצי התראות באנדרואיד 8.0 ומעלה.
import android.app.NotificationManager; // ייבוא NotificationManager, המשמש לניהול התראות.
import android.content.Intent; // ייבוא המחלקה Intent, המשמשת למעבר בין מסכים (Activities).
import android.content.pm.PackageManager; // ייבוא PackageManager, המשמש לבדיקת הרשאות.
import android.os.Build; // ייבוא Build, המשמש לבדיקת גרסת האנדרואיד הנוכחית.
import android.os.Bundle; // ייבוא המחלקה Bundle, המשמשת לשמירה ושחזור מצב האקטיביטי.
import android.view.View; // ייבוא המחלקה View, הבסיס לכל רכיבי ממשק המשתמש.
import android.widget.Button; // ייבוא המחלקה Button, המשמשת ליצירת כפתורים.
import android.widget.EditText; // ייבוא המחלקה EditText, המשמשת לשדות קלט טקסט.
import android.widget.Toast; // ייבוא המחלקה Toast, המשמשת להצגת הודעות קצרות למשתמש.

import androidx.activity.result.ActivityResultLauncher; // ייבוא ActivityResultLauncher, לטיפול בתוצאות מ-Activities אחרים (כמו בקשות הרשאה).
import androidx.activity.result.contract.ActivityResultContracts; // ייבוא ActivityResultContracts, המספק חוזים מוכנים מראש עבור ActivityResultLauncher.
import androidx.appcompat.app.AppCompatActivity; // ייבוא מחלקת הבסיס AppCompatActivity, המספקת תאימות לאחור.
import androidx.core.content.ContextCompat; // ייבוא ContextCompat, המספק שיטות עזר לבדיקת הרשאות.

import java.util.concurrent.ExecutorService; // ייבוא ExecutorService, לניהול Threads ברקע.
import java.util.concurrent.Executors; // ייבוא Executors, ליצירת מופעי ExecutorService.

// ייבוא לפונקציה הסטטית שניצור בשלב 3 (הנחה שהיא קיימת במחלקה NotificationUtils)
import static com.example.lostfound.NotificationUtils.showSimpleNotification;


/**
 * המחלקה {@code LoginActivity} מייצגת את מסך ההתחברות הראשי של האפליקציה.
 * היא מאפשרת למשתמשים קיימים להתחבר באמצעות שם משתמש וסיסמה, ולמשתמשים חדשים
 * לעבור למסך ההרשמה. בנוסף, היא מטפלת בבקשת הרשאות להתראות ויצירת ערוץ התראות.
 * פעולות מסד הנתונים מבוצעות ב-Thread רקע כדי למנוע תקיעות בממשק המשתמש.
 */
public class LoginActivity extends AppCompatActivity {

    // הצהרה על משתני ממשק המשתמש (EditText ו-Button).
    private EditText usernameEditText; // שדה קלט עבור שם המשתמש.
    private EditText passwordEditText; // שדה קלט עבור הסיסמה.
    private Button loginButton; // כפתור התחברות.
    private Button registerButton; // כפתור הרשמה.
    /**
     * מופע של {@code DatabaseHelper} לביצוע פעולות על מסד הנתונים.
     */
    private DatabaseHelper dbHelper;
    /**
     * שירות לביצוע פעולות אסינכרוניות (ב-Thread רקע) כדי למנוע חסימת ממשק המשתמש.
     */
    private ExecutorService executorService;

    /**
     * מזהה קבוע עבור ערוץ ההתראות הראשי של האפליקציה.
     */
    public static final String DEFAULT_CHANNEL_ID = "default_app_channel";

    /**
     * אובייקט {@code ActivityResultLauncher} המשמש לבקשת הרשאות מהמשתמש.
     * הוא מטפל בתוצאה של בקשת ההרשאה (האם ניתנה או נדחתה).
     */
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // קוד זה יופעל כאשר המשתמש יגיב לבקשת ההרשאה.
                if (isGranted) {
                    // אם ההרשאה ניתנה, הצג הודעת אישור.
                    Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show();
                    // ניתן להמשיך בפעולות הדורשות הרשאה.
                } else {
                    // אם ההרשאה נדחתה, הצג הודעה המודיעה על כך ועל ההשלכות האפשריות.
                    Toast.makeText(this, "Notification permission denied. Some features might not work.", Toast.LENGTH_LONG).show();
                    // יש לשקול להסביר למשתמש מדוע ההרשאה נחוצה.
                }
            });

    /**
     * מתודת מחזור החיים {@code onCreate} נקראת כאשר האקטיביטי נוצר לראשונה.
     * כאן מתבצעות רוב פעולות האתחול של המסך, כולל טעינת ה-Layout, קישור רכיבי UI,
     * אתחול מסד הנתונים ושירות ה-ExecutorService, יצירת ערוץ התראות ובקשת הרשאות.
     *
     * @param savedInstanceState אובייקט {@code Bundle} המכיל את הנתונים שנשמרו ממצב קודם של האקטיביטי, אם קיים.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // קריאה למתודת onCreate של מחלקת האב (AppCompatActivity) לביצוע אתחול בסיסי.
        super.onCreate(savedInstanceState);
        // הגדרת קובץ ה-Layout (עיצוב הממשק) עבור מסך זה.
        // R.layout.activity_login מפנה לקובץ ה-XML שמגדיר את מבנה המסך.
        setContentView(R.layout.activity_login);

        // אתחול רכיבי ממשק המשתמש (EditTexts ו-Buttons) מתוך קובץ ה-XML.
        // findViewById() מאתר את הרכיבים בקובץ ה-Layout לפי ה-ID שלהם.
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        // אתחול מופע של DatabaseHelper לגישה למסד הנתונים.
        dbHelper = new DatabaseHelper(this);
        // אתחול שירות ה-ExecutorService לביצוע משימות ב-Thread רקע.
        executorService = Executors.newSingleThreadExecutor();

        // יצירת ערוץ התראות.
        // יש לבצע זאת פעם אחת בלבד בעת הפעלת האפליקציה (או האקטיביטי הראשי).
        createNotificationChannel();
        // בקשת הרשאה לשליחת התראות.
        // בקשה זו מבוצעת רק אם האפליקציה פועלת על אנדרואיד 13 (API 33) ומעלה, וההרשאה עדיין לא ניתנה.
        requestNotificationPermission();

        // הגדרת מאזין לחיצות (OnClickListener) עבור כפתור ההתחברות.
        // כאשר המשתמש לוחץ על כפתור זה, מתבצע תהליך האימות.
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // קבלת שם המשתמש והסיסמה משדות הקלט, תוך הסרת רווחים מיותרים מההתחלה והסוף.
                String username = usernameEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                // בדיקת ולידציה בסיסית: ודא ששדות שם המשתמש והסיסמה אינם ריקים.
                if (username.isEmpty() || password.isEmpty()) {
                    // הצגת הודעת שגיאה למשתמש.
                    Toast.makeText(LoginActivity.this, "Please enter username and password.", Toast.LENGTH_SHORT).show();
                    return; // יציאה מהמתודה.
                }

                // ביצוע פעולות מסד הנתונים (בדיקת משתמש ותפקיד) ב-Thread רקע.
                // זה מונע חסימת ממשק המשתמש בזמן שהאפליקציה מתקשרת עם מסד הנתונים.
                executorService.execute(() -> {
                    // בדיקת פרטי המשתמש מול מסד הנתונים.
                    boolean isValidUser = dbHelper.checkUser(username, password);
                    // אם המשתמש והסיסמה תקינים.
                    if (isValidUser) {
                        // שליפת תפקיד המשתמש ממסד הנתונים.
                        String role = dbHelper.getUserRole(username);
                        // חזרה ל-Thread הראשי (UI Thread) כדי לעדכן את ממשק המשתמש (לדוגמה, ניווט למסך הבא).
                        runOnUiThread(() -> {
                            // בדיקה אם תפקיד המשתמש נשלף בהצלחה.
                            if (role != null) {
                                Intent intent; // הצהרה על אובייקט Intent.
                                // ניווט למסך המתאים בהתאם לתפקיד המשתמש.
                                if (role.equals("user")) {
                                    intent = new Intent(LoginActivity.this, UserActivity.class);
                                } else if (role.equals("admin")) {
                                    intent = new Intent(LoginActivity.this, AdminActivity.class);
                                }  else {
                                    // מקרה של תפקיד משתמש לא ידוע (לא אמור לקרות אם הלוגיקה נכונה).
                                    Toast.makeText(LoginActivity.this, "Unknown user role.", Toast.LENGTH_SHORT).show();
                                    return; // יציאה.
                                }
                                // העברת שם המשתמש למסך הבא.
                                intent.putExtra("username", username);
                                // הפעלת המסך החדש.
                                startActivity(intent);
                                // סגירת LoginActivity לאחר התחברות מוצלחת כדי למנוע חזרה אליו באמצעות כפתור ה-Back.
                                finish();
                            } else {
                                // מקרה חריג בו checkUser החזיר true אך getUserRole החזיר null.

                                Toast.makeText(LoginActivity.this,"Invalid username or password.", Toast.LENGTH_SHORT).show();
                                return; // יציאה מהמתודה.
                            }
                        });
                    } else {
                        // אם פרטי המשתמש אינם תקינים, חזור ל-Thread הראשי והצג הודעת שגיאה.
                        runOnUiThread(() -> {

                            Toast.makeText(LoginActivity.this, "Invalid username or password.", Toast.LENGTH_SHORT).show();
                            return; // יציאה מהמתודה.
                        });
                    }
                });
            }
        });

        // הגדרת מאזין לחיצות (OnClickListener) עבור כפתור ההרשמה.
        // כאשר המשתמש לוחץ על כפתור זה, הוא מנווט למסך ההרשמה.
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // יצירת Intent חדש למעבר למסך RegistrationActivity.
                Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                // הפעלת המסך החדש.
                startActivity(intent);
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

    /**
     * יוצרת ערוץ התראות עבור האפליקציה.
     * ערוצי התראות נדרשים באנדרואיד 8.0 (Oreo, API 26) ומעלה כדי שניתן יהיה להציג התראות.
     */
    private void createNotificationChannel() {
        // בדיקה אם גרסת האנדרואיד הנוכחית היא Oreo (API 26) ומעלה.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // קבלת שם ותיאור לערוץ ההתראות מקובץ המשאבים (strings.xml).
            CharSequence name = getString(R.string.default_channel_name);
            String description = getString(R.string.default_channel_description);
            // הגדרת חשיבות הערוץ (קובע את רמת ההפרעה של ההתראות).
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            // יצירת אובייקט NotificationChannel עם ה-ID, השם והחשיבות.
            NotificationChannel channel = new NotificationChannel(DEFAULT_CHANNEL_ID, name, importance);
            // הגדרת תיאור לערוץ.
            channel.setDescription(description);

            // קבלת שירות NotificationManager מהמערכת.
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            // אם השירות זמין, צור את ערוץ ההתראות.
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * מבקשת מהמשתמש הרשאה לשלוח התראות.
     * הרשאה זו נדרשת באנדרואיד 13 (API 33) ומעלה.
     * הבקשה מבוצעת באמצעות {@code ActivityResultLauncher} שהוגדר מראש.
     */
    private void requestNotificationPermission() {
        // בדיקה אם גרסת האנדרואיד הנוכחית היא Android 13 (API 33) ומעלה.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // בדיקה אם הרשאת POST_NOTIFICATIONS כבר ניתנה לאפליקציה.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                // אם ההרשאה לא ניתנה, בקש אותה מהמשתמש באמצעות ה-Launcher.
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}
