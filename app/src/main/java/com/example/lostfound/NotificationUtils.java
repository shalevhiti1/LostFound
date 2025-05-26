package com.example.lostfound;


import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.lostfound.LoginActivity; // נשתמש ב-LoginActivity.DEFAULT_CHANNEL_ID
import com.example.lostfound.R; // עבור אייקונים וכו'

public class NotificationUtils {

    /**
     * פונקציה סטטית פשוטה להצגת התראה.
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
                // אפשר להוסיף כאן לוג אם רוצים.
                // Log.e("NotificationUtils", "Attempted to show notification without POST_NOTIFICATIONS permission.");
                return;
            }
        }
        notificationManager.notify(notificationId, builder.build());
    }

    /**
     * גרסה פשוטה יותר של הפונקציה ללא PendingIntent.
     */
    public static void showSimpleNotification(Context context, String title, String message, int notificationId) {
        showSimpleNotification(context, title, message, notificationId, null);
    }
}