package com.example.lostfound; // מגדיר את חבילת הקוד של האפליקציה.

import android.content.Intent; // ייבוא מחלקת Intent, המשמשת למעבר בין מסכים (Activities).
import android.os.Bundle; // ייבוא המחלקה Bundle, המשמשת לשמירה ושחזור מצב האקטיביטי.
import android.view.View; // ייבוא המחלקה View, הבסיס לכל רכיבי ממשק המשתמש.
import android.widget.Button; // ייבוא המחלקה Button, המשמשת ליצירת כפתורים.

import androidx.appcompat.app.AppCompatActivity; // ייבוא מחלקת הבסיס AppCompatActivity, המספקת תאימות לאחור לתכונות אנדרואיד.

/**
 * המחלקה {@code AdminActivity} מייצגת את מסך הבית הראשי עבור משתמשים בעלי הרשאות אדמין.
 * מסך זה מאפשר לאדמין לנווט לפונקציות ניהוליות שונות, כגון צפייה בכל הפניות וניהול משתמשים.
 */
public class AdminActivity extends AppCompatActivity {

    /**
     * כפתור ממשק המשתמש המאפשר לאדמין לצפות ברשימה של כל הפניות במערכת.
     */
    private Button viewAllCasesButton;
    /**
     * כפתור ממשק המשתמש המאפשר לאדמין לנווט למסך ניהול המשתמשים.
     */
    private Button manageUsersButton;
    /**
     * משתנה לשמירת שם המשתמש של האדמין המחובר.
     * שם משתמש זה מועבר ממסך ההתחברות ומשמש להעברה למסכים הבאים.
     */
    private String username;

    /**
     * מתודת מחזור החיים {@code onCreate} נקראת כאשר האקטיביטי נוצר לראשונה.
     * כאן מתבצעות רוב פעולות האתחול של המסך, כולל טעינת ה-Layout, קישור רכיבי UI,
     * ושליפת נתונים מה-Intent.
     *
     * @param savedInstanceState אובייקט {@code Bundle} המכיל את הנתונים שנשמרו ממצב קודם של האקטיביטי, אם קיים.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // קריאה למתודת onCreate של מחלקת האב (AppCompatActivity) לביצוע אתחול בסיסי.
        super.onCreate(savedInstanceState);
        // הגדרת קובץ ה-Layout (עיצוב הממשק) עבור מסך זה.
        // R.layout.activity_admin מפנה לקובץ ה-XML שמגדיר את מבנה המסך.
        setContentView(R.layout.activity_admin);

        // שליפת שם המשתמש.
        // שם המשתמש של האדמין המחובר מועבר לאקטיביטי זה באמצעות Intent,
        // בדרך כלל ממסך ההתחברות (LoginActivity).
        username = getIntent().getStringExtra("username");

        // קישור רכיבי ממשק המשתמש.
        // מאתר את הכפתורים בקובץ ה-Layout (activity_admin.xml) ומקשר אותם למשתני ה-Button המתאימים במחלקה.
        viewAllCasesButton = findViewById(R.id.viewAllCasesButton);
        manageUsersButton = findViewById(R.id.manageUsersButton);

        // הגדרת מאזין לחיצות לכפתור "צפייה בכל הפניות".
        // כאשר המשתמש לוחץ על כפתור זה, האפליקציה תנווט למסך המציג את כל הפניות במערכת.
        viewAllCasesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // יצירת אובייקט Intent חדש.
                // Intent משמש להפעלת רכיבי אפליקציה אחרים, במקרה זה, האקטיביטי AllCasesActivity.
                Intent intent = new Intent(AdminActivity.this, AllCasesActivity.class);
                // העברת שם המשתמש של האדמין למסך AllCasesActivity.
                // זה מאפשר למסך הבא לדעת מי האדמין שצופה בפניות.
                intent.putExtra("username", username);
                // הפעלת האקטיביטי החדש שהוגדר ב-Intent.
                startActivity(intent);
            }
        });

        // הגדרת מאזין לחיצות לכפתור "ניהול משתמשים".
        // בדיקה אם הכפתור manageUsersButton אינו null לפני הגדרת מאזין לחיצות,
        // כדי למנוע NullPointerException אם הכפתור לא קיים ב-Layout מסיבה כלשהי.
        if (manageUsersButton != null) {
            // כאשר המשתמש לוחץ על כפתור זה, האפליקציה תנווט למסך ניהול המשתמשים.
            manageUsersButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // יצירת אובייקט Intent חדש למעבר למסך UserManagementActivity.
                    Intent intent = new Intent(AdminActivity.this, UserManagementActivity.class);
                    // העברת שם המשתמש גם למסך ניהול המשתמשים, אם יש צורך בכך (לדוגמה, להצגת פרטי האדמין).
                    intent.putExtra("username", username);
                    // הפעלת האקטיביטי החדש.
                    startActivity(intent);
                }
            });
        }
    }
}
