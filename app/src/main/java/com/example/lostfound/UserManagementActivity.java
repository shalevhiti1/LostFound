package com.example.lostfound; // מגדיר את חבילת הקוד של האפליקציה.

import android.os.Bundle; // ייבוא המחלקה Bundle, המשמשת לשמירה ושחזור מצב האקטיביטי.
import android.util.Log; // Added for logging
import android.view.View; // ייבוא המחלקה View, הבסיס לכל רכיבי ממשק המשתמש.
import android.widget.AdapterView;
import android.widget.ArrayAdapter; // ייבוא ArrayAdapter, מתאם בסיסי לקישור נתונים ל-ListView.
import android.widget.ListView; // ייבוא ListView, המשמש להצגת רשימות פריטים.
import android.widget.TextView; // ייבוא TextView, המשמש להצגת טקסט.
import android.widget.Toast; // ייבוא Toast, המשמש להצגת הודעות קצרות למשתמש.

import androidx.appcompat.app.AppCompatActivity; // ייבוא מחלקת הבסיס AppCompatActivity, המספקת תאימות לאחור.

import com.google.android.gms.tasks.Task; // Added for Firebase Tasks

import java.util.ArrayList; // ייבוא ArrayList, המשמש ליצירת רשימות דינמיות.
import java.util.List; // ייבוא List, ממשק לייצוג רשימות.
import java.util.concurrent.ExecutorService; // ייבוא ExecutorService, לניהול Threads ברקע.
import java.util.concurrent.Executors; // ייבוא Executors, ליצירת מופעי ExecutorService.

/**
 * המחלקה {@code UserManagementActivity} מיועדת למנהלי מערכת (אדמינים)
 * ומאפשרת להם לצפות ברשימה של כל המשתמשים הרשומים במערכת, יחד עם תפקידיהם.
 * היא מציגה את רשימת המשתמשים ב-{@code ListView} וטוענת את הנתונים ממסד הנתונים
 * באופן אסינכרוני.
 * קודכן לעבודה עם Firebase Firestore.
 */
public class UserManagementActivity extends AppCompatActivity {

    private static final String TAG = "UserManagementActivity"; // Added TAG for logging

    /**
     * רכיב {@code ListView} להצגת רשימת המשתמשים.
     */
    private ListView usersListView;
    /**
     * רכיב {@code TextView} המוצג כאשר אין משתמשים רשומים להצגה.
     */
    private TextView noUsersTextView;
    /**
     * מופע של {@code DatabaseHelper} לביצוע פעולות על מסד הנתונים (Firebase Firestore).
     */
    private DatabaseHelper dbHelper;
    /**
     * שירות לביצוע פעולות אסינכרוניות (ב-Thread רקע) כדי למנוע חסימת ממשק המשתמש.
     * אינו הכרחי לקריאות Firebase עצמן, אך נשמר אם ישנן פעולות רקע אחרות.
     */
    private ExecutorService executorService; // Kept for general background tasks if needed

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

        // אתחול מופע של DatabaseHelper לגישה למסד הנתונים (Firebase Firestore).
        dbHelper = new DatabaseHelper(this);
        // אתחול שירות ה-ExecutorService לביצוע משימות ב-Thread רקע.
        executorService = Executors.newSingleThreadExecutor(); // Kept for general background tasks if needed

        // קישור רכיבי ממשק המשתמש (ListView ו-TextView) מתוך קובץ ה-XML.
        // findViewById() מאתר את הרכיבים בקובץ ה-Layout לפי ה-ID שלהם.
        usersListView = findViewById(R.id.usersListView);
        noUsersTextView = findViewById(R.id.noUsersTextView);

        // טעינה ראשונית של רשימת המשתמשים.
        loadUsers();

        // הגדרת מאזין לחיצות על פריטים ב-ListView.
        usersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // טיפול בבחירת משתמש: הצגת הודעת Toast עם שם המשתמש שנבחר.
                // הפריט ב-ListView הוא מחרוזת, לדוגמה "Username: user1 | Role: user".
                // נוכל לחלץ את שם המשתמש מהמחרוזת אם נרצה לבצע פעולה נוספת ספציפית למשתמש.
                String selectedDisplayString = (String) parent.getItemAtPosition(position);
                Log.d(TAG, "Selected list item: " + selectedDisplayString);
                Toast.makeText(UserManagementActivity.this, "Selected user: " + selectedDisplayString, Toast.LENGTH_SHORT).show();

                // אם תרצה לעבור לאקטיביטי עריכה של משתמש, תצטרך לחלץ את שם המשתמש מהמחרוזת.
                // לדוגמה:
                // String username = selectedDisplayString.substring(selectedDisplayString.indexOf(":") + 2, selectedDisplayString.indexOf(" |"));
                // Intent intent = new Intent(UserManagementActivity.this, EditUserProfileActivity.class); // פעילות עריכה למשתמשים
                // intent.putExtra("username", username);
                // startActivity(intent);
            }
        });
    }

    /**
     * מתודת מחזור החיים {@code onResume} נקראת כאשר האקטיביטי חוזר לפורגראונד.
     * כאן אנו מבצעים טעינה מחדש של רשימת המשתמשים כדי לוודא שהיא מעודכנת
     * בכל שינוי שבוצע (לדוגמה, אם משתמש חדש נרשם או שונה תפקידו).
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
        // כיבוי מסודר של ה-ExecutorService.
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    /**
     * טוענת את כל שמות המשתמשים והתפקידים שלהם מ-Firebase Firestore ומעדכנת את ה-ListView.
     * פעולה זו מתבצעת באופן אסינכרוני.
     */
    private void loadUsers() {
        Log.d(TAG, "Loading all users and roles from Firestore.");

        // קריאה למתודה getAllUsernamesAndRoles() מ-DatabaseHelper.
        // מתודה זו מחזירה Task<List<String[]>>, ו Firebase מטפל ב-threading ברקע.
        dbHelper.getAllUsernamesAndRoles()
                .addOnSuccessListener(allUsersData -> {
                    // בלוק זה מופעל כאשר הנתונים נשלפו בהצלחה מ-Firestore.
                    // יצירת רשימה שתכיל את המחרוזות שיוצגו ב-ListView.
                    List<String> userList = new ArrayList<>();
                    if (allUsersData != null) {
                        for (String[] userData : allUsersData) {
                            // userData[0] הוא שם המשתמש, userData[1] הוא התפקיד.
                            // הוספת מחרוזת מעוצבת לרשימה.
                            userList.add("Username: " + userData[0] + " | Role: " + userData[1]);
                        }
                    }

                    // חזרה ל-Thread הראשי (UI Thread) כדי לעדכן את ממשק הממשק.
                    runOnUiThread(() -> {
                        // בדיקה אם רשימת המשתמשים ריקה.
                        if (userList.isEmpty()) {
                            // הסתרת ה-ListView והצגת הודעת "אין משתמשים".
                            usersListView.setVisibility(View.GONE);
                            noUsersTextView.setVisibility(View.VISIBLE);
                            noUsersTextView.setText("No registered users to display.");
                            Log.d(TAG, "No registered users found.");
                        } else {
                            // הצגת ה-ListView והסתרת הודעת "אין משתמשים".
                            usersListView.setVisibility(View.VISIBLE);
                            noUsersTextView.setVisibility(View.GONE);

                            // יצירת מתאם (ArrayAdapter) סטנדרטי.
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                    UserManagementActivity.this,
                                    android.R.layout.simple_list_item_1,
                                    userList
                            );
                            // קישור המתאם ל-ListView.
                            usersListView.setAdapter(adapter);
                            Log.d(TAG, "Displayed " + userList.size() + " registered users.");
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    // בלוק זה מופעל אם הייתה שגיאה באחזור הנתונים מ-Firestore.
                    runOnUiThread(() -> {
                        String toastMsg = "Failed to load users: " + e.getMessage();
                        Toast.makeText(UserManagementActivity.this, toastMsg, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error loading users from Firebase: " + e.getMessage(), e);
                        // הצגת הודעת שגיאה במקום רשימת המשתמשים.
                        usersListView.setVisibility(View.GONE);
                        noUsersTextView.setVisibility(View.VISIBLE);
                        noUsersTextView.setText("Failed to load users. Please check your network connection.");
                    });
                });
        // executorService.execute() wrapping the dbHelper call is no longer needed
        // because Firebase Tasks handle their own background threading.
    }
}