package com.example.lostfound;

// ייבוא עבור Firebase Firestore ו-Tasks
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks; // עבור Tasks.forException
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions; // עבור עדכון חלקי/מיזוג נתונים

// ייבוא אנדרואיד בסיסי שרלוונטי (Context, Log)
import android.content.Context;
import android.util.Log; // עבור רישום (logging)

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects; // עבור השוואת אובייקטים (אם יש צורך)

// הוסרו ייבוא של SQLite:
// import android.content.ContentValues;
// import android.database.Cursor;
// import android.database.sqlite.SQLiteDatabase;
// import android.database.sqlite.SQLiteOpenHelper;

/**
 * This class now handles all data operations using Firebase Firestore.
 * It replaces the previous SQLiteOpenHelper functionality.
 * All methods now return a Task object, reflecting the asynchronous nature of Firebase.
 * Consumers of this class must handle these Tasks (e.g., using addOnSuccessListener, addOnFailureListener).
 */
public class DatabaseHelper { // אינו יורש מ-SQLiteOpenHelper יותר

    private static final String TAG = "DatabaseHelperFirebase"; // שינוי ה-TAG עבור בהירות ב-Logcat
    private FirebaseFirestore db; // מופע של Firestore

    // שמות הקולקציות (מקבילים לטבלאות ב-SQLite)
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_REQUESTS = "requests";

    // קבועי שמות העמודות הישנים של SQLite אינם בשימוש ישיר עבור Firestore,
    // מכיוון ש-Firestore עובד עם שדות בתוך מסמכים.
    // שמות השדות במסמכי Firestore נובעים משמות המפתחות ב-Map שאתה שולח,
    // או משמות ה-Getters/Setters במודל ה-POJO (Request/User) שלך.
    // השארתי אותם כהערה כדי להראות התאמה אם תצטרך.

    // Table names (Firestore collection names) - אלו רלוונטיים
    // private static final String TABLE_USERS = "users";
    // private static final String TABLE_REQUESTS = "requests";

    // Common column names (Firestore field names) - לא רלוונטיים כקבועים ישירים
    // private static final String KEY_USERNAME = "username";

    // Users table column names (Firestore field names) - לא רלוונטיים כקבועים ישירים
    // private static final String KEY_PASSWORD = "password";
    // private static final String KEY_ROLE = "role";
    // private static final String KEY_FULL_NAME = "fullName";
    // private static final String KEY_ID_CARD = "idCard";
    // private static final String KEY_PHONE_NUMBER = "phoneNumber";
    // private static final String KEY_EMAIL = "email";
    // private static final String KEY_CITY = "city";

    // Requests table column names (Firestore field names) - לא רלוונטיים כקבועים ישירים
    // private static final String KEY_REQUEST_ID = "_id"; // זה יהיה Firestore document ID
    // private static final String KEY_ITEM_TYPE = "itemType";
    // private static final String KEY_COLOR = "color";
    // private static final String KEY_BRAND = "brand";
    // private static final String KEY_OWNER_NAME = "ownerName";
    // private static final String KEY_LOSS_DESCRIPTION = "lossDescription";
    // private static final String KEY_TRIP_DATE = "tripDate";
    // private static final String KEY_TRIP_TIME = "tripTime";
    // private static final String KEY_ORIGIN = "origin";
    // private static final String KEY_DESTINATION = "destination";
    // private static final String KEY_LINE_NUMBER = "lineNumber";
    // private static final String KEY_STATUS = "status";
    // private static final String KEY_SYSTEM_COMMENTS = "systemComments";
    // private static final String KEY_CREATION_TIMESTAMP = "creationTimestamp";
    // private static final String KEY_LOCATION_ADDRESS = "locationAddress";
    // private static final String KEY_LATITUDE = "latitude";
    // private static final String KEY_LONGITUDE = "longitude";

    // Table Create Statements - הוסרו, לא רלוונטיים ל-Firestore
    // private static final String CREATE_TABLE_USERS = "...";
    // private static final String CREATE_TABLE_REQUESTS = "...";

    // Constructor - אינו יורש מ-SQLiteOpenHelper, ולכן קריאה ל-super הוסרה.
    // ה-Context אינו נחוץ לאתחול בסיסי של FirebaseFirestore.getInstance()
    public DatabaseHelper(Context context) {
        // אין צורך בקריאה ל-super(context, DATABASE_NAME, null, DATABASE_VERSION);
        // יצירת מופע של Firestore מתבצעת באופן עצלני באמצעות getFirestoreInstance()
    }

    // Lazy initialization for FirebaseFirestore instance
    private FirebaseFirestore getFirestoreInstance() {
        if (db == null) {
            db = FirebaseFirestore.getInstance();
            Log.d(TAG, "FirebaseFirestore instance initialized.");
        }
        return db;
    }




    // --- User related methods ---
    // כל המתודות המקוריות נשמרו והותאמו ל-Firebase Firestore.

    /**
     * Adds a new user to Firebase Firestore.
     * The username is used as the document ID for quick lookup.
     * @param username The username for the new user (will be document ID).
     * @param password The password for the new user.
     * @param role The role of the new user.
     * @return A Task<Boolean> indicating success (true) or failure (false) of the operation.
     */
    public Task<Boolean> addUser(String username, String password, String role) {
        Map<String, Object> user = new HashMap<>();
        user.put("password", password);
        user.put("role", role);
        user.put("fullName", ""); // Initialize as empty string as per SQLite schema
        user.put("idCard", "");
        user.put("phoneNumber", "");
        user.put("email", "");
        user.put("city", "");

        Log.d(TAG, "Attempting to add user: " + username);
        // Use document(username).set() to create or overwrite a document with a specific ID
        return getFirestoreInstance().collection(COLLECTION_USERS).document(username).set(user)
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User " + username + " added successfully!");
                        return true;
                    } else {
                        Log.w(TAG, "Error adding user " + username, task.getException());
                        return false;
                    }
                });
    }

    /**
     * Checks if a user exists with the given username and password.
     * Note: For production, use Firebase Authentication for secure user login.
     * This method fetches the document and compares the password locally.
     * @param username The username to check.
     * @param password The password to verify.
     * @return A Task<Boolean> indicating if the user exists and credentials match.
     */
    public Task<Boolean> checkUser(String username, String password) {
        Log.d(TAG, "Checking user: " + username);
        return getFirestoreInstance().collection(COLLECTION_USERS).document(username).get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String storedPassword = document.getString("password");
                            return password.equals(storedPassword);
                        } else {
                            Log.d(TAG, "User " + username + " not found.");
                            return false; // User does not exist
                        }
                    } else {
                        Log.e(TAG, "Error checking user document for " + username, task.getException());
                        return false;
                    }
                });
    }

    /**
     * Checks if a username already exists in Firestore.
     * @param username The username to check for existence.
     * @return A Task<Boolean> indicating if the username exists.
     */
    public Task<Boolean> checkUsername(String username) {
        Log.d(TAG, "Checking if username exists: " + username);
        return getFirestoreInstance().collection(COLLECTION_USERS).document(username).get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        return document.exists();
                    } else {
                        Log.e(TAG, "Error checking username existence for " + username, task.getException());
                        return false;
                    }
                });
    }

    /**
     * Retrieves the role of a specific user from Firestore.
     * @param username The username of the user.
     * @return A Task<String> containing the user's role, or null if not found/error.
     */
    public Task<String> getUserRole(String username) {
        Log.d(TAG, "Getting role for user: " + username);
        return getFirestoreInstance().collection(COLLECTION_USERS).document(username).get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            return document.getString("role");
                        }
                    } else {
                        Log.e(TAG, "Error getting user role for " + username, task.getException());
                    }
                    return null;
                });
    }

    /**
     * Updates user details in Firestore.
     * @param username The username of the user to update.
     * @param fullName User's full name.
     * @param idCard User's ID card number.
     * @param phoneNumber User's phone number.
     * @param email User's email address.
     * @param city User's city.
     * @return A Task<Boolean> indicating success (true) or failure (false).
     */
    public Task<Boolean> updateUserDetails(String username, String fullName, String idCard, String phoneNumber, String email, String city) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("idCard", idCard);
        updates.put("phoneNumber", phoneNumber);
        updates.put("email", email);
        updates.put("city", city);

        Log.d(TAG, "Updating details for user: " + username);
        // Use SetOptions.merge() to update fields without overwriting the entire document
        return getFirestoreInstance().collection(COLLECTION_USERS).document(username).set(updates, SetOptions.merge())
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User details for " + username + " updated successfully!");
                        return true;
                    } else {
                        Log.w(TAG, "Error updating user details for " + username, task.getException());
                        return false;
                    }
                });
    }

    /**
     * Retrieves the full name of a specific user.
     * @param username The username.
     * @return A Task<String> with the full name or null.
     */
    public Task<String> getUserFullName(String username) {
        return getFirestoreInstance().collection(COLLECTION_USERS).document(username).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        return task.getResult().getString("fullName");
                    }
                    return null;
                });
    }

    /**
     * Retrieves the ID card of a specific user.
     * @param username The username.
     * @return A Task<String> with the ID card or null.
     */
    public Task<String> getUserIdCard(String username) {
        return getFirestoreInstance().collection(COLLECTION_USERS).document(username).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        return task.getResult().getString("idCard");
                    }
                    return null;
                });
    }

    /**
     * Retrieves the phone number of a specific user.
     * @param username The username.
     * @return A Task<String> with the phone number or null.
     */
    public Task<String> getUserPhoneNumber(String username) {
        return getFirestoreInstance().collection(COLLECTION_USERS).document(username).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        return task.getResult().getString("phoneNumber");
                    }
                    return null;
                });
    }

    /**
     * Retrieves the email of a specific user.
     * @param username The username.
     * @return A Task<String> with the email or null.
     */
    public Task<String> getUserEmail(String username) {
        return getFirestoreInstance().collection(COLLECTION_USERS).document(username).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        return task.getResult().getString("email");
                    }
                    return null;
                });
    }

    /**
     * Retrieves the city of a specific user.
     * @param username The username.
     * @return A Task<String> with the city or null.
     */
    public Task<String> getUserCity(String username) {
        return getFirestoreInstance().collection(COLLECTION_USERS).document(username).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        return task.getResult().getString("city");
                    }
                    return null;
                });
    }

    /**
     * Fetches all usernames and their roles from Firestore.
     * @return A Task<List<String[]>> containing a list of username-role pairs.
     */
    public Task<List<String[]>> getAllUsernamesAndRoles() {
        Log.d(TAG, "Getting all usernames and roles.");
        return getFirestoreInstance().collection(COLLECTION_USERS).get()
                .continueWith(task -> {
                    List<String[]> userList = new ArrayList<>();
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String username = document.getId(); // Document ID is the username
                            String role = document.getString("role");
                            userList.add(new String[]{username, role});
                        }
                    } else {
                        Log.e(TAG, "Error getting usernames and roles", task.getException());
                    }
                    return userList;
                });
    }


    // --- Request related methods ---

    /**
     * Adds a new request to Firebase Firestore.
     * Firebase generates a unique document ID automatically.
     * The Request object's 'firestoreId' field will be updated upon successful addition.
     * @param request The Request object to add.
     * @return A Task<Long> representing the Firestore operation.
     * The Long value will be the creation timestamp (Firebase's approximate equivalent to SQLite's row ID).
     * Returns -1 on failure.
     */
    public Task<Long> addRequest(Request request) {
        // Firebase can directly convert a POJO (Plain Old Java Object) to a document
        // if it has a public no-argument constructor and getters/setters for all fields.
        // The 'id' field from SQLite will be ignored by Firestore, as Firestore generates its own ID.
        // We will explicitly set 'firestoreId' after document creation.

        Log.d(TAG, "Adding new request for item type: " + request.getItemType());
        // Use the request POJO directly, Firestore will map its public getters
        return getFirestoreInstance().collection(COLLECTION_REQUESTS).add(request)
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        DocumentReference documentReference = task.getResult();
                        request.setFirestoreId(documentReference.getId()); // Update Request object with Firestore ID
                        Log.d(TAG, "Request added with ID: " + documentReference.getId());
                        // Return the creationTimestamp as an approximate equivalent to SQLite's row ID
                        return request.getCreationTimestamp();
                    } else {
                        Log.w(TAG, "Error adding request", task.getException());
                        return -1L; // Indicate failure, similar to SQLite insert returning -1
                    }
                });
    }

    /**
     * Updates an existing request in Firebase Firestore.
     * The Request object must contain a valid Firestore document ID (getFirestoreId()).
     * @param request The Request object with updated data and a valid Firestore ID.
     * @return A Task<Boolean> indicating success (true) or failure (false).
     */
    public Task<Boolean> updateRequest(Request request) {
        // Ensure the Request object has a Firestore ID for update operations
        if (request.getFirestoreId() == null || request.getFirestoreId().isEmpty()) {
            String errorMessage = "Request object must have a Firestore ID (getFirestoreId()) for update.";
            Log.e(TAG, errorMessage);
            return Tasks.forException(new IllegalArgumentException(errorMessage));
        }

        Log.d(TAG, "Updating request with ID: " + request.getFirestoreId());
        // Use the Firestore ID to target the specific document for update
        // We use .set(request, SetOptions.merge()) to update the document.
        // This will update fields based on your Request POJO.
        return getFirestoreInstance().collection(COLLECTION_REQUESTS).document(request.getFirestoreId()).set(request, SetOptions.merge())
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Request updated successfully! ID: " + request.getFirestoreId());
                        return true;
                    } else {
                        Log.w(TAG, "Error updating request with ID: " + request.getFirestoreId(), task.getException());
                        return false;
                    }
                });
    }

    /**
     * Deletes a request from Firebase Firestore using its Firestore document ID.
     * @param firestoreRequestId The Firestore document ID of the request to delete.
     * @return A Task<Boolean> indicating success (true) or failure (false).
     */
    public Task<Boolean> deleteRequest(String firestoreRequestId) { // Changed int requestId to String firestoreRequestId
        Log.d(TAG, "Deleting request with ID: " + firestoreRequestId);
        return getFirestoreInstance().collection(COLLECTION_REQUESTS).document(firestoreRequestId).delete()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Request deleted successfully! ID: " + firestoreRequestId);
                        return true;
                    } else {
                        Log.w(TAG, "Error deleting request with ID: " + firestoreRequestId, task.getException());
                        return false;
                    }
                });
    }

    /**
     * Retrieves a request by its Firebase Firestore ID.
     * @param firestoreRequestId The Firestore document ID of the request.
     * @return A Task<Request> containing the Request object or null if not found/error.
     */
    public Task<Request> getRequestById(String firestoreRequestId) { // Changed int requestId to String firestoreRequestId
        Log.d(TAG, "Getting request by ID: " + firestoreRequestId);
        return getFirestoreInstance().collection(COLLECTION_REQUESTS).document(firestoreRequestId).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot document = task.getResult();
                        // Firebase can automatically map document to object if getters/setters and empty constructor exist
                        Request request = document.toObject(Request.class);
                        if (request != null) {
                            request.setFirestoreId(document.getId()); // Manually set Firestore ID from document.getId()
                            // The old 'id' field (int) will likely remain its default value or -1
                            // as Firestore doesn't use it.
                        }
                        return request;
                    } else if (!task.getResult().exists()){
                        Log.d(TAG, "No such document with ID: " + firestoreRequestId);
                    } else {
                        Log.e(TAG, "Error getting request by ID: " + firestoreRequestId, task.getException());
                    }
                    return null;
                });
    }

    /**
     * Retrieves all requests associated with a specific username.
     * @param username The username to filter requests by.
     * @return A Task<List<Request>> containing a list of Request objects.
     */
    public Task<List<Request>> getRequestsByUsername(String username) {
        Log.d(TAG, "Getting requests for username: " + username);
        return getFirestoreInstance().collection(COLLECTION_REQUESTS)
                .whereEqualTo("username", username) // Query by username field
                .get()
                .continueWith(task -> {
                    List<Request> requests = new ArrayList<>();
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Request request = document.toObject(Request.class);
                            if (request != null) {
                                request.setFirestoreId(document.getId()); // Manually set Firestore ID
                                requests.add(request);
                            }
                        }
                    } else {
                        Log.e(TAG, "Error getting requests by username: " + username, task.getException());
                    }
                    return requests;
                });
    }

    /**
     * Retrieves all requests from the database.
     * @return A Task<List<Request>> containing a list of all Request objects.
     */
    public Task<List<Request>> getAllRequests() {
        Log.d(TAG, "Getting all requests.");
        return getFirestoreInstance().collection(COLLECTION_REQUESTS).get()
                .continueWith(task -> {
                    List<Request> requests = new ArrayList<>();
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Request request = document.toObject(Request.class);
                            if (request != null) {
                                request.setFirestoreId(document.getId()); // Manually set Firestore ID
                                requests.add(request);
                            }
                        }
                    } else {
                        Log.e(TAG, "Error getting all requests", task.getException());
                    }
                    return requests;
                });
    }
}