package com.example.lostfound;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "LostFound.db";
    private static final int DATABASE_VERSION = 8; // Increased version to trigger upgrade

    // Table names
    private static final String TABLE_USERS = "users";
    private static final String TABLE_REQUESTS = "requests";

    // Common column names
    private static final String KEY_USERNAME = "username";

    // Users table column names
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_ROLE = "role";
    private static final String KEY_FULL_NAME = "fullName";
    private static final String KEY_ID_CARD = "idCard";
    private static final String KEY_PHONE_NUMBER = "phoneNumber";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_CITY = "city";

    // Requests table column names
    private static final String KEY_REQUEST_ID = "_id";
    private static final String KEY_ITEM_TYPE = "itemType";
    private static final String KEY_COLOR = "color";
    private static final String KEY_BRAND = "brand";
    private static final String KEY_OWNER_NAME = "ownerName";
    private static final String KEY_LOSS_DESCRIPTION = "lossDescription";
    private static final String KEY_TRIP_DATE = "tripDate";
    private static final String KEY_TRIP_TIME = "tripTime";
    private static final String KEY_ORIGIN = "origin";
    private static final String KEY_DESTINATION = "destination";
    private static final String KEY_LINE_NUMBER = "lineNumber";
    private static final String KEY_STATUS = "status";
    private static final String KEY_SYSTEM_COMMENTS = "systemComments";
    private static final String KEY_CREATION_TIMESTAMP = "creationTimestamp";
    private static final String KEY_LOCATION_ADDRESS = "locationAddress";
    // NEW: Columns for coordinates
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";

    // Table Create Statements
    private static final String CREATE_TABLE_USERS = "CREATE TABLE " + TABLE_USERS + "("
            + KEY_USERNAME + " TEXT PRIMARY KEY,"
            + KEY_PASSWORD + " TEXT,"
            + KEY_ROLE + " TEXT,"
            + KEY_FULL_NAME + " TEXT,"
            + KEY_ID_CARD + " TEXT,"
            + KEY_PHONE_NUMBER + " TEXT,"
            + KEY_EMAIL + " TEXT,"
            + KEY_CITY + " TEXT" + ")";

    private static final String CREATE_TABLE_REQUESTS = "CREATE TABLE " + TABLE_REQUESTS + "("
            + KEY_REQUEST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_USERNAME + " TEXT,"
            + KEY_FULL_NAME + " TEXT,"
            + KEY_ID_CARD + " TEXT,"
            + KEY_PHONE_NUMBER + " TEXT,"
            + KEY_EMAIL + " TEXT,"
            + KEY_CITY + " TEXT,"
            + KEY_ITEM_TYPE + " TEXT,"
            + KEY_COLOR + " TEXT,"
            + KEY_BRAND + " TEXT,"
            + KEY_OWNER_NAME + " TEXT,"
            + KEY_LOSS_DESCRIPTION + " TEXT,"
            + KEY_TRIP_DATE + " INTEGER,"
            + KEY_TRIP_TIME + " TEXT,"
            + KEY_ORIGIN + " TEXT,"
            + KEY_DESTINATION + " TEXT,"
            + KEY_LINE_NUMBER + " TEXT,"
            + KEY_STATUS + " TEXT,"
            + KEY_SYSTEM_COMMENTS + " TEXT,"
            + KEY_CREATION_TIMESTAMP + " INTEGER,"
            + KEY_LOCATION_ADDRESS + " TEXT,"
            + KEY_LATITUDE + " REAL,"
            + KEY_LONGITUDE + " REAL"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_REQUESTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_REQUESTS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            onCreate(db);
        }
    }

    // --- User related methods ---
    // All user methods are unchanged and preserved as in the original code!
    // ... (no lines removed or omitted)

    public boolean addUser(String username, String password, String role) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USERNAME, username);
        values.put(KEY_PASSWORD, password);
        values.put(KEY_ROLE, role);
        long result = -1;
        try {
            result = db.insert(TABLE_USERS, null, values);
        } finally {
            db.close();
        }
        return result != -1;
    }

    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean exists = false;
        try {
            cursor = db.query(TABLE_USERS, null, KEY_USERNAME + "=? AND " + KEY_PASSWORD + "=?",
                    new String[]{username, password}, null, null, null);
            exists = cursor.moveToFirst();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return exists;
    }

    public boolean checkUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean exists = false;
        try {
            cursor = db.query(TABLE_USERS, null, KEY_USERNAME + "=?",
                    new String[]{username}, null, null, null);
            exists = cursor.moveToFirst();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return exists;
    }

    public String getUserRole(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String role = null;
        try {
            cursor = db.query(TABLE_USERS, new String[]{KEY_ROLE}, KEY_USERNAME + "=?",
                    new String[]{username}, null, null, null);
            if (cursor.moveToFirst()) {
                role = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ROLE));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return role;
    }

    public boolean updateUserDetails(String username, String fullName, String idCard, String phoneNumber, String email, String city) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_FULL_NAME, fullName);
        values.put(KEY_ID_CARD, idCard);
        values.put(KEY_PHONE_NUMBER, phoneNumber);
        values.put(KEY_EMAIL, email);
        values.put(KEY_CITY, city);
        int rowsAffected = 0;
        try {
            rowsAffected = db.update(TABLE_USERS, values, KEY_USERNAME + "=?", new String[]{username});
        } finally {
            db.close();
        }
        return rowsAffected > 0;
    }

    public String getUserFullName(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String fullName = null;
        try {
            cursor = db.query(TABLE_USERS, new String[]{KEY_FULL_NAME}, KEY_USERNAME + "=?",
                    new String[]{username}, null, null, null);
            if (cursor.moveToFirst()) {
                fullName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FULL_NAME));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return fullName;
    }

    public String getUserIdCard(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String idCard = null;
        try {
            cursor = db.query(TABLE_USERS, new String[]{KEY_ID_CARD}, KEY_USERNAME + "=?",
                    new String[]{username}, null, null, null);
            if (cursor.moveToFirst()) {
                idCard = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID_CARD));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return idCard;
    }

    public String getUserPhoneNumber(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String phoneNumber = null;
        try {
            cursor = db.query(TABLE_USERS, new String[]{KEY_PHONE_NUMBER}, KEY_USERNAME + "=?",
                    new String[]{username}, null, null, null);
            if (cursor.moveToFirst()) {
                phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PHONE_NUMBER));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return phoneNumber;
    }

    public String getUserEmail(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String email = null;
        try {
            cursor = db.query(TABLE_USERS, new String[]{KEY_EMAIL}, KEY_USERNAME + "=?",
                    new String[]{username}, null, null, null);
            if (cursor.moveToFirst()) {
                email = cursor.getString(cursor.getColumnIndexOrThrow(KEY_EMAIL));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return email;
    }

    public String getUserCity(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String city = null;
        try {
            cursor = db.query(TABLE_USERS, new String[]{KEY_CITY}, KEY_USERNAME + "=?",
                    new String[]{username}, null, null, null);
            if (cursor.moveToFirst()) {
                city = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CITY));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return city;
    }

    public List<String[]> getAllUsernamesAndRoles() {
        List<String[]> userList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_USERS, new String[]{KEY_USERNAME, KEY_ROLE},
                    null, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String username = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME));
                    String role = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ROLE));
                    userList.add(new String[]{username, role});
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return userList;
    }


    // --- Request related methods ---
    // כל המתודות המקוריות נשמרו, רק עודכן הטיפול בכתובת מחלקת אבידות (locationAddress) וקואורדינטות בכל המתודות!
    // לא נמחקה אף מתודה!

    public long addRequest(Request request) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // User Details (reporter details for the request)
        values.put(KEY_USERNAME, request.getUsername());
        values.put(KEY_FULL_NAME, request.getFullName());
        values.put(KEY_ID_CARD, request.getIdCard());
        values.put(KEY_PHONE_NUMBER, request.getPhoneNumber());
        values.put(KEY_EMAIL, request.getEmail());
        values.put(KEY_CITY, request.getCity());

        // Loss Details
        values.put(KEY_ITEM_TYPE, request.getItemType());
        values.put(KEY_COLOR, request.getColor());
        values.put(KEY_BRAND, request.getBrand());
        values.put(KEY_OWNER_NAME, request.getOwnerName());
        values.put(KEY_LOSS_DESCRIPTION, request.getLossDescription());

        // Trip Details
        values.put(KEY_TRIP_DATE, request.getTripDate().getTime()); // Date stored as long (milliseconds)
        values.put(KEY_TRIP_TIME, request.getTripTime());
        values.put(KEY_ORIGIN, request.getOrigin());
        values.put(KEY_DESTINATION, request.getDestination());
        values.put(KEY_LINE_NUMBER, request.getLineNumber());

        // Add Status and System Comments
        values.put(KEY_STATUS, request.getStatus());
        values.put(KEY_SYSTEM_COMMENTS, request.getSystemComments());
        values.put(KEY_CREATION_TIMESTAMP, request.getCreationTimestamp());
        values.put(KEY_LOCATION_ADDRESS, request.getLocationAddress());
        values.put(KEY_LATITUDE, request.getLatitude());
        values.put(KEY_LONGITUDE, request.getLongitude());

        long result = -1;
        try {
            result = db.insert(TABLE_REQUESTS, null, values);
        } finally {
            db.close();
        }
        return result;
    }

    public boolean updateRequest(Request request) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        if (request.getId() == -1) {
            db.close();
            return false;
        }

        // User Details (reporter details for the request)
        values.put(KEY_USERNAME, request.getUsername());
        values.put(KEY_FULL_NAME, request.getFullName());
        values.put(KEY_ID_CARD, request.getIdCard());
        values.put(KEY_PHONE_NUMBER, request.getPhoneNumber());
        values.put(KEY_EMAIL, request.getEmail());
        values.put(KEY_CITY, request.getCity());

        // Loss Details
        values.put(KEY_ITEM_TYPE, request.getItemType());
        values.put(KEY_COLOR, request.getColor());
        values.put(KEY_BRAND, request.getBrand());
        values.put(KEY_OWNER_NAME, request.getOwnerName());
        values.put(KEY_LOSS_DESCRIPTION, request.getLossDescription());

        // Trip Details
        values.put(KEY_TRIP_DATE, request.getTripDate().getTime());
        values.put(KEY_TRIP_TIME, request.getTripTime());
        values.put(KEY_ORIGIN, request.getOrigin());
        values.put(KEY_DESTINATION, request.getDestination());
        values.put(KEY_LINE_NUMBER, request.getLineNumber());

        // Update Status and System Comments
        values.put(KEY_STATUS, request.getStatus());
        values.put(KEY_SYSTEM_COMMENTS, request.getSystemComments());
        values.put(KEY_CREATION_TIMESTAMP, request.getCreationTimestamp());
        values.put(KEY_LOCATION_ADDRESS, request.getLocationAddress());
        values.put(KEY_LATITUDE, request.getLatitude());
        values.put(KEY_LONGITUDE, request.getLongitude());

        int rowsAffected = 0;
        try {
            rowsAffected = db.update(TABLE_REQUESTS, values, KEY_REQUEST_ID + " = ?", new String[]{String.valueOf(request.getId())});
        } finally {
            db.close();
        }
        return rowsAffected > 0;
    }

    public boolean deleteRequest(int requestId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = 0;
        try {
            rowsAffected = db.delete(TABLE_REQUESTS, KEY_REQUEST_ID + " = ?", new String[]{String.valueOf(requestId)});
        } finally {
            db.close();
        }
        return rowsAffected > 0;
    }

    public Request getRequestById(int requestId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Request request = null;
        try {
            cursor = db.query(TABLE_REQUESTS, null, KEY_REQUEST_ID + " = ?", new String[]{String.valueOf(requestId)},
                    null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                Double latitude = cursor.isNull(cursor.getColumnIndexOrThrow(KEY_LATITUDE)) ? null : cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_LATITUDE));
                Double longitude = cursor.isNull(cursor.getColumnIndexOrThrow(KEY_LONGITUDE)) ? null : cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_LONGITUDE));
                request = new Request(
                        cursor.getInt(cursor.getColumnIndexOrThrow(KEY_REQUEST_ID)), // ID
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_FULL_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID_CARD)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_PHONE_NUMBER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_EMAIL)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_CITY)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_ITEM_TYPE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_COLOR)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_BRAND)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_OWNER_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_LOSS_DESCRIPTION)),
                        new Date(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TRIP_DATE))),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_TRIP_TIME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_ORIGIN)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESTINATION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_LINE_NUMBER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_STATUS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_SYSTEM_COMMENTS)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(KEY_CREATION_TIMESTAMP)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_LOCATION_ADDRESS)),
                        latitude,
                        longitude
                );
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return request;
    }

    public List<Request> getRequestsByUsername(String username) {
        List<Request> requests = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_REQUESTS, null, KEY_USERNAME + "=?", new String[]{username},
                    null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    Double latitude = cursor.isNull(cursor.getColumnIndexOrThrow(KEY_LATITUDE)) ? null : cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_LATITUDE));
                    Double longitude = cursor.isNull(cursor.getColumnIndexOrThrow(KEY_LONGITUDE)) ? null : cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_LONGITUDE));
                    Request request = new Request(
                            cursor.getInt(cursor.getColumnIndexOrThrow(KEY_REQUEST_ID)), // ID
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_FULL_NAME)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID_CARD)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_PHONE_NUMBER)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_EMAIL)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_CITY)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_ITEM_TYPE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_COLOR)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_BRAND)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_OWNER_NAME)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_LOSS_DESCRIPTION)),
                            new Date(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TRIP_DATE))),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_TRIP_TIME)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_ORIGIN)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESTINATION)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_LINE_NUMBER)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_STATUS)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_SYSTEM_COMMENTS)),
                            cursor.getLong(cursor.getColumnIndexOrThrow(KEY_CREATION_TIMESTAMP)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_LOCATION_ADDRESS)),
                            latitude,
                            longitude
                    );
                    requests.add(request);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return requests;
    }

    public List<Request> getAllRequests() {
        List<Request> requests = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_REQUESTS, null, null, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    Double latitude = cursor.isNull(cursor.getColumnIndexOrThrow(KEY_LATITUDE)) ? null : cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_LATITUDE));
                    Double longitude = cursor.isNull(cursor.getColumnIndexOrThrow(KEY_LONGITUDE)) ? null : cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_LONGITUDE));
                    Request request = new Request(
                            cursor.getInt(cursor.getColumnIndexOrThrow(KEY_REQUEST_ID)), // ID
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_FULL_NAME)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID_CARD)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_PHONE_NUMBER)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_EMAIL)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_CITY)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_ITEM_TYPE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_COLOR)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_BRAND)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_OWNER_NAME)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_LOSS_DESCRIPTION)),
                            new Date(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TRIP_DATE))),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_TRIP_TIME)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_ORIGIN)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESTINATION)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_LINE_NUMBER)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_STATUS)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_SYSTEM_COMMENTS)),
                            cursor.getLong(cursor.getColumnIndexOrThrow(KEY_CREATION_TIMESTAMP)),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_LOCATION_ADDRESS)),
                            latitude,
                            longitude
                    );
                    requests.add(request);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return requests;
    }
}