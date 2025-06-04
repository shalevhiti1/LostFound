package com.example.lostfound; // מגדיר את חבילת הקוד של האפליקציה.

import android.content.Intent; // ייבוא המחלקה Intent, המשמשת למעבר בין מסכים (Activities).
import android.os.Bundle; // ייבוא המחלקה Bundle, המשמשת לשמירה ושחזור מצב האקטיביטי.
import android.view.View; // ייבוא המחלקה View, הבסיס לכל רכיבי ממשק המשתמש.
import android.widget.AdapterView; // ייבוא AdapterView, המשמש לטיפול בלחיצות על פריטים ברשימה/ספינר.
import android.widget.ArrayAdapter; // ייבוא ArrayAdapter, המשמש לקישור נתונים לרשימות וספינרים.
import android.widget.ListView; // ייבוא ListView, המשמש להצגת רשימות פריטים.
import android.widget.SearchView; // ייבוא SearchView, המשמש לפונקציונליות חיפוש.
import android.widget.Spinner; // ייבוא Spinner, המשמש לרשימות נפתחות (פילטרים).
import android.widget.TextView; // ייבוא TextView, המשמש להצגת טקסט.
import android.widget.Toast; // ייבוא Toast, המשמש להצגת הודעות קצרות למשתמש.

import androidx.appcompat.app.AppCompatActivity; // ייבוא מחלקת הבסיס AppCompatActivity, המספקת תאימות לאחור.

import java.util.ArrayList; // ייבוא ArrayList, המשמש ליצירת רשימות דינמיות.
import java.util.List; // ייבוא List, ממשק לייצוג רשימות.
import java.util.concurrent.ExecutorService; // ייבוא ExecutorService, לניהול Threads ברקע.
import java.util.concurrent.Executors; // ייבוא Executors, ליצירת מופעי ExecutorService.

/**
 * המחלקה {@code AllCasesActivity} מציגה רשימה של כל הפניות על אבידות במערכת.
 * מסך זה מיועד בעיקר למשתמשי אדמין ומאפשר להם לחפש, לסנן ולצפות בפרטי כל פנייה.
 * טעינת הנתונים ויישום החיפוש/סינון מתבצעים ב-Thread רקע כדי למנוע תקיעות בממשק המשתמש.
 */
public class AllCasesActivity extends AppCompatActivity {

    /**
     * רכיב {@code ListView} להצגת רשימת הפניות.
     */
    private ListView allCasesListView;
    /**
     * רכיב {@code TextView} המוצג כאשר אין פניות התואמות את קריטריוני החיפוש/סינון.
     */
    private TextView noCasesTextView;
    /**
     * מופע של {@code DatabaseHelper} לביצוע פעולות על מסד הנתונים.
     */
    private DatabaseHelper dbHelper;
    /**
     * שירות לביצוע פעולות אסינכרוניות (ב-Thread רקע) כדי למנוע חסימת ממשק המשתמש.
     */
    private ExecutorService executorService;
    /**
     * רכיב {@code SearchView} המאפשר חיפוש טקסטואלי בפניות.
     */
    private SearchView caseSearchView;
    /**
     * רכיב {@code Spinner} המאפשר סינון פניות לפי סטטוס.
     */
    private Spinner statusFilterSpinner;

    /**
     * מחרוזת המכילה את שאילתת החיפוש הנוכחית שהוזנה על ידי המשתמש.
     */
    private String currentSearchQuery = "";
    /**
     * מחרוזת המכילה את סטטוס הסינון הנוכחי שנבחר מה-Spinner.
     * ברירת המחדל היא "All Cases".
     */
    private String currentFilterStatus = "All Cases";
    /**
     * שם המשתמש של המשתמש המחובר (בדרך כלל אדמין), המשמש להעברה למסכים הבאים.
     */
    private String loggedInUsername;

    /**
     * מתודת מחזור החיים {@code onCreate} נקראת כאשר האקטיביטי נוצר לראשונה.
     * כאן מתבצעות רוב פעולות האתחול של המסך, כולל טעינת ה-Layout, קישור רכיבי UI,
     * קבלת נתונים מה-Intent, אתחול מסד הנתונים ושירות ה-ExecutorService,
     * והגדרת מאזינים לרכיבי החיפוש והסינון.
     *
     * @param savedInstanceState אובייקט {@code Bundle} המכיל את הנתונים שנשמרו ממצב קודם של האקטיביטי, אם קיים.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // קריאה למתודת onCreate של מחלקת האב (AppCompatActivity) לביצוע אתחול בסיסי.
        super.onCreate(savedInstanceState);
        // הגדרת קובץ ה-Layout (עיצוב הממשק) עבור מסך זה.
        // R.layout.activity_all_cases מפנה לקובץ ה-XML שמגדיר את מבנה המסך.
        setContentView(R.layout.activity_all_cases);

        // אתחול מופע של DatabaseHelper לגישה למסד הנתונים.
        dbHelper = new DatabaseHelper(this);
        // אתחול שירות ה-ExecutorService לביצוע משימות ב-Thread רקע.
        executorService = Executors.newSingleThreadExecutor();

        // קישור רכיבי ממשק המשתמש (UI) למשתנים המתאימים במחלקה.
        // findViewById() מאתר את הרכיבים בקובץ ה-Layout לפי ה-ID שלהם.
        allCasesListView = findViewById(R.id.allCasesListView);
        noCasesTextView = findViewById(R.id.noCasesTextView);
        caseSearchView = findViewById(R.id.caseSearchView);
        statusFilterSpinner = findViewById(R.id.statusFilterSpinner);

        // קבלת שם המשתמש המחובר מה-Intent שפתח את האקטיביטי הזה.
        // שם משתמש זה נשלח בדרך כלל מ-AdminActivity.
        loggedInUsername = getIntent().getStringExtra("username");

        // הגדרת המתאם (Adapter) עבור ה-Spinner של פילטר הסטטוסים.
        // המתאם מקשר בין רשימת הסטטוסים המוגדרת בקובץ המשאבים (R.array.all_case_statuses_filter)
        // לבין תצוגת ה-Spinner.
        ArrayAdapter<CharSequence> statusFilterAdapter = ArrayAdapter.createFromResource(this,
                R.array.all_case_statuses_filter, android.R.layout.simple_spinner_item);
        // הגדרת פריסת התצוגה עבור כל פריט ברשימה הנפתחת של ה-Spinner.
        statusFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // קישור המתאם ל-Spinner.
        statusFilterSpinner.setAdapter(statusFilterAdapter);

        // הגדרת הבחירה הראשונית ב-Spinner של פילטר הסטטוסים.
        // מאתר את המיקום של הסטטוס הנוכחי (ברירת מחדל "All Cases") ומגדיר אותו כבחירה.
        int spinnerPosition = statusFilterAdapter.getPosition(currentFilterStatus);
        statusFilterSpinner.setSelection(spinnerPosition);

        // הגדרת מאזין לשינויים בטקסט של שדה החיפוש (SearchView).
        // כאשר המשתמש מקליד או לוחץ על חיפוש, המתודות המתאימות ייקראו.
        caseSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            /**
             * נקראת כאשר המשתמש לוחץ על כפתור החיפוש או מגיש את השאילתה.
             * @param query שאילתת החיפוש שהוזנה.
             * @return true אם השאילתה טופלה, false אחרת.
             */
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query; // עדכון שאילתת החיפוש הנוכחית.
                loadAllCases(); // טעינה מחדש של הפניות עם החיפוש החדש.
                return false; // מציין שהאירוע לא נצרך במלואו (מערכת ההפעלה יכולה לטפל בו).
            }

            /**
             * נקראת כאשר הטקסט בשדה החיפוש משתנה (בזמן הקלדה).
             * @param newText הטקסט החדש בשדה החיפוש.
             * @return true אם השינוי טופל, false אחרת.
             */
            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText; // עדכון שאילתת החיפוש בזמן אמת.
                loadAllCases(); // טעינה מחדש של הפניות עם החיפוש המעודכן.
                return false; // מציין שהאירוע לא נצרך במלואו.
            }
        });

        // הגדרת מאזין לבחירת פריט ב-Spinner של פילטר הסטטוסים.
        // כאשר המשתמש בוחר סטטוס חדש, הפניות ייטענו מחדש בהתאם.
        statusFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            /**
             * נקראת כאשר פריט נבחר ב-Spinner.
             * @param parent ה-AdapterView שבו התרחשה הבחירה.
             * @param view ה-View בתוך ה-AdapterView שנבחר.
             * @param position המיקום של ה-View שנבחר ברשימה.
             * @param id ה-ID של השורה של הפריט שנבחר.
             */
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFilterStatus = parent.getItemAtPosition(position).toString(); // עדכון סטטוס הסינון.
                loadAllCases(); // טעינה מחדש של הפניות עם הסינון החדש.
            }

            /**
             * נקראת כאשר לא נבחר שום פריט ב-Spinner (לדוגמה, כאשר המתאם מתרוקן).
             * @param parent ה-AdapterView שבו לא נבחר דבר.
             */
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // אין פעולה מיוחדת נדרשת במקרה זה.
            }
        });

        // טעינה ראשונית של כל הפניות כאשר המסך נפתח.
        loadAllCases();

        // הגדרת מאזין לחיצות (OnItemClickListener) עבור פריטים ב-ListView של הפניות.
        // כאשר המשתמש לוחץ על פנייה ברשימה, הוא יועבר למסך פרטי הפנייה.
        allCasesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
                Intent intent = new Intent(AllCasesActivity.this, CaseDetailsActivity.class);
                // העברת מזהה הפנייה (ID) של הפנייה שנבחרה למסך הבא.
                intent.putExtra("REQUEST_ID", selectedRequest.getId());
                // העברת שם המשתמש המחובר למסך הבא.
                // זה חשוב כדי ש-CaseDetailsActivity תוכל לקבוע אם המשתמש הוא אדמין ולהציג כפתור עריכה.
                intent.putExtra("username", loggedInUsername);
                // הפעלת האקטיביטי החדש.
                startActivity(intent);
            }
        });
    }

    /**
     * מתודת מחזור החיים {@code onResume} נקראת כאשר האקטיביטי חוזר לפורגראונד (לדוגמה,
     * לאחר חזרה ממסך אחר).
     * כאן אנו מבצעים טעינה מחדש של הפניות כדי לוודא שהרשימה מעודכנת בשינויים שבוצעו.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadAllCases(); // טעינה מחדש של הפניות.
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
     * טוענת את כל הפניות ממסד הנתונים, מיישמת חיפוש וסינון, ומעדכנת את ה-ListView.
     * פעולה זו מתבצעת ב-Thread רקע באמצעות {@code ExecutorService} כדי לא לחסום את ממשק המשתמש.
     * לאחר עיבוד הנתונים, ממשק המשתמש מעודכן ב-Thread הראשי.
     */
    private void loadAllCases() {
        // הפעלת משימה חדשה ב-Thread רקע.
        executorService.execute(() -> {
            // קריאה למתודה getAllRequests של DatabaseHelper כדי לשלוף את כל הפניות ממסד הנתונים.
            List<Request> allRequests = dbHelper.getAllRequests();
            // יצירת רשימה חדשה שתכיל את הפניות המסוננות והמחופשות.
            List<Request> filteredRequests = new ArrayList<>();

            // לולאה העוברת על כל הפניות שנשלפו ממסד הנתונים.
            for (Request request : allRequests) {
                // בדיקה האם הפנייה הנוכחית מתאימה לפילטר הסטטוסים.
                boolean matchesStatus = false;
                if (currentFilterStatus.equals("All Cases")) {
                    // אם הפילטר הוא "All Cases", כל הפניות מתאימות.
                    matchesStatus = true;
                } else if (request.getStatus() != null && request.getStatus().equals(currentFilterStatus)) {
                    // אם הסטטוס של הפנייה תואם לסטטוס שנבחר בפילטר.
                    matchesStatus = true;
                }

                // בדיקה האם הפנייה הנוכחית מתאימה לשאילתת החיפוש.
                boolean matchesSearch = true;
                if (!currentSearchQuery.isEmpty()) {
                    // המרת שאילתת החיפוש לאותיות קטנות לביצוע חיפוש לא תלוי רישיות.
                    String queryLower = currentSearchQuery.toLowerCase();
                    // בדיקה אם סוג הפריט, שם הבעלים או תיאור האבידה מכילים את שאילתת החיפוש.
                    if (!request.getItemType().toLowerCase().contains(queryLower) &&
                            !request.getOwnerName().toLowerCase().contains(queryLower) &&
                            !request.getLossDescription().toLowerCase().contains(queryLower)) {
                        matchesSearch = false; // אם אף אחד מהשדות לא מכיל את שאילתת החיפוש, לא תואם.
                    }
                }

                // אם הפנייה מתאימה גם לפילטר הסטטוס וגם לשאילתת החיפוש, הוסף אותה לרשימת הפניות המסוננות.
                if (matchesStatus && matchesSearch) {
                    filteredRequests.add(request);
                }
            }

            // חזרה ל-Thread הראשי (UI Thread) כדי לעדכן את ממשק המשתמש.
            runOnUiThread(() -> {
                // בדיקה האם רשימת הפניות המסוננות ריקה.
                if (filteredRequests.isEmpty()) {
                    // הסתרת ה-ListView והצגת הודעה שאין פניות.
                    allCasesListView.setVisibility(View.GONE);
                    noCasesTextView.setVisibility(View.VISIBLE);
                    noCasesTextView.setText("No cases found matching your criteria.");
                } else {
                    // הצגת ה-ListView והסתרת הודעת "אין פניות".
                    allCasesListView.setVisibility(View.VISIBLE);
                    noCasesTextView.setVisibility(View.GONE);

                    // יצירת מתאם (Adapter) מסוג RequestAdapter (מתאם מותאם אישית).
                    // מתאם זה אחראי על הצגת כל פריט ברשימה בצורה מותאמת אישית.
                    RequestAdapter adapter = new RequestAdapter(
                            AllCasesActivity.this, // הקונטקסט של האקטיביטי.
                            filteredRequests // רשימת הפניות המסוננות להצגה.
                    );
                    // קישור המתאם ל-ListView.
                    allCasesListView.setAdapter(adapter);
                }
            });
        });
    }
}
