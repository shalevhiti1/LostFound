package com.example.lostfound;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

// ייבוא לפונקציה הסטטית שניצור בשלב 3
import static com.example.lostfound.NotificationUtils.showSimpleNotification;


public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button registerButton;
    private DatabaseHelper dbHelper;

    public static final String DEFAULT_CHANNEL_ID = "default_app_channel"; // מזהה ערוץ

    // Launcher לבקשת הרשאה
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show();
                    // ההרשאה ניתנה, אפשר להמשיך
                } else {
                    Toast.makeText(this, "Notification permission denied. Some features might not work.", Toast.LENGTH_LONG).show();
                    // ההרשאה נדחתה, שקול להסביר למשתמש את ההשלכות
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        dbHelper = new DatabaseHelper(this);

        createNotificationChannel(); // יצירת ערוץ התראות
        requestNotificationPermission(); // בקשת הרשאה אם צריך

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                if (dbHelper.checkUser(username, password)) {
                    String role = dbHelper.getUserRole(username);
                    if (role.equals("user")) {
                        Intent intent = new Intent(LoginActivity.this, UserActivity.class);
                        intent.putExtra("username", username);
                        startActivity(intent);
                    } else if (role.equals("admin")) {
                        Intent intent = new Intent(LoginActivity.this, AdminActivity.class);
                        intent.putExtra("username", username);
                        startActivity(intent);
                    } else if (role.equals("representative")) {
                        Intent intent = new Intent(LoginActivity.this, RepresentativeActivity.class);
                        intent.putExtra("username", username);
                        startActivity(intent);
                    }
                } else {
                    // הצגת התראה פשוטה במקרה של שגיאת התחברות
                    // כאן אנו מניחים שההרשאה כבר טופלה ב-onCreate
                    showSimpleNotification(
                            LoginActivity.this,
                            "Login Failed",
                            "Invalid username or password.",
                            101 // ID ייחודי להתראה זו
                    );
                    // Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show(); // אפשר להשאיר או להסיר
                }
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                startActivity(intent);
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // השתמש במשאבי מחרוזות עבור שם ותיאור הערוץ
            CharSequence name = getString(R.string.default_channel_name); // לדוגמה: "App Notifications"
            String description = getString(R.string.default_channel_description); // לדוגמה: "General notifications for the app"
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(DEFAULT_CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // אנדרואיד 13 ומעלה
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                // אם ההרשאה לא ניתנה, בקש אותה
                // אפשר להוסיף כאן לוגיקה של shouldShowRequestPermissionRationale אם רוצים
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
            // אם ההרשאה כבר ניתנה, לא עושים כלום
        }
        // עבור גרסאות ישנות יותר, אין צורך בבקשה מפורשת בזמן ריצה
    }
}