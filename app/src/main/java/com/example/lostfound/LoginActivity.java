package com.example.lostfound;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button registerButton; // Add this line
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton); // Add this line
        dbHelper = new DatabaseHelper(this);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                if (dbHelper.checkUser(username, password)) {
                    String role = dbHelper.getUserRole(username);
                    if (role.equals("user")) {
                        Intent intent = new Intent(LoginActivity.this, UserActivity.class);
                        intent.putExtra("username", username); // Pass the username
                        startActivity(intent);
                    } else if (role.equals("admin")) {
                        Intent intent = new Intent(LoginActivity.this, AdminActivity.class);
                        intent.putExtra("username", username); // Pass the username
                        startActivity(intent);
                    } else if (role.equals("representative")) {
                        Intent intent = new Intent(LoginActivity.this, RepresentativeActivity.class);
                        intent.putExtra("username", username); // Pass the username
                        startActivity(intent);
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                }
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() { // Add this block
            @Override
            public void onClick(View v) {
                // Start the RegistrationActivity
                Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                startActivity(intent);
            }
        });
    }
}