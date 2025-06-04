package com.example.lostfound; // מגדיר את חבילת הקוד של האפליקציה.

import android.os.Bundle; // ייבוא המחלקה Bundle, המשמשת לשמירה ושחזור מצב האקטיביטי.
import android.text.TextUtils; // ייבוא המחלקה TextUtils, המספקת שיטות עזר לבדיקת מחרוזות.
import android.view.View; // ייבוא המחלקה View, הבסיס לכל רכיבי ממשק המשתמש.
import android.widget.Button; // ייבוא המחלקה Button, המשמשת ליצירת כפתורים.
import android.widget.EditText; // ייבוא המחלקה EditText, המשמשת לשדות קלט טקסט.
import android.widget.Toast; // ייבוא המחלקה Toast, המשמשת להצגת הודעות קצרות למשתמש.

import androidx.appcompat.app.AppCompatActivity; // ייבוא מחלקת הבסיס AppCompatActivity, המספקת תאימות לאחור.

import java.util.concurrent.ExecutorService; // ייבוא ExecutorService, לניהול Threads ברקע.
import java.util.concurrent.Executors; // ייבוא Executors, ליצירת מופעי ExecutorService.

/**
 * המחלקה {@code EditProfileActivity} מאפשרת למשתמשים לערוך את פרטיהם האישיים.
 * משתמשים יכולים לעדכן את שמם המלא, מספר הטלפון, כתובת האימייל ועיר המגורים.
 * טעינת פרטי המשתמש ושמירת השינויים מתבצעות ב-Thread רקע כדי למנוע תקיעות בממשק המשתמש.
 */
public class EditProfileActivity extends AppCompatActivity {

    // הצהרה על משתני ממשק המשתמש (EditText ו-Button).
    private EditText editUsername, editFullName, editIdCard, editPhoneNumber, editEmail, editCity;
    /**
     * כפתור לשמירת השינויים שבוצעו בפרופיל המשתמש.
     */
    private Button saveProfileButton;
    /**
     * מופע של {@code DatabaseHelper} לביצוע פעולות על מסד הנתונים.
     */
    private DatabaseHelper dbHelper;
    /**
     * שם המשתמש של המשתמש המחובר כעת, שאת פרטיו עורכים.
     */
    private String currentUsername;
    /**
     * שירות לביצוע פעולות אסינכרוניות (ב-Thread רקע) כדי למנוע חסימת ממשק המשתמש.
     */
    private ExecutorService executorService;

    /**
     * מתודת מחזור החיים {@code onCreate} נקראת כאשר האקטיביטי נוצר לראשונה.
     * כאן מתבצעות רוב פעולות האתחול של המסך, כולל טעינת ה-Layout, קישור רכיבי UI,
     * קבלת שם המשתמש מה-Intent, אתחול מסד הנתונים ושירות ה-ExecutorService,
     * וטעינת פרטי המשתמש הנוכחיים.
     *
     * @param savedInstanceState אובייקט {@code Bundle} המכיל את הנתונים שנשמרו ממצב קודם של האקטיביטי, אם קיים.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // קריאה למתודת onCreate של מחלקת האב (AppCompatActivity) לביצוע אתחול בסיסי.
        super.onCreate(savedInstanceState);
        // הגדרת קובץ ה-Layout (עיצוב הממשק) עבור מסך זה.
        // R.layout.activity_edit_profile מפנה לקובץ ה-XML שמגדיר את מבנה המסך.
        setContentView(R.layout.activity_edit_profile);

        // אתחול מופע של DatabaseHelper לגישה למסד הנתונים.
        dbHelper = new DatabaseHelper(this);
        // אתחול שירות ה-ExecutorService לביצוע משימות ב-Thread רקע.
        executorService = Executors.newSingleThreadExecutor();

        // קבלת שם המשתמש שהועבר לאקטיביטי זה באמצעות Intent (לדוגמה, מ-UserActivity).
        currentUsername = getIntent().getStringExtra("username");
        // בדיקה אם שם המשתמש התקבל בהצלחה.
        if (currentUsername == null || currentUsername.isEmpty()) {
            // הצגת הודעת שגיאה למשתמש אם שם המשתמש חסר.
            Toast.makeText(this, "שגיאה: שם משתמש לא נמצא.", Toast.LENGTH_SHORT).show();
            // סגירת האקטיביטי וחזרה למסך הקודם.
            finish();
            return; // יציאה ממתודת onCreate.
        }

        // אתחול רכיבי ממשק המשתמש (EditTexts ו-Button) מתוך קובץ ה-XML.
        // findViewById() מאתר את הרכיבים בקובץ ה-Layout לפי ה-ID שלהם.
        editUsername = findViewById(R.id.editUsername);
        editFullName = findViewById(R.id.editFullName);
        editIdCard = findViewById(R.id.editIdCard);
        editPhoneNumber = findViewById(R.id.editPhoneNumber);
        editEmail = findViewById(R.id.editEmail);
        editCity = findViewById(R.id.editCity);
        saveProfileButton = findViewById(R.id.saveProfileButton);

        // טעינת פרטי המשתמש הנוכחיים והצגתם בשדות העריכה.
        // פעולה זו מתבצעת ב-Thread רקע.
        loadUserDetails();

        // הגדרת מאזין לחיצות (OnClickListener) עבור כפתור שמירת הפרופיל.
        // כאשר הכפתור נלחץ, מתודת saveUserDetails() תיקרא.
        saveProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserDetails();
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
        // כיבוי מסודר של ה-ExecutorService.
        executorService.shutdown();
    }

    /**
     * טוענת את פרטי המשתמש הנוכחיים ממסד הנתונים ומציגה אותם בשדות העריכה.
     * פעולה זו מתבצעת ב-Thread רקע באמצעות {@code ExecutorService}.
     * לאחר טעינת הנתונים, ממשק המשתמש מעודכן ב-Thread הראשי.
     */
    private void loadUserDetails() {
        // הפעלת משימה חדשה ב-Thread רקע.
        executorService.execute(() -> {
            // קבלת הפרטים ממסד הנתונים באמצעות שם המשתמש הנוכחי.
            // מתודות אלו ב-DatabaseHelper מבצעות קריאה למסד הנתונים.
            final String fullName = dbHelper.getUserFullName(currentUsername);
            final String idCard = dbHelper.getUserIdCard(currentUsername);
            final String phoneNumber = dbHelper.getUserPhoneNumber(currentUsername);
            final String email = dbHelper.getUserEmail(currentUsername);
            final String city = dbHelper.getUserCity(currentUsername);

            // חזרה ל-Thread הראשי (UI Thread) כדי לעדכן את ממשק המשתמש.
            runOnUiThread(() -> {
                // הצגת שם המשתמש בשדה המתאים (שם המשתמש מגיע מה-Intent ולא משתנה).
                editUsername.setText(currentUsername);
                // הצגת הפרטים שנשלפו בשדות העריכה המתאימים.
                editFullName.setText(fullName);
                editIdCard.setText(idCard);
                editPhoneNumber.setText(phoneNumber);
                editEmail.setText(email);
                editCity.setText(city);
            });
        });
    }

    /**
     * שומרת את השינויים שבוצעו על ידי המשתמש בפרטיו האישיים במסד הנתונים.
     * המתודה מבצעת ולידציה בסיסית על הקלט ושומרת את הנתונים ב-Thread רקע.
     */
    private void saveUserDetails() {
        // קבלת הערכים הנוכחיים משדות העריכה, תוך הסרת רווחים מיותרים.
        String fullName = editFullName.getText().toString().trim();
        String idCard = editIdCard.getText().toString().trim();
        String phoneNumber = editPhoneNumber.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String city = editCity.getText().toString().trim();

        // ולידציה בסיסית: ודא שכל שדות החובה אינם ריקים.
        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(idCard) ||
                TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(email) || TextUtils.isEmpty(city)) {
            // הצגת הודעת שגיאה אם יש שדות ריקים.
            Toast.makeText(this, "אנא מלא את כל השדות.", Toast.LENGTH_SHORT).show();
            return; // יציאה מהמתודה.
        }

        // ולידציה עבור פורמט האימייל: ודא שכתובת האימייל חוקית.
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            // הצגת הודעת שגיאה אם פורמט האימייל אינו חוקי.
            Toast.makeText(this, "אנא הזן כתובת אימייל חוקית.", Toast.LENGTH_SHORT).show();
            return; // יציאה מהמתודה.
        }

        // ביצוע פעולת עדכון במסד הנתונים ב-Thread רקע.
        executorService.execute(() -> {
            // שמירת השינויים למסד הנתונים באמצעות מתודת updateUserDetails ב-DatabaseHelper.
            boolean isUpdated = dbHelper.updateUserDetails(currentUsername, fullName, idCard, phoneNumber, email, city);

            // חזרה ל-Thread הראשי (UI Thread) כדי להציג את תוצאות השמירה.
            runOnUiThread(() -> {
                // בדיקה האם העדכון הצליח.
                if (isUpdated) {
                    // הצגת הודעת הצלחה.
                    Toast.makeText(EditProfileActivity.this, "פרטי המשתמש נשמרו בהצלחה!", Toast.LENGTH_SHORT).show();
                    // סגירת האקטיביטי וחזרה למסך הקודם.
                    finish();
                } else {
                    // הצגת הודעת כישלון.
                    Toast.makeText(EditProfileActivity.this, "שמירת הפרטים נכשלה. נסה שוב.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
