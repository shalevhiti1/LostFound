package com.example.lostfound;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegistrationActivity extends AppCompatActivity {

    private EditText usernameEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        dbHelper = new DatabaseHelper(this);

        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();
                String confirmPassword = confirmPasswordEditText.getText().toString().trim();

                if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
                    Toast.makeText(RegistrationActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    Toast.makeText(RegistrationActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 6 || !password.matches(".*[a-zA-Z].*")) {
                    Toast.makeText(RegistrationActivity.this, "Password must be at least 6 characters and contain at least one letter", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (dbHelper.checkUsername(username)) {
                    Toast.makeText(RegistrationActivity.this, "Username already exists", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Determine the role based on the username
                String role = "user"; // Default role
                if (username.equals("admin")) {
                    role = "admin";
                } else if (username.equals("South") || username.equals("North") || username.equals("Center") || username.equals("Jerusalem")) {
                    role = "representative";
                }

                if (dbHelper.addUser(username, password, role)) {
                    Toast.makeText(RegistrationActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegistrationActivity.this, UserDetailsActivity.class);
                    intent.putExtra("username", username);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(RegistrationActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}