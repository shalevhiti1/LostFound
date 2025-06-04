package com.example.lostfound; // מגדיר את חבילת הקוד של האפליקציה.

import android.content.Intent; // ייבוא המחלקה Intent, המשמשת למעבר בין מסכים (Activities).
import android.os.Bundle; // ייבוא המחלקה Bundle, המשמשת לשמירה ושחזור מצב האקטיביטי.
import android.view.View; // ייבוא המחלקה View, הבסיס לכל רכיבי ממשק המשתמש.
import android.widget.Button; // ייבוא המחלקה Button, המשמשת ליצירת כפתורים.
// import android.widget.TextView; // ייבוא המחלקה TextView, המשמשת להצגת טקסט. (הוסר)

import androidx.appcompat.app.AppCompatActivity; // ייבוא מחלקת הבסיס AppCompatActivity, המספקת תאימות לאחור.

/**
 * המחלקה {@code UserActivity} מייצגת את מסך הבית הראשי עבור משתמשים רגילים.
 * לאחר התחברות מוצלחת, משתמשים מועברים למסך זה, שממנו הם יכולים לבצע
 * פעולות שונות הקשורות לדיווח ולמעקב אחר אבידות, וכן לערוך את הפרופיל האישי שלהם.
 */
public class UserActivity extends AppCompatActivity {
    // הצהרה על משתני ממשק המשתמש (Button).
    private Button openNewCaseButton; // כפתור לפתיחת דיווח חדש.
    private Button viewMyCasesButton; // כפתור לצפייה בדיווחים קיימים של המשתמש.
    /**
     * כפתור לעריכת הפרופיל האישי של המשתמש.
     */
    private Button editProfileButton;
    // private TextView welcomeTextView; // מניחים שיש הודעת ברוך הבא ב-Layout. (הוסר)
    /**
     * שם המשתמש של המשתמש המחובר כעת.
     */
    private String username;

    /**
     * מתודת מחזור החיים {@code onCreate} נקראת כאשר האקטיביטי נוצר לראשונה.
     * כאן מתבצעות רוב פעולות האתחול של המסך, כולל טעינת ה-Layout, קישור רכיבי UI,
     * קבלת שם המשתמש מה-Intent, והגדרת מאזינים לכפתורים.
     *
     * @param savedInstanceState אובייקט {@code Bundle} המכיל את הנתונים שנשמרו ממצב קודם של האקטיביטי, אם קיים.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // קריאה למתודת onCreate של מחלקת האב (AppCompatActivity) לביצוע אתחול בסיסי.
        super.onCreate(savedInstanceState);
        // הגדרת קובץ ה-Layout (עיצוב הממשק) עבור מסך זה.
        // R.layout.activity_user מפנה לקובץ ה-XML שמגדיר את מבנה המסך.
        setContentView(R.layout.activity_user);

        // קבלת שם המשתמש שהועבר לאקטיביטי זה באמצעות Intent (לדוגמה, מ-LoginActivity).
        username = getIntent().getStringExtra("username");

        // אתחול רכיבי ממשק המשתמש הקיימים (כפתורים).
        // findViewById() מאתר את הרכיבים בקובץ ה-Layout לפי ה-ID שלהם.
        openNewCaseButton = findViewById(R.id.openNewCaseButton);
        viewMyCasesButton = findViewById(R.id.viewMyCasesButton);

        // אתחול כפתור עריכת הפרופיל החדש.
        // יש לוודא שה-ID 'editProfileButton' קיים בקובץ activity_user.xml.
        editProfileButton = findViewById(R.id.editProfileButton);

        // בדיקה אם רכיב welcomeTextView קיים וכן אם שם המשתמש אינו null.
        // אם כן, עדכן את הטקסט שלו עם הודעת ברוך הבא אישית.
        // welcomeTextView = findViewById(R.id.welcomeTextView); // יש לוודא ש-ID זה קיים ב-XML (הוסר)
        // if (welcomeTextView != null && username != null) { // (הוסר)
        //     welcomeTextView.setText("ברוך הבא, " + username + "!"); // (הוסר)
        // } // (הוסר)

        // הגדרת מאזין לחיצות (OnClickListener) עבור כפתור "פתיחת דיווח חדש".
        // כאשר המשתמש לוחץ על כפתור זה, הוא מנווט למסך פתיחת פנייה חדשה.
        openNewCaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // יצירת Intent חדש למעבר למסך NewCaseActivity.
                Intent intent = new Intent(UserActivity.this, NewCaseActivity.class);
                // העברת שם המשתמש למסך NewCaseActivity.
                intent.putExtra("username", username);
                // הפעלת המסך החדש.
                startActivity(intent);
            }
        });

        // הגדרת מאזין לחיצות (OnClickListener) עבור כפתור "הצגת הדיווחים שלי".
        // כאשר המשתמש לוחץ על כפתור זה, הוא מנווט למסך המציג את כל הפניות שפתח.
        viewMyCasesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // יצירת Intent חדש למעבר למסך MyCasesActivity.
                Intent intent = new Intent(UserActivity.this, MyCasesActivity.class);
                // העברת שם המשתמש למסך MyCasesActivity.
                intent.putExtra("username", username);
                // הפעלת המסך החדש.
                startActivity(intent);
            }
        });

        // הגדרת מאזין לחיצות (OnClickListener) עבור כפתור "עריכת פרופיל".
        // בדיקה אם הכפתור editProfileButton אינו null לפני הגדרת מאזין לחיצות,
        // כדי למנוע NullPointerException אם הכפתור לא קיים ב-Layout מסיבה כלשהי.
        if (editProfileButton != null) {
            // כאשר המשתמש לוחץ על כפתור זה, הוא מנווט למסך עריכת הפרופיל.
            editProfileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // יצירת Intent חדש למעבר למסך EditProfileActivity.
                    Intent intent = new Intent(UserActivity.this, EditProfileActivity.class);
                    // העברת שם המשתמש הנוכחי לפעילות העריכה,
                    // כך שמסך העריכה יוכל לטעון ולשמור את הפרטים הנכונים.
                    intent.putExtra("username", username);
                    // הפעלת המסך החדש.
                    startActivity(intent);
                }
            });
        }
    }
}
