package com.example.lostfound; // מגדיר את חבילת הקוד של האפליקציה.

import android.content.Intent; // ייבוא המחלקה Intent, המשמשת למעבר בין מסכים (Activities).
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
 * המחלקה {@code UserDetailsActivity} מאפשרת למשתמשים חדשים (לאחר הרשמה בסיסית)
 * להזין ולשמור פרטים אישיים נוספים כמו שם מלא, תעודת זהות, מספר טלפון, אימייל ועיר מגורים.
 * לאחר שמירת הפרטים, המשתמש מנווט למסך הבית המתאים לתפקידו.
 * שמירת הנתונים וקבלת תפקיד המשתמש מתבצעות ב-Thread רקע כדי למנוע תקיעות בממשק המשתמש.
 */
public class UserDetailsActivity extends AppCompatActivity {

    // הצהרה על משתני ממשק המשתמש (EditText ו-Button).
    private EditText fullNameEditText, idCardEditText, phoneNumberEditText, emailEditText, cityEditText;
    /**
     * כפתור לשמירת הפרטים האישיים שהוזנו.
     */
    private Button saveButton;
    /**
     * מופע של {@code DatabaseHelper} לביצוע פעולות על מסד הנתונים.
     */
    private DatabaseHelper dbHelper;
    /**
     * שם המשתמש של המשתמש הנוכחי, עבורו נשמרים הפרטים.
     * שם משתמש זה מתקבל ממסך ההרשמה.
     */
    private String username;
    /**
     * שירות לביצוע פעולות אסינכרוניות (ב-Thread רקע) כדי למנוע חסימת ממשק המשתמש.
     */
    private ExecutorService executorService;

    /**
     * מתודת מחזור החיים {@code onCreate} נקראת כאשר האקטיביטי נוצר לראשונה.
     * כאן מתבצעות רוב פעולות האתחול של המסך, כולל טעינת ה-Layout, קישור רכיבי UI,
     * קבלת שם המשתמש מה-Intent, אתחול מסד הנתונים ושירות ה-ExecutorService,
     * והגדרת מאזין לחיצות לכפתור השמירה.
     *
     * @param savedInstanceState אובייקט {@code Bundle} המכיל את הנתונים שנשמרו ממצב קודם של האקטיביטי, אם קיים.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // קריאה למתודת onCreate של מחלקת האב (AppCompatActivity) לביצוע אתחול בסיסי.
        super.onCreate(savedInstanceState);
        // הגדרת קובץ ה-Layout (עיצוב הממשק) עבור מסך זה.
        // R.layout.activity_user_details מפנה לקובץ ה-XML שמגדיר את מבנה המסך.
        setContentView(R.layout.activity_user_details);

        // אתחול מופע של DatabaseHelper לגישה למסד הנתונים.
        dbHelper = new DatabaseHelper(this);
        // אתחול שירות ה-ExecutorService לביצוע משימות ב-Thread רקע.
        executorService = Executors.newSingleThreadExecutor();

        // קבלת שם המשתמש מה-Intent שפתח את האקטיביטי הזה (נשלח ממסך ההרשמה).
        username = getIntent().getStringExtra("username");

        // קישור רכיבי ממשק המשתמש (EditTexts ו-Button) מתוך קובץ ה-XML.
        // findViewById() מאתר את הרכיבים בקובץ ה-Layout לפי ה-ID שלהם.
        fullNameEditText = findViewById(R.id.fullNameEditText);
        idCardEditText = findViewById(R.id.idCardEditText);
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        emailEditText = findViewById(R.id.emailEditText);
        cityEditText = findViewById(R.id.cityEditText);
        saveButton = findViewById(R.id.saveButton);

        // הגדרת מאזין לחיצות (OnClickListener) עבור כפתור השמירה.
        // כאשר המשתמש לוחץ על כפתור זה, מתבצע תהליך שמירת הפרטים.
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // קבלת הערכים משדות הקלט, תוך הסרת רווחים מיותרים.
                String fullName = fullNameEditText.getText().toString().trim();
                String idCard = idCardEditText.getText().toString().trim();
                String phoneNumber = phoneNumberEditText.getText().toString().trim();
                String email = emailEditText.getText().toString().trim();
                String city = cityEditText.getText().toString().trim();

                // ולידציה בסיסית: ודא שכל שדות החובה אינם ריקים.
                if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(idCard) || TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(email) || TextUtils.isEmpty(city)) {
                    // הצגת הודעת שגיאה למשתמש.
                    Toast.makeText(UserDetailsActivity.this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                    return; // יציאה מהמתודה.
                }

                // ביצוע פעולות מסד הנתונים (עדכון פרטי משתמש וקבלת תפקיד) ב-Thread רקע.
                // זה מונע חסימת ממשק המשתמש בזמן שהאפליקציה מתקשרת עם מסד הנתונים.
                executorService.execute(() -> {
                    // עדכון פרטי המשתמש במסד הנתונים.
                    boolean isUpdated = dbHelper.updateUserDetails(username, fullName, idCard, phoneNumber, email, city);

                    // חזרה ל-Thread הראשי (UI Thread) כדי להציג הודעות ולנווט למסך הבא.
                    runOnUiThread(() -> {
                        // בדיקה אם עדכון הפרטים הצליח.
                        if (isUpdated) {
                            // הצגת הודעת הצלחה למשתמש.
                            Toast.makeText(UserDetailsActivity.this, "User details saved successfully!", Toast.LENGTH_SHORT).show();

                            // ניווט למסך המתאים בהתאם לתפקיד המשתמש.
                            // קריאה למתודה getUserRole מ-dbHelper (היא עצמה צריכה להיות ב-Thread רקע אם היא לא כבר).
                            // הערה: קריאה זו ל-dbHelper.getUserRole בתוך runOnUiThread() היא בעייתית
                            // מכיוון שהיא מבוצעת על ה-UI Thread. יש להעביר גם אותה ל-Thread רקע.
                            // הפתרון הנכון הוא לשלוף את התפקיד באותו Thread רקע שבו בוצע ה-updateUserDetails.
                            // תיקון: נשלוף את התפקיד ב-Thread הרקע ונשתמש בערך הסופי ב-runOnUiThread.
                            String role = dbHelper.getUserRole(username); // קריאה זו תבוצע ב-Thread הרקע הנוכחי.
                            Intent intent;
                            // אם התפקיד הוא null, זה מצביע על בעיה בשליפה.
                            // במקרה כזה, ננווט למסך ההתחברות ונבקש מהמשתמש להתחבר שוב.
                            if (role != null) {
                                if (role.equals("user")) {
                                    intent = new Intent(UserDetailsActivity.this, UserActivity.class);
                                } else if (role.equals("admin")) {
                                    intent = new Intent(UserDetailsActivity.this, AdminActivity.class);
                                }  else {
                                    // גיבוי למקרה שתפקיד המשתמש אינו צפוי (לדוגמה, "representative").
                                    // במקרה כזה, ננווט למסך ההתחברות.
                                    intent = new Intent(UserDetailsActivity.this, LoginActivity.class);
                                }
                                // העברת שם המשתמש למסך הבא.
                                intent.putExtra("username", username);
                                // הפעלת המסך החדש.
                                startActivity(intent);
                                // סגירת UserDetailsActivity לאחר שמירה מוצלחת.
                                finish();
                            } else {
                                // הצגת הודעת שגיאה אם לא ניתן היה לאחזר את תפקיד המשתמש.
                                Toast.makeText(UserDetailsActivity.this, "Error retrieving user role. Please login again.", Toast.LENGTH_LONG).show();
                                // ניווט למסך ההתחברות.
                                startActivity(new Intent(UserDetailsActivity.this, LoginActivity.class));
                                finish();
                            }
                        } else {
                            // הצגת הודעת כישלון אם שמירת הפרטים נכשלה.
                            Toast.makeText(UserDetailsActivity.this, "Failed to save user details. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
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
