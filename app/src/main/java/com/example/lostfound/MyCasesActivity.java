package com.example.lostfound;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

/**
 * מסך הצגת כל הפניות של המשתמש הנוכחי. כולל TTS, טעינה ברקע וניווט לפרטי פנייה.
 */
public class MyCasesActivity extends AppCompatActivity {

    private ListView myCasesListView;
    private TextView noCasesTextView;
    private DatabaseHelper dbHelper;
    private String username;
    private ExecutorService executorService;

    private TextToSpeech tts;
    private boolean isTtsReady = false;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_cases);

        myCasesListView = findViewById(R.id.myCasesListView);
        noCasesTextView = findViewById(R.id.noCasesTextView);

        dbHelper = new DatabaseHelper(this);
        executorService = Executors.newSingleThreadExecutor();
        username = getIntent().getStringExtra("username");

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                    isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
                } else {
                    isTtsReady = false;
                }
            }
        });

        if (username == null) {
            Log.e("MyCasesActivity", "Username is null!");
            String toastMsg = "Error: Username not found.";
            noCasesTextView.setText(toastMsg);
            noCasesTextView.setVisibility(View.VISIBLE);
            Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
            speakIfTtsReady(toastMsg);
            return;
        }

        loadMyCases();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMyCases();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    private void speakIfTtsReady(String text) {
        if (tts != null && isTtsReady) {
            if (tts.isSpeaking()) {
                tts.stop();
            }
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @SuppressLint("SetTextI18n")
    private void loadMyCases() {
        Log.d("MyCasesActivity", "loadMyCases called for username: " + username);

        executorService.execute(() -> {
            final List<Request> myRequests = dbHelper.getRequestsByUsername(username);

            runOnUiThread(() -> {
                if (myRequests == null) {
                    Log.e("MyCasesActivity", "getRequestsByUsername returned null!");
                    String toastMsg = "Error loading requests.";
                    noCasesTextView.setText(toastMsg);
                    noCasesTextView.setVisibility(View.VISIBLE);
                    myCasesListView.setVisibility(View.GONE);
                    Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
                    speakIfTtsReady(toastMsg);
                    return;
                }

                if (myRequests.isEmpty()) {
                    Log.d("MyCasesActivity", "No requests found for username: " + username);
                    String toastMsg = "You have not opened any requests with us yet.";
                    noCasesTextView.setText(toastMsg);
                    noCasesTextView.setVisibility(View.VISIBLE);
                    myCasesListView.setVisibility(View.GONE);
                    Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
                    speakIfTtsReady(toastMsg);
                } else {
                    Log.d("MyCasesActivity", "Found " + myRequests.size() + " requests for username: " + username);
                    noCasesTextView.setVisibility(View.GONE);
                    myCasesListView.setVisibility(View.VISIBLE);

                    RequestAdapter adapter = new RequestAdapter(
                            MyCasesActivity.this,
                            myRequests
                    );
                    myCasesListView.setAdapter(adapter);

                    myCasesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            Request selectedRequest = (Request) parent.getItemAtPosition(position);

                            Intent intent = new Intent(MyCasesActivity.this, CaseDetailsActivity.class);
                            intent.putExtra("REQUEST_ID", selectedRequest.getId());
                            intent.putExtra("username", username);
                            startActivity(intent);
                        }
                    });
                }
            });
        });
    }
}