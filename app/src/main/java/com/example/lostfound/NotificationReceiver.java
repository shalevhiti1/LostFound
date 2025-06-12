package com.example.lostfound;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.lostfound.NotificationUtils.showSimpleNotification;

public class NotificationReceiver extends BroadcastReceiver {

    public static final String NOTIFICATION_TITLE = "notification_title";
    public static final String NOTIFICATION_MESSAGE = "notification_message";
    public static final String NOTIFICATION_ID = "notification_id";
    // נוסיף קבוע למזהה הפנייה
    public static final String REQUEST_ID = "request_id"; // חייב לוודא שגם ב-scheduleNotification אתה מעביר זאת!

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("NotificationReceiver", "Received broadcast to show notification.");

        String title = intent.getStringExtra(NOTIFICATION_TITLE);
        String message = intent.getStringExtra(NOTIFICATION_MESSAGE);
        int notificationId = intent.getIntExtra(NOTIFICATION_ID, 0);

        // שליפת מזהה הפנייה
        int requestId = intent.getIntExtra(REQUEST_ID, -1);

        if (requestId == -1) {
            // לא ניתן לבדוק סטטוס, נוהגים כמו קודם (למקרה תאימות לאחור)
            if (title != null && message != null) {
                Log.d("NotificationReceiver", "Showing notification: " + title + " - " + message);
                showSimpleNotification(context, title, message, notificationId);
            } else {
                Log.e("NotificationReceiver", "Notification details are missing from Intent.");
            }
            return;
        }

        // נבדוק את הסטטוס במסד הנתונים
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            Request request = dbHelper.getRequestById(requestId);

            if (request != null) {
                String status = request.getStatus();
                if (status != null && (
                        status.equals("IN_PROGRESS"))) {
                    // רק אם עדיין בתהליך - מציגים התראה!
                    Log.d("NotificationReceiver", "Request " + requestId + " is still pending, showing notification.");
                    showSimpleNotification(context, title, message, notificationId);
                } else {
                    Log.d("NotificationReceiver", "Request " + requestId + " is NOT pending anymore, not showing notification.");
                }
            } else {
                Log.e("NotificationReceiver", "Request with id " + requestId + " not found in DB.");
            }
            executor.shutdown();
        });
    }
}