package com.example.lostfound;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyCasesActivity extends AppCompatActivity {

    private ListView myCasesListView;
    private TextView noCasesTextView;
    private DatabaseHelper dbHelper;
    private String username;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_cases);

        myCasesListView = findViewById(R.id.myCasesListView);
        noCasesTextView = findViewById(R.id.noCasesTextView);

        dbHelper = new DatabaseHelper(this);
        username = getIntent().getStringExtra("username");

        if (username == null) {
            Log.e("MyCasesActivity", "Username is null!");
            noCasesTextView.setText("Error: Username not found.");
            noCasesTextView.setVisibility(View.VISIBLE);
            return;
        }

        loadMyCases();
    }

    @SuppressLint("SetTextI18n")
    private void loadMyCases() {
        Log.d("MyCasesActivity", "loadMyCases called for username: " + username);
        List<Request> myRequests = dbHelper.getRequestsByUsername(username);

        if (myRequests == null) {
            Log.e("MyCasesActivity", "getRequestsByUsername returned null!");
            noCasesTextView.setText("Error loading requests.");
            noCasesTextView.setVisibility(View.VISIBLE);
            return;
        }

        if (myRequests.isEmpty()) {
            Log.d("MyCasesActivity", "No requests found for username: " + username);
            noCasesTextView.setText("You have not opened any requests with us yet.");
            noCasesTextView.setVisibility(View.VISIBLE);
            myCasesListView.setVisibility(View.GONE);
        } else {
            Log.d("MyCasesActivity", "Found " + myRequests.size() + " requests for username: " + username);
            List<String> requestDetails = new ArrayList<>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            for (Request request : myRequests) {
                String formattedDate = dateFormat.format(request.getTripDate());
                requestDetails.add("Item: " + request.getItemType() + ", Color: " + request.getColor() + ", Date: " + formattedDate);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, requestDetails);
            myCasesListView.setAdapter(adapter);
            myCasesListView.setVisibility(View.VISIBLE);
            noCasesTextView.setVisibility(View.GONE);

            // Set OnItemClickListener for the ListView
            myCasesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // Get the selected Request object
                    Request selectedRequest = myRequests.get(position);

                    // Create an Intent to start CaseDetailsActivity
                    Intent intent = new Intent(MyCasesActivity.this, CaseDetailsActivity.class);

                    // Pass the Request object to CaseDetailsActivity
                    intent.putExtra("request", (CharSequence) selectedRequest);

                    // Start CaseDetailsActivity
                    startActivity(intent);
                }
            });
        }
    }
}