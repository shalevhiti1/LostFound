package com.example.lostfound;

import android.content.Intent;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * מסך ניהול כל הפניות במערכת. מאפשר חיפוש, סינון וצפייה בפרטי פנייה.
 */
public class AllCasesActivity extends AppCompatActivity {

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

        loggedInUsername = getIntent().getStringExtra("username");

        ArrayAdapter<CharSequence> statusFilterAdapter = ArrayAdapter.createFromResource(this,
                R.array.all_case_statuses_filter, android.R.layout.simple_spinner_item);
        statusFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusFilterSpinner.setAdapter(statusFilterAdapter);

        int spinnerPosition = statusFilterAdapter.getPosition(currentFilterStatus);
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
                intent.putExtra("REQUEST_ID", selectedRequest.getId());
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
        executorService.shutdown();
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
        }
    }

    /**
     * טוען את כל הפניות, מסנן לפי סטטוס וחיפוש ומעדכן את הרשימה.
     */
    private void loadAllCases() {
        executorService.execute(() -> {
            List<Request> allRequests = dbHelper.getAllRequests();
            List<Request> filteredRequests = new ArrayList<>();

            String normalizedFilterStatus = normalizeStatus(currentFilterStatus);

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
                    String queryLower = currentSearchQuery.toLowerCase();
                    if (!request.getItemType().toLowerCase().contains(queryLower) &&
                            !request.getOwnerName().toLowerCase().contains(queryLower) &&
                            !request.getLossDescription().toLowerCase().contains(queryLower)) {
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
                }
            });
        });
    }

    /**
     * מנרמל טקסט סטטוס לסינון עקבי.
     */
    private String normalizeStatus(String status) {
        if (status == null) return "";
        status = status.trim().toLowerCase();
        if (status.equals("all cases") || status.equals("כל הפניות")) return "all_cases";
        if (status.equals("found") || status.equals("אבידה נמצאה")) return "found";
        if (status.equals("not found") || status.equals("לא נמצאה")) return "not_found";
        if (status.equals("rejected") || status.equals("נדחתה")) return "rejected";
        if (status.equals("pending") || status.equals("ממתינה לטיפול")) return "pending";
        return status.replace(" ", "_");
    }
}