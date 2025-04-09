package com.example.lostfound;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class AllCasesActivity extends AppCompatActivity {

    private ListView allCasesListView;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_cases);

        dbHelper = new DatabaseHelper(this);
        allCasesListView = findViewById(R.id.allCasesListView);

        loadAllCases();
    }

    private void loadAllCases() {
        List<Request> allRequests = dbHelper.getAllRequests();
        List<String> requestDetails = new ArrayList<>();
        for (Request request : allRequests) {
            requestDetails.add("User: " + request.getUsername() + ", Item: " + request.getItemType() + ", Color: " + request.getColor() + ", Date: " + request.getTripDate());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, requestDetails);
        allCasesListView.setAdapter(adapter);
    }
}