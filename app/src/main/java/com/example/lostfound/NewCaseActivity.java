package com.example.lostfound;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class NewCaseActivity extends AppCompatActivity {

    private EditText itemTypeEditText, colorEditText, brandEditText, ownerNameEditText, lossDescriptionEditText;
    private EditText tripDateEditText, tripTimeEditText, originEditText, destinationEditText, lineNumberEditText;
    private Button saveCaseButton;
    private DatabaseHelper dbHelper;
    private String username;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_case);

        dbHelper = new DatabaseHelper(this);
        username = getIntent().getStringExtra("username");

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

    private void updateDateLabel() {
        String myFormat = "MM/dd/yy"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        tripDateEditText.setText(sdf.format(calendar.getTime()));
    }

    private void saveCaseToDatabase() {
        String itemType = itemTypeEditText.getText().toString().trim();
        String color = colorEditText.getText().toString().trim();
        String brand = brandEditText.getText().toString().trim();
        String ownerName = ownerNameEditText.getText().toString().trim();
        String lossDescription = lossDescriptionEditText.getText().toString().trim();
        Date tripDate = calendar.getTime();
        String tripTime = tripTimeEditText.getText().toString().trim();
        String origin = originEditText.getText().toString().trim();
        String destination = destinationEditText.getText().toString().trim();
        String lineNumber = lineNumberEditText.getText().toString().trim();

        if (TextUtils.isEmpty(itemType) || TextUtils.isEmpty(color) || TextUtils.isEmpty(brand) ||
                TextUtils.isEmpty(ownerName) || TextUtils.isEmpty(lossDescription) || TextUtils.isEmpty(tripTime) ||
                TextUtils.isEmpty(origin) || TextUtils.isEmpty(destination) || TextUtils.isEmpty(lineNumber)) {
            Toast.makeText(NewCaseActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Retrieve user details from the database
        String fullName = dbHelper.getUserFullName(username);
        String idCard = dbHelper.getUserIdCard(username);
        String phoneNumber = dbHelper.getUserPhoneNumber(username);
        String email = dbHelper.getUserEmail(username);
        String city = dbHelper.getUserCity(username);

        Request request = new Request(username, fullName, idCard, phoneNumber, email, city,
                itemType, color, brand, ownerName, lossDescription,
                tripDate, tripTime, origin, destination, lineNumber);

        if (dbHelper.addRequest(request)) {
            Toast.makeText(NewCaseActivity.this, "Case saved successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(NewCaseActivity.this, "Failed to save case", Toast.LENGTH_SHORT).show();
        }
    }
}