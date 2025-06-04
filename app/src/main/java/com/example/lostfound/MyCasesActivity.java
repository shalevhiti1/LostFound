package com.example.lostfound; // מגדיר את חבילת הקוד של האפליקציה.

import android.annotation.SuppressLint; // ייבוא Annotation לדיכוי אזהרות Lint ספציפיות.
import android.content.Intent; // ייבוא המחלקה Intent, המשמשת למעבר בין מסכים (Activities).
import android.os.Bundle; // ייבוא המחלקה Bundle, המשמשת לשמירה ושחזור מצב האקטיביטי.
import android.util.Log; // ייבוא המחלקה Log, המשמש לרישום הודעות למסוף (לצרכי דיבוג).
import android.view.View; // ייבוא המחלקה View, הבסיס לכל רכיבי ממשק המשתמש.
import android.widget.AdapterView; // ייבוא AdapterView, המשמש לטיפול בלחיצות על פריטים ברשימה.
import android.widget.ListView; // ייבוא ListView, המשמש להצגת רשימות פריטים.
import android.widget.TextView; // ייבוא TextView, המשמש להצגת טקסט.
import android.widget.Toast; // ייבוא Toast, המשמש להצגת הודעות קצרות למשתמש.

import androidx.appcompat.app.AppCompatActivity; // ייבוא מחלקת הבסיס AppCompatActivity, המספקת תאימות לאחור.

import java.util.List; // ייבוא List, ממשק לייצוג רשימות.
import java.util.concurrent.ExecutorService; // ייבוא ExecutorService, לניהול Threads ברקע.
import java.util.concurrent.Executors; // ייבוא Executors, ליצירת מופעי ExecutorService.

/**
 * המחלקה {@code MyCasesActivity} אחראית על הצגת רשימה של כל הפניות על אבידות
 * שפתח המשתמש המחובר כעת. היא מאפשרת למשתמש לצפות בסקירה של הדיווחים שלו
 * וללחוץ על דיווח ספציפי כדי לראות פרטים נוספים.
 * טעינת הנתונים מתבצעת ב-Thread רקע כדי למנוע תקיעות בממשק המשתמש.
 */
public class MyCasesActivity extends AppCompatActivity {

    /**
     * רכיב {@code ListView} להצגת רשימת הפניות של המשתמש.
     */
    private ListView myCasesListView;
    /**
     * רכיב {@code TextView} המוצג כאשר למשתמש אין פניות פתוחות או אם אירעה שגיאה בטעינה.
     */
    private TextView noCasesTextView;
    /**
     * מופע של {@code DatabaseHelper} לביצוע פעולות על מסד הנתונים.
     */
    private DatabaseHelper dbHelper;
    /**
     * שם המשתמש של המשתמש המחובר כעת, שאת פניותיו אנו מציגים.
     */
    private String username;
    /**
     * שירות לביצוע פעולות אסינכרוניות (ב-Thread רקע) כדי למנוע חסימת ממשק המשתמש.
     */
    private ExecutorService executorService;

    /**
     * מתודת מחזור החיים {@code onCreate} נקראת כאשר האקטיביטי נוצר לראשונה.
     * כאן מתבצעות רוב פעולות האתחול של המסך, כולל טעינת ה-Layout, קישור רכיבי UI,
     * אתחול מסד הנתונים ושירות ה-ExecutorService, קבלת שם המשתמש מה-Intent,
     * וטעינה ראשונית של הפניות.
     *
     * @param savedInstanceState אובייקט {@code Bundle} המכיל את הנתונים שנשמרו ממצב קודם של האקטיביטי, אם קיים.
     */
    @SuppressLint("SetTextI18n") // דיכוי אזהרת Lint לגבי שרשור טקסט ב-setText, מכיוון שזהו שימוש לגיטימי כאן.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // קריאה למתודת onCreate של מחלקת האב (AppCompatActivity) לביצוע אתחול בסיסי.
        super.onCreate(savedInstanceState);
        // הגדרת קובץ ה-Layout (עיצוב הממשק) עבור מסך זה.
        // R.layout.activity_my_cases מפנה לקובץ ה-XML שמגדיר את מבנה המסך.
        setContentView(R.layout.activity_my_cases);

        // קישור רכיבי ממשק המשתמש (ListView ו-TextView) מתוך קובץ ה-XML.
        // findViewById() מאתר את הרכיבים בקובץ ה-Layout לפי ה-ID שלהם.
        myCasesListView = findViewById(R.id.myCasesListView);
        noCasesTextView = findViewById(R.id.noCasesTextView);

        // אתחול מופע של DatabaseHelper לגישה למסד הנתונים.
        dbHelper = new DatabaseHelper(this);
        // אתחול שירות ה-ExecutorService לביצוע משימות ב-Thread רקע.
        executorService = Executors.newSingleThreadExecutor();
        // קבלת שם המשתמש שהועבר לאקטיביטי זה באמצעות Intent (לדוגמה, מ-UserActivity).
        username = getIntent().getStringExtra("username");

        // בדיקה אם שם המשתמש התקבל בהצלחה.
        if (username == null) {
            // רישום שגיאה ללוג.
            Log.e("MyCasesActivity", "Username is null!");
            // הצגת הודעת שגיאה למשתמש.
            noCasesTextView.setText("Error: Username not found.");
            // הפיכת ה-TextView של השגיאה לגלוי.
            noCasesTextView.setVisibility(View.VISIBLE);
            // יציאה ממתודת onCreate.
            return;
        }

        // טעינה ראשונית של הפניות של המשתמש.
        // פעולה זו מתבצעת ב-Thread רקע.
        loadMyCases();
    }

    /**
     * מתודת מחזור החיים {@code onResume} נקראת כאשר האקטיביטי חוזר לפורגראונד (לדוגמה,
     * לאחר חזרה ממסך אחר כמו מסך פרטי פנייה).
     * כאן אנו מבצעים טעינה מחדש של הפניות כדי לוודא שהרשימה מעודכנת בשינויים שבוצעו
     * (לדוגמה, אם פנייה נערכה על ידי אדמין).
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadMyCases(); // טעינה מחדש של הפניות.
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
     * טוענת את הפניות של המשתמש הנוכחי ממסד הנתונים ומעדכנת את ה-ListView.
     * פעולה זו מתבצעת ב-Thread רקע באמצעות {@code ExecutorService} כדי לא לחסום את ממשק המשתמש.
     * לאחר טעינת הנתונים, ממשק המשתמש מעודכן ב-Thread הראשי.
     */
    @SuppressLint("SetTextI18n") // דיכוי אזהרת Lint לגבי שרשור טקסט ב-setText, מכיוון שזהו שימוש לגיטימי כאן.
    private void loadMyCases() {
        // רישום הודעה ללוג המציינת שהמתודה נקראה עבור שם המשתמש הנוכחי.
        Log.d("MyCasesActivity", "loadMyCases called for username: " + username);

        // הפעלת משימה חדשה ב-Thread רקע.
        executorService.execute(() -> {
            // קריאה למתודה getRequestsByUsername של DatabaseHelper
            // כדי לשלוף את כל הפניות של המשתמש הספציפי ממסד הנתונים.
            final List<Request> myRequests = dbHelper.getRequestsByUsername(username);

            // חזרה ל-Thread הראשי (UI Thread) כדי לעדכן את ממשק המשתמש.
            runOnUiThread(() -> {
                // בדיקה האם רשימת הפניות שהוחזרה היא null (מקרה שגיאה).
                if (myRequests == null) {
                    // רישום שגיאה ללוג.
                    Log.e("MyCasesActivity", "getRequestsByUsername returned null!");
                    // הצגת הודעת שגיאה למשתמש.
                    noCasesTextView.setText("Error loading requests.");
                    // הפיכת ה-TextView של השגיאה לגלוי.
                    noCasesTextView.setVisibility(View.VISIBLE);
                    // הסתרת ה-ListView.
                    myCasesListView.setVisibility(View.GONE);
                    return; // יציאה מהמתודה.
                }

                // בדיקה האם רשימת הפניות ריקה (כלומר, למשתמש אין פניות).
                if (myRequests.isEmpty()) {
                    // רישום הודעה ללוג.
                    Log.d("MyCasesActivity", "No requests found for username: " + username);
                    // הצגת הודעה מתאימה למשתמש.
                    noCasesTextView.setText("You have not opened any requests with us yet.");
                    // הפיכת ה-TextView לגלוי.
                    noCasesTextView.setVisibility(View.VISIBLE);
                    // הסתרת ה-ListView.
                    myCasesListView.setVisibility(View.GONE);
                } else {
                    // רישום הודעה ללוג המציינת כמה פניות נמצאו.
                    Log.d("MyCasesActivity", "Found " + myRequests.size() + " requests for username: " + username);
                    // הסתרת ה-TextView של "אין פניות".
                    noCasesTextView.setVisibility(View.GONE);
                    // הפיכת ה-ListView לגלוי.
                    myCasesListView.setVisibility(View.VISIBLE);

                    // יצירת מתאם (Adapter) מסוג RequestAdapter (מתאם מותאם אישית).
                    // מתאם זה אחראי על הצגת כל פריט ברשימה בצורה מותאמת אישית (באמצעות list_item_request.xml).
                    RequestAdapter adapter = new RequestAdapter(
                            MyCasesActivity.this, // הקונטקסט של האקטיביטי.
                            myRequests // רשימת הפניות להצגה.
                    );
                    // קישור המתאם ל-ListView.
                    myCasesListView.setAdapter(adapter);

                    // הגדרת מאזין לחיצות (OnItemClickListener) עבור פריטים ב-ListView של הפניות.
                    // כאשר המשתמש לוחץ על פנייה ברשימה, הוא יועבר למסך פרטי הפנייה.
                    myCasesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        /**
                         * נקראת כאשר פריט ברשימה נלחץ.
                         * @param parent ה-AdapterView שבו התרחשה הלחיצה.
                         * @param view ה-View בתוך ה-AdapterView שנלחץ.
                         * @param position המיקום של ה-View שנלחץ ברשימה.
                         * @param id ה-ID של השורה של הפריט שנלחץ.
                         */
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            // קבלת אובייקט ה-Request שנבחר מהרשימה.
                            Request selectedRequest = (Request) parent.getItemAtPosition(position);

                            // יצירת Intent חדש למעבר למסך CaseDetailsActivity (פרטי פנייה).
                            Intent intent = new Intent(MyCasesActivity.this, CaseDetailsActivity.class);
                            // העברת מזהה הפנייה (ID) של הפנייה שנבחרה למסך הבא.
                            intent.putExtra("REQUEST_ID", selectedRequest.getId());
                            // העברת שם המשתמש המחובר למסך הבא (חשוב לקביעת הרשאות תצוגה).
                            intent.putExtra("username", username);
                            // הפעלת האקטיביטי החדש.
                            startActivity(intent);
                        }
                    });
                }
            });
        });
    }
}
