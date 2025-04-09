package com.example.lostfound;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class UserDetailsActivity extends AppCompatActivity {

    private EditText fullNameEditText, idCardEditText, phoneNumberEditText, emailEditText, cityEditText;
    private Button saveButton;
    private DatabaseHelper dbHelper;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        dbHelper = new DatabaseHelper(this);

        // Get the username from the intent
        username = getIntent().getStringExtra("username");

        fullNameEditText = findViewById(R.id.fullNameEditText);
        idCardEditText = findViewById(R.id.idCardEditText);
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        emailEditText = findViewById(R.id.emailEditText);
        cityEditText = findViewById(R.id.cityEditText);
        saveButton = findViewById(R.id.saveButton);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fullName = fullNameEditText.getText().toString().trim();
                String idCard = idCardEditText.getText().toString().trim();
                String phoneNumber = phoneNumberEditText.getText().toString().trim();
                String email = emailEditText.getText().toString().trim();
                String city = cityEditText.getText().toString().trim();

                if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(idCard) || TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(email) || TextUtils.isEmpty(city)) {
                    Toast.makeText(UserDetailsActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (dbHelper.addUserDetails(username, fullName, idCard, phoneNumber, email, city)) {
                    Toast.makeText(UserDetailsActivity.this, "User details saved", Toast.LENGTH_SHORT).show();
                    // Navigate to the appropriate screen based on the user's role
                    String role = dbHelper.getUserRole(username);
                    Intent intent;
                    intent = new Intent(UserDetailsActivity.this, LoginActivity.class);

                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(UserDetailsActivity.this, "Failed to save user details", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}