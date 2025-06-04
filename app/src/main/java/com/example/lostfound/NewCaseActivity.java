package com.example.lostfound; // מגדיר את חבילת הקוד של האפליקציה.

import android.app.DatePickerDialog; // ייבוא DatePickerDialog, המשמש להצגת חלון בחירת תאריך.
import android.content.Intent; // ייבוא המחלקה Intent, המשמשת למעבר בין מסכים (Activities).
import android.os.Bundle; // ייבוא המחלקה Bundle, המשמשת לשמירה ושחזור מצב האקטיביטי.
import android.text.TextUtils; // ייבוא המחלקה TextUtils, המספקת שיטות עזר לבדיקת מחרוזות.
import android.view.View; // ייבוא המחלקה View, הבסיס לכל רכיבי ממשק המשתמש.
import android.widget.Button; // ייבוא המחלקה Button, המשמשת ליצירת כפתורים.
import android.widget.DatePicker; // ייבוא DatePicker, רכיב ממשק המשתמש לבחירת תאריך.
import android.widget.EditText; // ייבוא המחלקה EditText, המשמשת לשדות קלט טקסט.
import android.widget.Toast; // ייבוא המחלקה Toast, המשמשת להצגת הודעות קצרות למשתמש.

import androidx.appcompat.app.AppCompatActivity; // ייבוא מחלקת הבסיס AppCompatActivity, המספקת תאימות לאחור.

import java.text.SimpleDateFormat; // ייבוא SimpleDateFormat, לעיצוב תאריכים.
import java.util.Calendar; // ייבוא Calendar, לטיפול בתאריכים ושעות.
import java.util.Date; // ייבוא Date, לייצוג תאריכים ושעות.
import java.util.Locale; // ייבוא Locale, להגדרת אזור גיאוגרפי (לעיצוב תאריכים).
import java.util.concurrent.ExecutorService; // ייבוא ExecutorService, לניהול Threads ברקע.
import java.util.concurrent.Executors; // ייבוא Executors, ליצירת מופעי ExecutorService.

// ייבוא לפונקציה הסטטית של הנוטיפיקציה
import static com.example.lostfound.NotificationUtils.scheduleNotification;
import static com.example.lostfound.NotificationUtils.showSimpleNotification; // ייבוא showSimpleNotification

/**
 * המחלקה {@code NewCaseActivity} מאפשרת למשתמשים רגילים לפתוח דיווח חדש על אבידה.
 * היא מציגה שדות קלט למילוי פרטים מקיפים על הפריט שאבד ועל הנסיעה שבה האבידה התרחשה.
 * לאחר מילוי הפרטים ושמירתם, המידע נשמר במסד הנתונים, והמשתמש מקבל אישור (כולל נוטיפיקציה).
 * טעינת הנתונים ושמירתם מתבצעות ב-Thread רקע כדי למנוע תקיעות בממשק הממשק.
 */
public class NewCaseActivity extends AppCompatActivity {

    // הצהרה על משתני ממשק המשתמש (EditText ו-Button).
    private EditText itemTypeEditText, colorEditText, brandEditText, ownerNameEditText, lossDescriptionEditText;
    private EditText tripDateEditText, tripTimeEditText, originEditText, destinationEditText, lineNumberEditText;
    /**
     * כפתור לשמירת הדיווח החדש על אבידה.
     */
    private Button saveCaseButton;
    /**
     * מופע של {@code DatabaseHelper} לביצוע פעולות על מסד הנתונים.
     */
    private DatabaseHelper dbHelper;
    /**
     * שם המשתמש של המשתמש המחובר כעת, שפותח את הפנייה.
     */
    private String username;
    /**
     * מופע של {@code Calendar} המשמש לבחירת תאריך הנסיעה באמצעות {@code DatePickerDialog}.
     */
    private Calendar calendar;
    /**
     * שירות לביצוע פעולות אסינכרוניות (ב-Thread רקע) כדי למנוע חסימת ממשק המשתמש.
     */
    private ExecutorService executorService;


    /**
     * מתודת מחזור החיים {@code onCreate} נקראת כאשר האקטיביטי נוצר לראשונה.
     * כאן מתבצעות רוב פעולות האתחול של המסך, כולל טעינת ה-Layout, קישור רכיבי UI,
     * אתחול מסד הנתונים ושירות ה-ExecutorService, קבלת שם המשתמש מה-Intent,
     * והגדרת מאזינים לרכיבי ממשק המשתמש.
     *
     * @param savedInstanceState אובייקט {@code Bundle} המכיל את הנתונים שנשמרו ממצב קודם של האקטיביטי, אם קיים.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // קריאה למתודת onCreate של מחלקת האב (AppCompatActivity) לביצוע אתחול בסיסי.
        super.onCreate(savedInstanceState);
        // הגדרת קובץ ה-Layout (עיצוב הממשק) עבור מסך זה.
        // R.layout.activity_new_case מפנה לקובץ ה-XML שמגדיר את מבנה המסך.
        setContentView(R.layout.activity_new_case);

        // אתחול מופע של DatabaseHelper לגישה למסד הנתונים.
        dbHelper = new DatabaseHelper(this);
        // אתחול שירות ה-ExecutorService לביצוע משימות ב-Thread רקע.
        executorService = Executors.newSingleThreadExecutor();
        // קבלת שם המשתמש שהועבר לאקטיביטי זה באמצעות Intent (לדוגמה, מ-UserActivity).
        username = getIntent().getStringExtra("username");

        // קישור רכיבי ממשק המשתמש (EditTexts ו-Button) מתוך קובץ ה-XML.
        // findViewById() מאתר את הרכיבים בקובץ ה-Layout לפי ה-ID שלהם.
        itemTypeEditText = findViewById(R.id.itemTypeEditText);
        colorEditText = findViewById(R.id.colorEditText);
        brandEditText = findViewById(R.id.brandEditText);
        ownerNameEditText = findViewById(R.id.ownerNameEditText);
        lossDescriptionEditText = findViewById(R.id.lossDescriptionEditText);
        tripDateEditText = findViewById(R.id.tripDateEditText);
        tripTimeEditText = findViewById(R.id.tripTimeEditText);
        originEditText = findViewById(R.id.originEditText);
        destinationEditText = findViewById(R.id.destinationEditText);
        lineNumberEditText = findViewById(R.id.lineNumberEditText);
        saveCaseButton = findViewById(R.id.saveCaseButton);
        calendar = Calendar.getInstance();

        // Set up DatePickerDialog for tripDateEditText
        DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, monthOfYear);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateLabel();
            }
        };

        tripDateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(NewCaseActivity.this, date, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        saveCaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCaseToDatabase();
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
     * מעדכנת את הטקסט בשדה {@code tripDateEditText} עם התאריך שנבחר ב-{@code DatePickerDialog}.
     * התאריך מעוצב לפורמט "dd/MM/yyyy".
     */
    private void updateDateLabel() {
        String myFormat = "dd/MM/yyyy"; // הגדרת פורמט התאריך.
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault()); // יצירת אובייקט לעיצוב תאריכים.
        // עדכון הטקסט בשדה תאריך הנסיעה עם התאריך המעוצב מאובייקט ה-Calendar.
        tripDateEditText.setText(sdf.format(calendar.getTime()));
    }

    /**
     * אוספת את הנתונים משדות הקלט, מבצעת ולידציה בסיסית,
     * שולפת פרטי משתמש נוספים ממסד הנתונים, יוצרת אובייקט {@code Request} חדש,
     * ושומרת אותו במסד הנתונים. פעולות אלו מתבצעות ב-Thread רקע.
     * לאחר שמירה מוצלחת, מוצגת הודעת Toast ונוטיפיקציה, והאקטיביטי נסגר.
     */
    private void saveCaseToDatabase() {
        // קבלת הערכים משדות הקלט, תוך הסרת רווחים מיותרים.
        String itemType = itemTypeEditText.getText().toString().trim();
        String color = colorEditText.getText().toString().trim();
        String brand = brandEditText.getText().toString().trim();
        String ownerName = ownerNameEditText.getText().toString().trim();
        String lossDescription = lossDescriptionEditText.getText().toString().trim();
        Date tripDate = calendar.getTime(); // קבלת התאריך מאובייקט ה-Calendar.
        String tripTime = tripTimeEditText.getText().toString().trim();
        String origin = originEditText.getText().toString().trim();
        String destination = destinationEditText.getText().toString().trim();
        String lineNumber = lineNumberEditText.getText().toString().trim();

        // ולידציה בסיסית: ודא שכל שדות החובה אינם ריקים.
        if (TextUtils.isEmpty(itemType) || TextUtils.isEmpty(color) || TextUtils.isEmpty(brand) ||
                TextUtils.isEmpty(ownerName) || TextUtils.isEmpty(lossDescription) || TextUtils.isEmpty(tripTime) ||
                TextUtils.isEmpty(origin) || TextUtils.isEmpty(destination) || TextUtils.isEmpty(lineNumber)) {
            // הצגת הודעת שגיאה למשתמש.
            Toast.makeText(NewCaseActivity.this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return; // יציאה מהמתודה.
        }

        executorService.execute(() -> {
            // שליפת פרטי המשתמש המלאים ממסד הנתונים (שם מלא, ת"ז, טלפון, אימייל, עיר)
            // בהתבסס על שם המשתמש המחובר.
            String fullName = dbHelper.getUserFullName(username);
            String idCard = dbHelper.getUserIdCard(username);
            String phoneNumber = dbHelper.getUserPhoneNumber(username);
            String email = dbHelper.getUserEmail(username);
            String city = dbHelper.getUserCity(username);

            // קבלת חותמת הזמן הנוכחית
            long currentTimestamp = System.currentTimeMillis();

            // יצירת אובייקט Request חדש עם כל הפרטים שנאספו, כולל חותמת הזמן.
            // הסטטוס והערות המערכת יאותחלו בבנאי של Request לערכי ברירת מחדל.
            Request request = new Request(username, fullName, idCard, phoneNumber, email, city,
                    itemType, color, brand, ownerName, lossDescription,
                    tripDate, tripTime, origin, destination, lineNumber,
                    currentTimestamp); // העברת currentTimestamp

            // שמירת הדיווח החדש במסד הנתונים.
            // addRequest() מחזירה את ה-ID של השורה החדשה שהוכנסה.
            long newRowId = dbHelper.addRequest(request);

            // חזרה ל-Thread הראשי (UI Thread) כדי לעדכן את ממשק המשתמש ולהציג הודעות.
            runOnUiThread(() -> {
                // בדיקה האם הדיווח נשמר בהצלחה (newRowId אינו -1).
                if (newRowId != -1) {
                    // הצגת הודעת הצלחה למשתמש, כולל מספר הפנייה.
                    Toast.makeText(NewCaseActivity.this, "Case saved successfully. ID: " + newRowId, Toast.LENGTH_LONG).show();

                    // הצגת התראה מיידית עם פתיחת הפנייה
                    showSimpleNotification(
                            NewCaseActivity.this,
                            "Case Opened Successfully!", // Notification title in English
                            "Your case ID " + newRowId + " has been opened and is being processed.", // Notification content in English
                            (int) newRowId + 1000 // A unique ID for this immediate notification, different from the scheduled one
                    );

                    // תזמון נוטיפיקציה שתופיע 3 ימים מהיום
                    long threeDaysInMillis = 3 * 24 * 60 * 60 * 1000L; // 3 ימים במילישניות
                    long futureTime = currentTimestamp + threeDaysInMillis; // שימוש ב-currentTimestamp כבסיס

                    scheduleNotification(
                            NewCaseActivity.this,
                            "Case Update Reminder", // Notification title in English
                            "Your case ID " + newRowId + " is still being processed. We will update you soon.", // Notification content in English
                            (int) newRowId, // ID ייחודי לנוטיפיקציה (ניתן להשתמש ב-ID של הפנייה)
                            futureTime // הזמן העתידי שבו ההתראה תופיע
                    );

                    // סגירת האקטיביטי וחזרה למסך הקודם (UserActivity).
                    finish();
                } else {
                    // הצגת הודעת כישלון אם השמירה נכשלה.
                    Toast.makeText(NewCaseActivity.this, "Failed to save case. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
