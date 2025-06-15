package com.example.lostfound;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * מסך ניהול כל הפניות במערכת. מאפשר חיפוש, סינון וצפייה בפרטי פנייה.
 * קודכן לעבודה עם Firebase Firestore.
 */
public class AllCasesActivity extends AppCompatActivity {

    private static final String TAG = "AllCasesActivity";

    private ListView allCasesListView;
    private TextView noCasesTextView;
    private DatabaseHelper dbHelper;
    private ExecutorService executorService;
    private SearchView caseSearchView;
    private Spinner statusFilterSpinner;

    private TextToSpeech tts;
    private boolean isTtsReady = false;

    private String currentSearchQuery = "";
    private String currentFilterStatus = "All Cases";
    private String loggedInUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_cases);

        dbHelper = new DatabaseHelper(this);
        executorService = Executors.newSingleThreadExecutor();

        allCasesListView = findViewById(R.id.allCasesListView);
        noCasesTextView = findViewById(R.id.noCasesTextView);
        caseSearchView = findViewById(R.id.caseSearchView);
        statusFilterSpinner = findViewById(R.id.statusFilterSpinner);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
                if (!isTtsReady) {
                    Log.e(TAG, "TTS language not supported or missing data.");
                }
            } else {
                isTtsReady = false;
                Log.e(TAG, "TTS initialization failed.");
            }
        });

        loggedInUsername = getIntent().getStringExtra("username");
        if (loggedInUsername == null) {
            Log.w(TAG, "Logged in username not found in intent.");
        }

        ArrayAdapter<CharSequence> statusFilterAdapter = ArrayAdapter.createFromResource(this,
                R.array.all_case_statuses_filter, android.R.layout.simple_spinner_item);
        statusFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusFilterSpinner.setAdapter(statusFilterAdapter);

        // Set initial spinner selection.
        int spinnerPosition = statusFilterAdapter.getPosition(currentFilterStatus);
        // Fallback for "All Cases" if it's not found directly (e.g., if it's "כל הפניות" in the array)
        if (spinnerPosition == -1) {
            spinnerPosition = statusFilterAdapter.getPosition("כל הפניות"); // Direct string check
            if (spinnerPosition == -1) {
                spinnerPosition = 0; // Default to first item if still not found
            }
        }
        statusFilterSpinner.setSelection(spinnerPosition);

        caseSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query;
                loadAllCases();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                loadAllCases();
                return false;
            }
        });

        statusFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFilterStatus = parent.getItemAtPosition(position).toString();
                loadAllCases();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        loadAllCases();

        allCasesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Request selectedRequest = (Request) parent.getItemAtPosition(position);
                Intent intent = new Intent(AllCasesActivity.this, CaseDetailsActivity.class);
                // CHANGED: Pass firestoreId (String) instead of old int ID
                // IMPORTANT: CaseDetailsActivity must also be updated to expect String REQUEST_ID
                intent.putExtra("REQUEST_ID", selectedRequest.getFirestoreId());
                intent.putExtra("username", loggedInUsername);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllCases();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    private void speakIfTtsReady(String text) {
        if (tts != null && isTtsReady) {
            if (tts.isSpeaking()) {
                tts.stop();
            }
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            Log.w(TAG, "TTS not ready or initialized, cannot speak: " + text);
        }
    }

    /**
     * טוען את כל הפניות מ-Firebase Firestore, מסנן לפי סטטוס וחיפוש ומעדכן את הרשימה.
     */
    private void loadAllCases() {
        Log.d(TAG, "Loading all cases with search query: '" + currentSearchQuery + "', filter status: '" + currentFilterStatus + "'");

        dbHelper.getAllRequests()
                .addOnSuccessListener(allRequests -> {
                    List<Request> filteredRequests = new ArrayList<>();
                    String normalizedFilterStatus = normalizeStatus(currentFilterStatus);
                    String queryLower = currentSearchQuery.toLowerCase(Locale.getDefault());

                    for (Request request : allRequests) {
                        String reqStatusNorm = normalizeStatus(request.getStatus());

                        boolean matchesStatus = false;
                        if (normalizedFilterStatus.equals("all_cases")) {
                            matchesStatus = true;
                        } else if (!reqStatusNorm.isEmpty() && reqStatusNorm.equals(normalizedFilterStatus)) {
                            matchesStatus = true;
                        }

                        boolean matchesSearch = true;
                        if (!currentSearchQuery.isEmpty()) {
                            if (!(request.getItemType() != null && request.getItemType().toLowerCase(Locale.getDefault()).contains(queryLower)) &&
                                    !(request.getOwnerName() != null && request.getOwnerName().toLowerCase(Locale.getDefault()).contains(queryLower)) &&
                                    !(request.getLossDescription() != null && request.getLossDescription().toLowerCase(Locale.getDefault()).contains(queryLower))) {
                                matchesSearch = false;
                            }
                        }

                        if (matchesStatus && matchesSearch) {
                            filteredRequests.add(request);
                        }
                    }

                    runOnUiThread(() -> {
                        if (filteredRequests.isEmpty()) {
                            allCasesListView.setVisibility(View.GONE);
                            noCasesTextView.setVisibility(View.VISIBLE);
                            String toastMsg = "No cases found matching your criteria.";
                            noCasesTextView.setText(toastMsg);
                            Toast.makeText(AllCasesActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                            speakIfTtsReady(toastMsg);
                        } else {
                            allCasesListView.setVisibility(View.VISIBLE);
                            noCasesTextView.setVisibility(View.GONE);

                            RequestAdapter adapter = new RequestAdapter(
                                    AllCasesActivity.this,
                                    filteredRequests
                            );
                            allCasesListView.setAdapter(adapter);
                            Log.d(TAG, "Displaying " + filteredRequests.size() + " filtered cases.");
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        Toast.makeText(AllCasesActivity.this, "Error loading cases: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error loading all cases from Firebase", e);
                        allCasesListView.setVisibility(View.GONE);
                        noCasesTextView.setVisibility(View.VISIBLE);
                        noCasesTextView.setText("Failed to load cases. Please check your network connection.");
                        speakIfTtsReady("Failed to load cases.");
                    });
                });
    }

    /**
     * מנרמל טקסט סטטוס לסינון עקבי, תוך שימוש במחרוזות מוגדרות ב-ENUM או בממשק המשתמש.
     */
    private String normalizeStatus(String status) {
        if (status == null) return "";
        status = status.trim().toLowerCase(Locale.getDefault());
        // Original logic from your provided code, with added explicit checks for common Hebrew values
        if (status.equals("all cases") || status.equals("כל הפניות")) return "all_cases";
        if (status.equals("found") || status.equals("אבידה נמצאה")) return "found";
        if (status.equals("not found") || status.equals("אבידה לא נמצאה")) return "not_found"; // Corrected "לא נמצאה" to "אבידה לא נמצאה" for consistency
        if (status.equals("rejected") || status.equals("פנייה נדחתה")) return "rejected";
        if (status.equals("in progress") || status.equals("פנייה בטיפול")) return "in_progress"; // Added "in progress" and its Hebrew equivalent
        return status.replace(" ", "_"); // Default for other statuses
    }
}