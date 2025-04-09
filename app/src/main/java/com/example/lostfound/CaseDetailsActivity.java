package com.example.lostfound;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class CaseDetailsActivity extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_case_details);

        // Initialize TextViews
        TextView itemTypeTextView = findViewById(R.id.itemTypeTextView);
        TextView colorTextView = findViewById(R.id.colorTextView);
        TextView brandTextView = findViewById(R.id.brandTextView);
        TextView ownerNameTextView = findViewById(R.id.ownerNameTextView);
        TextView lossDescriptionTextView = findViewById(R.id.lossDescriptionTextView);
        TextView tripDateTextView = findViewById(R.id.tripDateTextView);
        TextView tripTimeTextView = findViewById(R.id.tripTimeTextView);
        TextView originTextView = findViewById(R.id.originTextView);
        TextView destinationTextView = findViewById(R.id.destinationTextView);
        TextView lineNumberTextView = findViewById(R.id.lineNumberTextView);
        TextView fullNameTextView = findViewById(R.id.fullNameTextView);
        TextView idCardTextView = findViewById(R.id.idCardTextView);
        TextView phoneNumberTextView = findViewById(R.id.phoneNumberTextView);
        TextView emailTextView = findViewById(R.id.emailTextView);
        TextView cityTextView = findViewById(R.id.cityTextView);

        // Get the Request object from the Intent
        Request request = (Request) getIntent().getSerializableExtra("request");

        if (request != null) {
            // Populate the TextViews with the request details
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            itemTypeTextView.setText("Item Type: " + request.getItemType());
            colorTextView.setText("Color: " + request.getColor());
            brandTextView.setText("Brand: " + request.getBrand());
            ownerNameTextView.setText("Owner Name: " + request.getOwnerName());
            lossDescriptionTextView.setText("Loss Description: " + request.getLossDescription());
            tripDateTextView.setText("Trip Date: " + dateFormat.format(request.getTripDate()));
            tripTimeTextView.setText("Trip Time: " + request.getTripTime());
            originTextView.setText("Origin: " + request.getOrigin());
            destinationTextView.setText("Destination: " + request.getDestination());
            lineNumberTextView.setText("Line Number: " + request.getLineNumber());
            fullNameTextView.setText("Full Name: " + request.getFullName());
            idCardTextView.setText("ID Card: " + request.getIdCard());
            phoneNumberTextView.setText("Phone Number: " + request.getPhoneNumber());
            emailTextView.setText("Email: " + request.getEmail());
            cityTextView.setText("City: " + request.getCity());
        }
    }
}