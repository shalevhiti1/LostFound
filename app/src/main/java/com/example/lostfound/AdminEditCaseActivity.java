package com.example.lostfound; // מגדיר את חבילת הקוד של האפליקציה.

import android.os.Bundle; // ייבוא המחלקה Bundle, המשמשת לשמירה ושחזור מצב האקטיביטי.
import android.text.TextUtils; // ייבוא המחלקה TextUtils, המספקת שיטות עזר לבדיקת מחרוזות.
import android.view.View; // ייבוא המחלקה View, הבסיס לכל רכיבי ממשק המשתמש.
import android.widget.ArrayAdapter; // ייבוא ArrayAdapter, המשמש לקישור נתונים ל-Spinner.
import android.widget.Button; // ייבוא המחלקה Button, המשמשת ליצירת כפתורים.
import android.widget.EditText; // ייבוא המחלקה EditText, המשמשת לשדות קלט טקסט.
import android.widget.Spinner; // ייבוא המחלקה Spinner, המשמשת לרשימות נפתחות.
import android.widget.TextView; // ייבוא המחלקה TextView, המשמשת להצגת טקסט.
import android.widget.Toast; // ייבוא המחלקה Toast, המשמשת להצגת הודעות קצרות למשתמש.

import androidx.appcompat.app.AppCompatActivity; // ייבוא מחלקת הבסיס AppCompatActivity, המספקת תאימות לאחור.

import java.util.concurrent.ExecutorService; // ייבוא ExecutorService, לניהול Threads ברקע.
import java.util.concurrent.Executors; // ייבוא Executors, ליצירת מופעי ExecutorService.

/**
 * המחלקה {@code AdminEditCaseActivity} מאפשרת למשתמשי אדמין לערוך את פרטי פנייה קיימת.
 * האדמין יכול לעדכן את סטטוס הפנייה ולהוסיף או לערוך הערות מערכת.
 * טעינת הנתונים ושמירת השינויים מתבצעות ב-Thread רקע כדי למנוע תקיעות בממשק המשתמש.
 */
public class AdminEditCaseActivity extends AppCompatActivity {

    // משתני ממשק המשתמש להצגת פרטי הפנייה.
    private TextView adminEditCaseIdTextView, adminEditItemTypeTextView, adminEditReporterTextView;
    /**
     * רכיב {@code Spinner} לבחירת סטטוס הפנייה (לדוגמה: "בתהליך", "נמצא", "נסגר").
     */
    private Spinner statusSpinner;
    /**
     * שדה קלט טקסט לעריכת הערות מערכת הקשורות לפנייה.
     */
    private EditText systemCommentsEditText;
    /**
     * כפתור לשמירת השינויים שבוצעו על ידי האדמין.
     */
    private Button saveAdminChangesButton;
    /**
     * מופע של {@code DatabaseHelper} לביצוע פעולות על מסד הנתונים.
     */
    private DatabaseHelper dbHelper;
    /**
     * מזהה ייחודי של הפנייה הנערכת. מתקבל מה-Intent.
     */
    private int requestId;
    /**
     * אובייקט {@code Request} המכיל את כל פרטי הפנייה הנוכחית שנטענה ממסד הנתונים.
     */
    private Request currentRequest;
    /**
     * שירות לביצוע פעולות אסינכרוניות (ב-Thread רקע) כדי למנוע חסימת ממשק המשתמש.
     */
    private ExecutorService executorService;

    /**
     * מתודת מחזור החיים {@code onCreate} נקראת כאשר האקטיביטי נוצר לראשונה.
     * כאן מתבצעות רוב פעולות האתחול של המסך, כולל טעינת ה-Layout, קישור רכיבי UI,
     * קבלת נתונים מה-Intent, אתחול מסד הנתונים ושירות ה-ExecutorService.
     *
     * @param savedInstanceState אובייקט {@code Bundle} המכיל את הנתונים שנשמרו ממצב קודם של האקטיביטי, אם קיים.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // קריאה למתודת onCreate של מחלקת האב (AppCompatActivity) לביצוע אתחול בסיסי.
        super.onCreate(savedInstanceState);
        // הגדרת קובץ ה-Layout (עיצוב הממשק) עבור מסך זה.
        // R.layout.activity_admin_edit_case מפנה לקובץ ה-XML שמגדיר את מבנה המסך.
        setContentView(R.layout.activity_admin_edit_case);

        // אתחול מופע של DatabaseHelper לגישה למסד הנתונים.
        dbHelper = new DatabaseHelper(this);
        // אתחול שירות ה-ExecutorService לביצוע משימות ב-Thread רקע.
        executorService = Executors.newSingleThreadExecutor();

        // קישור רכיבי ממשק המשתמש (UI) למשתנים המתאימים במחלקה.
        // findViewById() מאתר את הרכיבים בקובץ ה-Layout לפי ה-ID שלהם.
        adminEditCaseIdTextView = findViewById(R.id.adminEditCaseIdTextView);
        adminEditItemTypeTextView = findViewById(R.id.adminEditItemTypeTextView);
        adminEditReporterTextView = findViewById(R.id.adminEditReporterTextView);
        statusSpinner = findViewById(R.id.statusSpinner);
        systemCommentsEditText = findViewById(R.id.systemCommentsEditText);
        saveAdminChangesButton = findViewById(R.id.saveAdminChangesButton);

        // קבלת מזהה הפנייה (REQUEST_ID) מה-Intent שפתח את האקטיביטי הזה.
        // אם ה-ID לא סופק, הערך ברירת המחדל יהיה -1.
        requestId = getIntent().getIntExtra("REQUEST_ID", -1);

        // בדיקה האם מזהה הפנייה תקין.
        // אם requestId הוא -1, זה מצביע על שגיאה (לא סופק ID פנייה).
        if (requestId == -1) {
            // הצגת הודעת שגיאה למשתמש.
            Toast.makeText(this, "Error: No case ID provided.", Toast.LENGTH_SHORT).show();
            // סגירת האקטיביטי וחזרה למסך הקודם.
            finish();
            return; // יציאה ממתודת onCreate.
        }

        // טעינת פרטי הפנייה ממסד הנתונים.
        // פעולה זו מתבצעת ב-Thread רקע כדי לא לחסום את ממשק המשתמש.
        loadCaseDetails();

        // הגדרת המתאם (Adapter) עבור ה-Spinner של הסטטוסים.
        // המתאם מקשר בין רשימת הסטטוסים המוגדרת בקובץ המשאבים (R.array.case_statuses)
        // לבין תצוגת ה-Spinner.
        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.case_statuses, android.R.layout.simple_spinner_item);
        // הגדרת פריסת התצוגה עבור כל פריט ברשימה הנפתחת של ה-Spinner.
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // קישור המתאם ל-Spinner.
        statusSpinner.setAdapter(statusAdapter);

        // הגדרת מאזין לחיצות (OnClickListener) עבור כפתור שמירת השינויים.
        // כאשר הכפתור נלחץ, מתודת saveAdminChanges() תיקרא.
        saveAdminChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAdminChanges();
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
     * טוענת את פרטי הפנייה ממסד הנתונים בהתבסס על ה-requestId.
     * פעולה זו מתבצעת ב-Thread רקע באמצעות {@code ExecutorService}.
     * לאחר טעינת הנתונים, ממשק המשתמש מעודכן ב-Thread הראשי.
     */
    private void loadCaseDetails() {
        // הפעלת משימה חדשה ב-Thread רקע.
        executorService.execute(() -> {
            // קריאה למתודה getRequestById של DatabaseHelper כדי לשלוף את הפנייה ממסד הנתונים.
            currentRequest = dbHelper.getRequestById(requestId);

            // חזרה ל-Thread הראשי (UI Thread) כדי לעדכן את ממשק המשתמש.
            runOnUiThread(() -> {
                // בדיקה האם הפנייה נמצאה במסד הנתונים.
                if (currentRequest != null) {
                    // עדכון שדות הטקסט ב-UI עם פרטי הפנייה.
                    adminEditCaseIdTextView.setText("Case ID: " + currentRequest.getId());
                    adminEditItemTypeTextView.setText("Item Type: " + currentRequest.getItemType());
                    adminEditReporterTextView.setText("Reporter: " + currentRequest.getFullName() + " (" + currentRequest.getUsername() + ")");
                    // הצגת הערות המערכת הקיימות בשדה העריכה.
                    systemCommentsEditText.setText(currentRequest.getSystemComments());

                    // הגדרת הבחירה הראשונית ב-Spinner של הסטטוסים.
                    // מאחזר את המתאם הנוכחי של ה-Spinner.
                    ArrayAdapter<CharSequence> statusAdapter = (ArrayAdapter<CharSequence>) statusSpinner.getAdapter();
                    // אם הסטטוס של הפנייה הנוכחית קיים במתאם, מגדיר אותו כבחירה הראשונית.
                    if (currentRequest.getStatus() != null && statusAdapter != null) {
                        int spinnerPosition = statusAdapter.getPosition(currentRequest.getStatus());
                        statusSpinner.setSelection(spinnerPosition);
                    }
                } else {
                    // הצגת הודעת שגיאה אם הפנייה לא נמצאה.
                    Toast.makeText(AdminEditCaseActivity.this, "Error: Case not found.", Toast.LENGTH_SHORT).show();
                    // סגירת האקטיביטי.
                    finish();
                }
            });
        });
    }

    /**
     * שומרת את השינויים שבוצעו על ידי האדמין בפרטי הפנייה.
     * המתודה מאחזרת את הסטטוס וההערות החדשות, מעדכנת את אובייקט הפנייה
     * ושומרת אותו במסד הנתונים ב-Thread רקע.
     */
    private void saveAdminChanges() {
        // בדיקה האם אובייקט הפנייה הנוכחית טעון.
        // אם currentRequest הוא null, זה מצביע על שגיאה.
        if (currentRequest == null) {
            Toast.makeText(this, "Error: No case to save.", Toast.LENGTH_SHORT).show();
            return; // יציאה מהמתודה.
        }

        // קבלת הסטטוס החדש שנבחר מה-Spinner.
        String newStatus = statusSpinner.getSelectedItem().toString();
        // קבלת הערות המערכת החדשות משדה הטקסט, תוך הסרת רווחים מיותרים.
        String newSystemComments = systemCommentsEditText.getText().toString().trim();

        // בדיקת תקינות: וודא שהערות המערכת אינן ריקות.
        if (TextUtils.isEmpty(newSystemComments)) {
            Toast.makeText(this, "System comments cannot be empty.", Toast.LENGTH_SHORT).show();
            return; // יציאה מהמתודה.
        }

        // עדכון אובייקט הפנייה הנוכחית עם הערכים החדשים.
        currentRequest.setStatus(newStatus);
        currentRequest.setSystemComments(newSystemComments);

        // ביצוע פעולת העדכון במסד הנתונים ב-Thread רקע.
        executorService.execute(() -> {
            // קריאה למתודה updateRequest של DatabaseHelper כדי לעדכן את הפנייה במסד הנתונים.
            boolean isUpdated = dbHelper.updateRequest(currentRequest);

            // חזרה ל-Thread הראשי (UI Thread) כדי להציג את תוצאות השמירה.
            runOnUiThread(() -> {
                // בדיקה האם העדכון הצליח.
                if (isUpdated) {
                    Toast.makeText(AdminEditCaseActivity.this, "Case updated successfully!", Toast.LENGTH_SHORT).show();
                    // סגירת האקטיביטי וחזרה למסך הקודם (AllCasesActivity).
                    finish();
                } else {
                    Toast.makeText(AdminEditCaseActivity.this, "Failed to update case. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
