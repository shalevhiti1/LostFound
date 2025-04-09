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
    private static final int DATABASE_VERSION = 2;

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
            + KEY_LINE_NUMBER + " TEXT" + ")";

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
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_REQUESTS);
            db.execSQL(CREATE_TABLE_REQUESTS);
        }
    }

    // ... (rest of the DatabaseHelper methods)
    // ... (addUser, checkUser, checkUsername, getUserRole, addUserDetails, addRequest, etc.)
    // ... (getUserFullName, getUserIdCard, getUserPhoneNumber, getUserEmail, getUserCity)
    // ... (getRequestsByUsername, getAllRequests)
    public boolean addUser(String username, String password, String role) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USERNAME, username);
        values.put(KEY_PASSWORD, password);
        values.put(KEY_ROLE, role);
        long result = db.insert(TABLE_USERS, null, values);
        db.close();
        return result != -1;
    }

    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, KEY_USERNAME + "=? AND " + KEY_PASSWORD + "=?", new String[]{username, password}, null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }

    public boolean checkUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, KEY_USERNAME + "=?", new String[]{username}, null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }

    public String getUserRole(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{KEY_ROLE}, KEY_USERNAME + "=?", new String[]{username}, null, null, null);
        String role = null;
        if (cursor.moveToFirst()) {
            role = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return role;
    }

    public boolean addUserDetails(String username, String fullName, String idCard, String phoneNumber, String email, String city) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_FULL_NAME, fullName);
        values.put(KEY_ID_CARD, idCard);
        values.put(KEY_PHONE_NUMBER, phoneNumber);
        values.put(KEY_EMAIL, email);
        values.put(KEY_CITY, city);
        long result = db.update(TABLE_USERS, values, KEY_USERNAME + "=?", new String[]{username});
        db.close();
        return result != -1;
    }

    public boolean addRequest(Request request) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // User Details
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
        values.put(KEY_TRIP_DATE, request.getTripDate().getTime()); // Store as long
        values.put(KEY_TRIP_TIME, request.getTripTime());
        values.put(KEY_ORIGIN, request.getOrigin());
        values.put(KEY_DESTINATION, request.getDestination());
        values.put(KEY_LINE_NUMBER, request.getLineNumber());

        long result = db.insert(TABLE_REQUESTS, null, values);
        db.close();
        return result != -1;
    }

    public String getUserFullName(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{KEY_FULL_NAME}, KEY_USERNAME + "=?",
                new String[]{username}, null, null, null);
        String fullName = null;
        if (cursor.moveToFirst()) {
            fullName = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return fullName;
    }


    public String getUserIdCard(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{KEY_ID_CARD}, KEY_USERNAME + "=?",
                new String[]{username}, null, null, null);
        String idCard = null;
        if (cursor.moveToFirst()) {
            idCard = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return idCard;
    }

    public String getUserPhoneNumber(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{KEY_PHONE_NUMBER}, KEY_USERNAME + "=?",
                new String[]{username}, null, null, null);
        String phoneNumber = null;
        if (cursor.moveToFirst()) {
            phoneNumber = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return phoneNumber;
    }

    public String getUserEmail(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{KEY_EMAIL}, KEY_USERNAME + "=?",
                new String[]{username}, null, null, null);
        String email = null;
        if (cursor.moveToFirst()) {
            email = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return email;
    }

    public String getUserCity(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{KEY_CITY}, KEY_USERNAME + "=?",
                new String[]{username}, null, null, null);
        String city = null;
        if (cursor.moveToFirst()) {
            city = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return city;
    }

    public List<Request> getRequestsByUsername(String username) {
        List<Request> requests = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_REQUESTS, null, KEY_USERNAME + "=?", new String[]{username},
                null, null, null);
        if (cursor.moveToFirst()) {
            do {
                Request request = new Request(
                        cursor.getString(0), // username
                        cursor.getString(1), // fullName
                        cursor.getString(2), // idCard
                        cursor.getString(3), // phoneNumber
                        cursor.getString(4), // email
                        cursor.getString(5), // city
                        cursor.getString(6), // itemType
                        cursor.getString(7), // color
                        cursor.getString(8), // brand
                        cursor.getString(9), // ownerName
                        cursor.getString(10), // lossDescription
                        new Date(cursor.getLong(11)), // tripDate
                        cursor.getString(12), // tripTime
                        cursor.getString(13), // origin
                        cursor.getString(14), // destination
                        cursor.getString(15)  // lineNumber
                );
                requests.add(request);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return requests;
    }

    public List<Request> getAllRequests() {
        List<Request> requests = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_REQUESTS, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                Request request = new Request(
                        cursor.getString(0), // username
                        cursor.getString(1), // fullName
                        cursor.getString(2), // idCard
                        cursor.getString(3), // phoneNumber
                        cursor.getString(4), // email
                        cursor.getString(5), // city
                        cursor.getString(6), // itemType
                        cursor.getString(7), // color
                        cursor.getString(8), // brand
                        cursor.getString(9), // ownerName
                        cursor.getString(10), // lossDescription
                        new Date(cursor.getLong(11)), // tripDate
                        cursor.getString(12), // tripTime
                        cursor.getString(13), // origin
                        cursor.getString(14), // destination
                        cursor.getString(15)  // lineNumber
                );
                requests.add(request);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return requests;
    }
}