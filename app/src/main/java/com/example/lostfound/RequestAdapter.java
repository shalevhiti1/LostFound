package com.example.lostfound; // מגדיר את חבילת הקוד של האפליקציה.

import android.content.Context; // ייבוא המחלקה Context, המספקת גישה למשאבי האפליקציה ושירותים.
import android.view.LayoutInflater; // ייבוא LayoutInflater, המשמש ליצירת אובייקטי View מקובצי Layout XML.
import android.view.View; // ייבוא המחלקה View, הבסיס לכל רכיבי ממשק המשתמש.
import android.view.ViewGroup; // ייבוא ViewGroup, מחלקת בסיס ל-Views שיכולים להכיל Views אחרים.
import android.widget.ArrayAdapter; // ייבוא ArrayAdapter, מתאם בסיסי לקישור נתונים ל-ListView.
import android.widget.TextView; // ייבוא TextView, המשמש להצגת טקסט.

import androidx.annotation.NonNull; // ייבוא Annotation המציין שפרמטר או ערך החזרה אינם יכולים להיות null.
import androidx.annotation.Nullable; // ייבוא Annotation המציין שפרמטר או ערך החזרה יכולים להיות null.

import java.text.SimpleDateFormat; // ייבוא SimpleDateFormat, לעיצוב תאריכים.
import java.util.List; // ייבוא List, ממשק לייצוג רשימות.
import java.util.Locale; // ייבוא Locale, להגדרת אזור גיאוגרפי (לעיצוב תאריכים).

/**
 * המחלקה {@code RequestAdapter} היא מתאם מותאם אישית עבור {@code ListView},
 * המאפשר להציג רשימה של אובייקטי {@code Request} בצורה מותאמת אישית.
 * היא אחראית על המרת אובייטי {@code Request} ל-{@code View}s שניתן להציג ב-{@code ListView},
 * תוך שימוש בקובץ ה-Layout המותאם אישית {@code list_item_request.xml}.
 */
public class RequestAdapter extends ArrayAdapter<Request> {

    /**
     * הקונטקסט של האפליקציה או האקטיביטי. נדרש לניפוח Layouts ולגישה למשאבים.
     */
    private Context context;
    /**
     * רשימת אובייקטי {@code Request} שאותם המתאם צריך להציג.
     */
    private List<Request> requests;

    /**
     * בנאי המחלקה {@code RequestAdapter}.
     *
     * @param context הקונטקסט של האפליקציה או האקטיביטי.
     * @param requests רשימה של אובייקטי {@code Request} להצגה.
     */
    public RequestAdapter(@NonNull Context context, @NonNull List<Request> requests) {
        // קריאה לבנאי של מחלקת האב (ArrayAdapter).
        // מועברים: הקונטקסט, 0 (מציין שאיננו משתמשים ב-Layout ברירת מחדל של ArrayAdapter), ורשימת הפניות.
        super(context, 0, requests);
        this.context = context; // שמירת הקונטקסט.
        this.requests = requests; // שמירת רשימת הפניות.
    }

    /**
     * מתודה זו נקראת עבור כל פריט ברשימה כדי לקבל את ה-{@code View} המייצג אותו.
     * היא מנפחת את קובץ ה-Layout המותאם אישית {@code list_item_request.xml}
     * וממלאת אותו בנתונים מאובייקט ה-{@code Request} המתאים.
     *
     * @param position המיקום של הפריט ברשימה.
     * @param convertView ה-{@code View} הישן לשימוש חוזר, אם קיים (לשיפור ביצועים).
     * @param parent ה-{@code ViewGroup} ההורה שאליו ה-{@code View} הזה ישויך.
     * @return ה-{@code View} המייצג את הפריט במיקום הנתון.
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // בדיקה אם קיים View לשימוש חוזר.
        // אם convertView הוא null, יש לנפח Layout חדש.
        View listItem = convertView;
        if (listItem == null) {
            // ניפוח ה-Layout המותאם אישית עבור פריט הרשימה.
            // LayoutInflater.from(context).inflate() ממיר קובץ XML של Layout לאובייקט View.
            listItem = LayoutInflater.from(context).inflate(
                    R.layout.list_item_request, // שימוש ב-Layout המותאם אישית עבור כל פריט ברשימה.
                    parent, // ה-ViewGroup ההורה.
                    false // false מציין שאין לצרף את ה-View באופן מיידי להורה (ArrayAdapter יעשה זאת).
            );
        }

        // קבלת אובייקט ה-Request הנוכחי מהרשימה במיקום הנתון.
        Request currentRequest = requests.get(position);

        // אתחול רכיבי ה-TextView מתוך ה-Layout המותאם אישית של פריט הרשימה.
        // findViewById() מאתר את רכיבי ה-UI בתוך ה-listItem (ה-View המנופח) לפי ה-ID שלהם.
        TextView caseIdTextView = listItem.findViewById(R.id.listItemCaseId);
        TextView itemTypeTextView = listItem.findViewById(R.id.listItemItemType);
        TextView statusTextView = listItem.findViewById(R.id.listItemStatus);
        TextView tripDateTextView = listItem.findViewById(R.id.listItemTripDate);

        // מילוי רכיבי ה-TextView בנתונים מאובייקט ה-Request הנוכחי.
        // בדיקה אם ה-TextView אינו null לפני עדכון הטקסט, כדי למנוע קריסות (למרות שבמקרה זה הם תמיד יהיו קיימים).
        if (caseIdTextView != null) {
            caseIdTextView.setText("Case ID: " + currentRequest.getId());
        }
        if (itemTypeTextView != null) {
            itemTypeTextView.setText("Item Type: " + currentRequest.getItemType());
        }
        if (statusTextView != null) {
            statusTextView.setText("Status: " + currentRequest.getStatus());
        }
        if (tripDateTextView != null) {
            // עיצוב התאריך (tripDate) לפורמט "dd/MM/yyyy" לפני הצגתו.
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tripDateTextView.setText("Trip Date: " + dateFormat.format(currentRequest.getTripDate()));
        }

        // החזרת ה-View המלא והמעודכן עבור פריט הרשימה הנוכחי.
        return listItem;
    }
}
