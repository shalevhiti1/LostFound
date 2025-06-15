package com.example.lostfound;

import android.content.BroadcastReceiver; // ייבוא BroadcastReceiver, מחלקת בסיס לקבלת שידורים כלליים.
import android.content.Context; // ייבוא Context, המספק גישה למשאבי האפליקציה ושירותים.
import android.content.Intent; // ייבוא Intent, המשמש להעברת הודעות בין רכיבים.
import android.util.Log; // ייבוא Log, לרישום הודעות דיבוג.

// ייבוא לפונקציה הסטטית של הנוטיפיקציה
import static com.example.lostfound.NotificationUtils.showSimpleNotification;

/**
 * המחלקה {@code NotificationReceiver} היא {@code BroadcastReceiver} המיועדת לקבל
 * אירועים מ-{@code AlarmManager} ולשלוח התראות למשתמש.
 * היא מקבלת את פרטי ההתראה (כותרת, הודעה, ID) מתוך ה-Intent שהופעל
 * ומפעילה את {@code NotificationUtils.showSimpleNotification} כדי להציג אותה.
 */
public class NotificationReceiver extends BroadcastReceiver {

    // קבועים עבור מפתחות הנתונים ב-Intent
    public static final String NOTIFICATION_TITLE = "notification_title";
    public static final String NOTIFICATION_MESSAGE = "notification_message";
    public static final String NOTIFICATION_ID = "notification_id";

    /**
     * מתודה זו נקראת כאשר ה-{@code BroadcastReceiver} מקבל {@code Intent} משודר.
     * כאן, היא מטפלת ב-Intent שהופעל על ידי {@code AlarmManager} כדי להציג התראה.
     *
     * @param context הקונטקסט שבו ה-Receiver פועל.
     * @param intent ה-Intent שהתקבל, המכיל את פרטי ההתראה.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("NotificationReceiver", "Received broadcast to show notification.");

        // שליפת פרטי ההתראה מה-Intent.
        String title = intent.getStringExtra(NOTIFICATION_TITLE);
        String message = intent.getStringExtra(NOTIFICATION_MESSAGE);
        int notificationId = intent.getIntExtra(NOTIFICATION_ID, 0); // ברירת מחדל 0 אם לא נמצא

        // בדיקה אם הפרטים חוקיים לפני הצגת ההתראה.
        if (title != null && message != null) {
            Log.d("NotificationReceiver", "Showing notification: " + title + " - " + message);
            // קריאה למתודה הסטטית ב-NotificationUtils כדי להציג את ההתראה.
            showSimpleNotification(context, title, message, notificationId);
        } else {
            Log.e("NotificationReceiver", "Notification details are missing from Intent.");
        }
    }
}
