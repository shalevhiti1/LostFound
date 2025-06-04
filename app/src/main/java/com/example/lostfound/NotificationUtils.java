package com.example.lostfound; // מגדיר את חבילת הקוד של האפליקציה.


import android.Manifest; // ייבוא המחלקה Manifest, המשמשת לגישה להרשאות מערכת.
import android.annotation.SuppressLint;
import android.app.AlarmManager; // NEW: ייבוא AlarmManager, המשמש לתזמון אירועים עתידיים.
import android.app.NotificationManager; // ייבוא NotificationManager, המשמש לניהול התראות.
import android.app.PendingIntent; // ייבוא PendingIntent, המשמש להפעלת Intent במועד מאוחר יותר.
import android.content.Context; // ייבוא Context, המספק גישה למשאבי האפליקציה ושירותים.
import android.content.Intent; // ייבוא המחלקה Intent, המשמשת למעבר בין מסכים (Activities).
import android.content.pm.PackageManager; // ייבוא PackageManager, המשמש לבדיקת הרשאות.
import android.os.Build; // ייבוא Build, המשמש לבדיקת גרסת האנדרואיד הנוכחית.
import android.util.Log; // NEW: ייבוא Log, לרישום הודעות דיבוג.
import android.widget.Toast;

import androidx.core.app.ActivityCompat; // ייבוא ActivityCompat, המספק שיטות עזר לבקשת הרשאות.
import androidx.core.app.NotificationCompat; // ייבוא NotificationCompat, מחלקת עזר ליצירת התראות תואמות לאחור.
import androidx.core.app.NotificationManagerCompat; // ייבוא NotificationManagerCompat, מחלקת עזר לניהול התראות תואמות לאחור.

import com.example.lostfound.LoginActivity; // נשתמש ב-LoginActivity.DEFAULT_CHANNEL_ID
import com.example.lostfound.R; // עבור אייקונים וכו'
import com.example.lostfound.NotificationReceiver; // NEW: ייבוא NotificationReceiver

import java.util.Date; // NEW: ייבוא Date, לייצוג תאריכים ושעות.

/**
 * המחלקה {@code NotificationUtils} מספקת שיטות עזר סטטיות להצגת התראות (Notifications) באפליקציה.
 * היא מטפלת ביצירת התראות בסיסיות, כולל הגדרת אייקון, כותרת, תוכן, עדיפות, וטיפול ב-PendingIntent.
 * כמו כן, היא כוללת בדיקת הרשאות מתאימה עבור גרסאות אנדרואיד שונות.
 * בנוסף, היא מספקת פונקציונליות לתזמון התראות עתידיות באמצעות {@code AlarmManager}.
 */
public class NotificationUtils {

    /**
     * פונקציה סטטית פשוטה להצגת התראה מיידית.
     * מניחה שההרשאה להצגת התראות כבר ניתנה, ושערוץ ההתראות נוצר.
     *
     * @param context         הקונטקסט של האפליקציה או האקטיביטי.
     * @param title           כותרת ההתראה.
     * @param message         התוכן של ההתראה.
     * @param notificationId  ID ייחודי להתראה זו (כדי לאפשר עדכון/ביטול).
     * @param pendingIntent   (אופציונלי) PendingIntent שיופעל בלחיצה על ההתראה. אם null, לחיצה לא תעשה כלום.
     */
    public static void showSimpleNotification(Context context, String title, String message, int notificationId, PendingIntent pendingIntent) {
        // ודא שהקונטקסט אינו null
        if (context == null) {
            Log.e("NotificationUtils", "Context is null in showSimpleNotification.");
            return;
        }

        // שימוש ב-Application Context כדי למנוע דליפות זיכרון פוטנציאליות
        Context appContext = context.getApplicationContext();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, LoginActivity.DEFAULT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // החלף באייקון ברירת מחדל שלך
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // אפשר לשנות לפי הצורך
                .setAutoCancel(true); // ההתראה תיסגר אוטומטית בלחיצה

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(appContext);

        // בדיקה אחרונה של הרשאה לפני הצגה (במיוחד אם קוראים לפונקציה הזו ישירות ללא בדיקה מוקדמת)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // לא אמור לקרות אם הלוגיקה ב-LoginActivity טובה,
                // אבל זו הגנה נוספת. לא נציג את ההתראה אם אין הרשאה.
                Log.e("NotificationUtils", "Attempted to show notification without POST_NOTIFICATIONS permission.");
                return;
            }
        }
        notificationManager.notify(notificationId, builder.build());
    }

    /**
     * גרסה פשוטה יותר של הפונקציה להצגת התראה מיידית ללא PendingIntent.
     */
    public static void showSimpleNotification(Context context, String title, String message, int notificationId) {
        showSimpleNotification(context, title, message, notificationId, null);
    }

    /**
     * NEW: פונקציה סטטית לתזמון התראה שתופיע במועד עתידי באמצעות AlarmManager.
     * ההתראה תופעל על ידי NotificationReceiver.
     *
     * @param context        הקונטקסט של האפליקציה.
     * @param title          כותרת ההתראה.
     * @param message        התוכן של ההתראה.
     * @param notificationId ID ייחודי להתראה זו.
     * @param futureTimeMillis הזמן במילישניות (System.currentTimeMillis() + delay) שבו ההתראה אמורה להופיע.
     */
    @SuppressLint("ScheduleExactAlarm")
    public static void scheduleNotification(Context context, String title, String message, int notificationId, long futureTimeMillis) {
        if (context == null) {
            Log.e("NotificationUtils", "Context is null in scheduleNotification.");
            return;
        }

        Log.d("NotificationUtils", "Scheduling notification for " + new Date(futureTimeMillis).toString());

        // יצירת Intent שיפעיל את NotificationReceiver כאשר ה-Alarm יופעל.
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra(NotificationReceiver.NOTIFICATION_TITLE, title);
        intent.putExtra(NotificationReceiver.NOTIFICATION_MESSAGE, message);
        intent.putExtra(NotificationReceiver.NOTIFICATION_ID, notificationId);

        // יצירת PendingIntent עבור ה-BroadcastReceiver.
        // FLAG_IMMUTABLE נדרש החל מ-API 23.
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId, // השתמש ב-notificationId כ-requestCode כדי שיהיה ייחודי לכל התראה מתוזמנת.
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE // FLAG_UPDATE_CURRENT מעדכן נתונים אם ה-Intent קיים.
        );

        // קבלת מופע של AlarmManager.
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            // תזמון ההתראה.
            // RTC_WAKEUP מעיר את המכשיר אם הוא במצב שינה.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // עבור אנדרואיד 6.0 (Marshmallow) ומעלה, השתמש ב-setExactAndAllowWhileIdle
                // כדי להבטיח שההתראה תופעל גם במצב Doze (חיסכון בסוללה).
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, futureTimeMillis, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // עבור אנדרואיד 4.4 (KitKat) ומעלה, השתמש ב-setExact
                // כדי להבטיח שההתראה תופעל בזמן מדויק.
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, futureTimeMillis, pendingIntent);
            } else {
                // עבור גרסאות ישנות יותר, השתמש ב-set.
                alarmManager.set(AlarmManager.RTC_WAKEUP, futureTimeMillis, pendingIntent);
            }
            Log.d("NotificationUtils", "Notification scheduled successfully for ID: " + notificationId);
        } else {
            Log.e("NotificationUtils", "AlarmManager service not available.");
            Toast.makeText(context, "Failed to schedule notification.", Toast.LENGTH_SHORT).show(); // הודעה באנגלית
        }
    }
}
