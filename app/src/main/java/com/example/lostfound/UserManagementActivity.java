package com.example.lostfound; // מגדיר את חבילת הקוד של האפליקציה.

import android.os.Bundle; // ייבוא המחלקה Bundle, המשמשת לשמירה ושחזור מצב האקטיביטי.
import android.view.View; // ייבוא המחלקה View, הבסיס לכל רכיבי ממשק המשתמש.
import android.widget.AdapterView;
import android.widget.ArrayAdapter; // ייבוא ArrayAdapter, מתאם בסיסי לקישור נתונים ל-ListView.
import android.widget.ListView; // ייבוא ListView, המשמש להצגת רשימות פריטים.
import android.widget.TextView; // ייבוא TextView, המשמש להצגת טקסט.
import android.widget.Toast; // ייבוא Toast, המשמש להצגת הודעות קצרות למשתמש.

import androidx.appcompat.app.AppCompatActivity; // ייבוא מחלקת הבסיס AppCompatActivity, המספקת תאימות לאחור.

import java.util.ArrayList; // ייבוא ArrayList, המשמש ליצירת רשימות דינמיות.
import java.util.List; // ייבוא List, ממשק לייצוג רשימות.
import java.util.concurrent.ExecutorService; // ייבוא ExecutorService, לניהול Threads ברקע.
import java.util.concurrent.Executors; // ייבוא Executors, ליצירת מופעי ExecutorService.

/**
 * המחלקה {@code UserManagementActivity} מיועדת למנהלי מערכת (אדמינים)
 * ומאפשרת להם לצפות ברשימה של כל המשתמשים הרשומים במערכת, יחד עם תפקידיהם.
 * היא מציגה את רשימת המשתמשים ב-{@code ListView} וטוענת את הנתונים ממסד הנתונים
 * ב-Thread רקע כדי למנוע חסימת ממשק המשתמש.
 */
public class UserManagementActivity extends AppCompatActivity {

    /**
     * רכיב {@code ListView} להצגת רשימת המשתמשים.
     */
    private ListView usersListView;
    /**
     * רכיב {@code TextView} המוצג כאשר אין משתמשים רשומים להצגה.
     */
    private TextView noUsersTextView;
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
     * אתחול מסד הנתונים ושירות ה-ExecutorService, וטעינה ראשונית של רשימת המשתמשים.
     *
     * @param savedInstanceState אובייקט {@code Bundle} המכיל את הנתונים שנשמרו ממצב קודם של האקטיביטי, אם קיים.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // קריאה למתודת onCreate של מחלקת האב (AppCompatActivity) לביצוע אתחול בסיסי.
        super.onCreate(savedInstanceState);
        // הגדרת קובץ ה-Layout (עיצוב הממשק) עבור מסך זה.
        // R.layout.activity_user_management מפנה לקובץ ה-XML שמגדיר את מבנה המסך.
        setContentView(R.layout.activity_user_management);

        // אתחול מופע של DatabaseHelper לגישה למסד הנתונים.
        dbHelper = new DatabaseHelper(this);
        // אתחול שירות ה-ExecutorService לביצוע משימות ב-Thread רקע.
        executorService = Executors.newSingleThreadExecutor();

        // קישור רכיבי ממשק המשתמש (ListView ו-TextView) מתוך קובץ ה-XML.
        // findViewById() מאתר את הרכיבים בקובץ ה-Layout לפי ה-ID שלהם.
        usersListView = findViewById(R.id.usersListView);
        noUsersTextView = findViewById(R.id.noUsersTextView);

        // טעינה ראשונית של רשימת המשתמשים.
        // פעולה זו מתבצעת ב-Thread רקע.
        loadUsers();

        usersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // טיפול בבחירת משתמש (לדוגמה, פתיחת אקטיביטי חדש לעריכת פרטי משתמש)
                String selectedUsername = (String) parent.getItemAtPosition(position);
                 Toast.makeText(UserManagementActivity.this, "Selected user: " + selectedUsername, Toast.LENGTH_SHORT).show();
             }
         });
    }

    /**
     * מתודת מחזור החיים {@code onResume} נקראת כאשר האקטיביטי חוזר לפורגראונד.
     * כאן אנו מבצעים טעינה מחדש של רשימת המשתמשים כדי לוודא שהיא מעודכנת
     * בכל שינוי שבוצע (לדוגמה, אם משתמש חדש נרשם).
     */
    @Override
    protected void onResume() {
        super.onResume();
        // טעינה מחדש של המשתמשים בכל פעם שהאקטיביטי חוזר לפורגראונד.
        loadUsers();
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
     * טוענת את כל שמות המשתמשים והתפקידים שלהם ממסד הנתונים ומעדכנת את ה-ListView.
     * פעולה זו מתבצעת ב-Thread רקע באמצעות {@code ExecutorService} כדי לא לחסום את ממשק המשתמש.
     * לאחר טעינת הנתונים, ממשק המשתמש מעודכן ב-Thread הראשי.
     */
    private void loadUsers() {
        // הפעלת משימה חדשה ב-Thread רקע.
        executorService.execute(() -> {
            // יצירת רשימה שתכיל את המחרוזות שיוצגו ב-ListView.
            List<String> userList = new ArrayList<>();
            // קריאה למתודה getAllUsernamesAndRoles() מ-DatabaseHelper.
            // מתודה זו מאחזרת רשימה של מערכי מחרוזות, כאשר כל מערך מכיל שם משתמש ותפקיד.
            List<String[]> allUsersData = dbHelper.getAllUsernamesAndRoles();
            // בדיקה אם הנתונים נשלפו בהצלחה.
            if (allUsersData != null) {
                // לולאה העוברת על כל נתוני המשתמשים.
                for (String[] userData : allUsersData) {
                    // הוספת מחרוזת מעוצבת לרשימה, המכילה את שם המשתמש והתפקיד.
                    userList.add("Username: " + userData[0] + " | Role: " + userData[1]);
                }
            }

            // חזרה ל-Thread הראשי (UI Thread) כדי לעדכן את ממשק המשתמש.
            runOnUiThread(() -> {
                // בדיקה אם רשימת המשתמשים ריקה.
                if (userList.isEmpty()) {
                    // הסתרת ה-ListView והצגת הודעת "אין משתמשים".
                    usersListView.setVisibility(View.GONE);
                    noUsersTextView.setVisibility(View.VISIBLE);
                    noUsersTextView.setText("No registered users to display.");
                } else {
                    // הצגת ה-ListView והסתרת הודעת "אין משתמשים".
                    usersListView.setVisibility(View.VISIBLE);
                    noUsersTextView.setVisibility(View.GONE);

                    // יצירת מתאם (ArrayAdapter) סטנדרטי.
                    // המתאם מקשר בין רשימת המחרוזות לבין תצוגת ה-ListView,
                    // ומשתמש ב-Layout ברירת מחדל של אנדרואיד עבור כל פריט.
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            UserManagementActivity.this, // הקונטקסט של האקטיביטי.
                            android.R.layout.simple_list_item_1, // Layout ברירת מחדל של אנדרואיד לפריט רשימה פשוט.
                            userList // רשימת המחרוזות להצגה.
                    );
                    // קישור המתאם ל-ListView.
                    usersListView.setAdapter(adapter);
                }
            });
        });
    }
}
