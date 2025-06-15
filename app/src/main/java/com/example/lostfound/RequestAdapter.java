package com.example.lostfound; // מגדיר את חבילת הקוד של האפליקציה.

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * מתאם להצגת רשימת פניות (Request) בליסט-ויו.
 * קודכן להצגת ה-Firestore ID של הפנייה.
 */
public class RequestAdapter extends ArrayAdapter<Request> {

    private Context context;
    private List<Request> requests;

    /**
     * בנאי המתאם.
     * @param context הקשר לאקטיביטי/אפליקציה
     * @param requests רשימת פניות להצגה
     */
    public RequestAdapter(@NonNull Context context, @NonNull List<Request> requests) {
        super(context, 0, requests);
        this.context = context;
        this.requests = requests;
    }

    /**
     * יוצר/ממחזר View של פריט רשימה אחד וממלא אותו בנתוני הפנייה.
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(context).inflate(
                    R.layout.list_item_request, // ודא שקובץ ה-layout הזה קיים
                    parent,
                    false
            );
        }

        Request currentRequest = requests.get(position);

        TextView caseIdTextView = listItem.findViewById(R.id.listItemCaseId);
        TextView itemTypeTextView = listItem.findViewById(R.id.listItemItemType);
        TextView statusTextView = listItem.findViewById(R.id.listItemStatus);
        TextView tripDateTextView = listItem.findViewById(R.id.listItemTripDate);

        // שינוי עיקרי: הצג את ה-Firestore ID במקום ה-ID המספרי הישן.
        if (caseIdTextView != null) {
            caseIdTextView.setText("Case ID: " + currentRequest.getFirestoreId());
        }
        if (itemTypeTextView != null) {
            itemTypeTextView.setText("Item Type: " + currentRequest.getItemType());
        }
        if (statusTextView != null) {
            statusTextView.setText("Status: " + currentRequest.getStatus());
        }
        if (tripDateTextView != null) {
            // ה-tripDate ב-Request כעת הוא long (timestamp), לכן יש ליצור אובייקט Date ממנו.
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tripDateTextView.setText("Trip Date: " + dateFormat.format(new Date(String.valueOf(currentRequest.getTripDate()))));
        }

        return listItem;
    }
}