package com.example.lostfound; // מגדיר את חבילת הקוד של האפליקציה.

import java.util.Date; // ייבוא המחלקה Date, לייצוג תאריכים ושעות.
import java.text.SimpleDateFormat; // ייבוא SimpleDateFormat, לעיצוב תאריכים (למרות שלא בשימוש ישיר במחלקה זו, הוא נפוץ בעבודה עם Date).
import java.util.Locale; // ייבוא Locale, להגדרת אזור גיאוגרפי (לעיצוב תאריכים).

/**
 * המחלקה {@code Request} מייצגת דיווח על אבידה במערכת LostFound.
 * היא מכילה את כל המידע הרלוונטי לדיווח, כולל פרטי המשתמש המדווח,
 * פרטי הפריט שאבד, פרטי הנסיעה שבה האבידה התרחשה, וכן סטטוס הטיפול
 * והערות מערכת.
 * המחלקה כוללת בנאים ליצירת פניות חדשות ולאחזור פניות קיימות ממסד הנתונים,
 * וכן מתודות Getters ו-Setters לגישה ושינוי הנתונים.
 */
public class Request {
    // מאפייני הפנייה - פרטים כלליים ומזהים.
    private int id; // מזהה ייחודי עבור הפנייה (מוגדר על ידי מסד הנתונים).
    private String username; // שם המשתמש של המדווח.

    // מאפייני הפריט שאבד.
    private String itemType; // סוג הפריט שאבד (לדוגמה: ארנק, טלפון).
    private String color; // צבע הפריט.
    private String brand; // מותג הפריט.
    private String ownerName; // שם הבעלים של הפריט (יכול להיות שונה מהמדווח).
    private String lossDescription; // תיאור מפורט של נסיבות האבידה.

    // מאפייני הנסיעה שבה אבדה האבידה.
    private Date tripDate; // תאריך הנסיעה (אובייקט Date).
    private String tripTime; // שעת הנסיעה.
    private String origin; // תחנת המוצא של הנסיעה.
    private String destination; // תחנת היעד של הנסיעה.
    private String lineNumber; // מספר קו האוטובוס.

    // מאפייני המדווח (פרטים אישיים).
    private String fullName; // שם מלא של המדווח.
    private String idCard; // מספר תעודת זהות של המדווח.
    private String phoneNumber; // מספר טלפון של המדווח.
    private String email; // כתובת אימייל של המדווח.
    private String city; // עיר מגורים של המדווח.

    // מאפיינים הקשורים לסטטוס הטיפול בפנייה.
    private String status; // סטטוס הטיפול בפנייה (לדוגמה: "In Progress", "Closed", "Found").
    private String systemComments; // הערות/תגובה ממנהל המערכת לגבי הטיפול בפנייה.
    private long creationTimestamp; // NEW: חותמת זמן של יצירת הפנייה (במילישניות).

    /**
     * קבוע המגדיר את הערת המערכת הראשונית עבור פניות חדשות.
     * הודעה זו מוצגת למשתמש מיד לאחר פתיחת פנייה.
     */
    public static final String DEFAULT_NEW_REQUEST_COMMENT = "Thank you for contacting us. Your request has been forwarded to the lost and found department. We will respond to your request within 3 business days.";

    /**
     * בנאי ליצירת אובייקט {@code Request} חדש (שעדיין לא נשמר במסד הנתונים).
     * ה-ID מאותחל ל--1 כדי לציין שהפנייה עדיין לא קיבלה ID ממסד הנתונים.
     * הסטטוס והערות המערכת מאותחלים לערכי ברירת מחדל.
     *
     * @param username שם המשתמש של המדווח.
     * @param fullName שם מלא של המדווח.
     * @param idCard מספר תעודת זהות של המדווח.
     * @param phoneNumber מספר טלפון של המדווח.
     * @param email כתובת אימייל של המדווח.
     * @param city עיר מגורים של המדווח.
     * @param itemType סוג הפריט שאבד.
     * @param color צבע הפריט.
     * @param brand מותג הפריט.
     * @param ownerName שם הבעלים של הפריט.
     * @param lossDescription תיאור מילולי של האבידה.
     * @param tripDate תאריך הנסיעה שבה אבד הפריט.
     * @param tripTime שעת הנסיעה.
     * @param origin תחנת המוצא של הנסיעה.
     * @param destination יעד הנסיעה.
     * @param lineNumber מספר קו האוטובוס.
     * @param creationTimestamp חותמת הזמן במילישניות שבה הפנייה נוצרה.
     */
    public Request(String username, String fullName, String idCard, String phoneNumber, String email, String city,
                   String itemType, String color, String brand, String ownerName, String lossDescription,
                   Date tripDate, String tripTime, String origin, String destination, String lineNumber,
                   long creationTimestamp) { // NEW: added creationTimestamp
        this.id = -1; // מציין שה-ID עדיין לא נוצר על ידי מסד הנתונים.
        this.username = username;
        this.fullName = fullName;
        this.idCard = idCard;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.city = city;
        this.itemType = itemType;
        this.color = color;
        this.brand = brand;
        this.ownerName = ownerName;
        this.lossDescription = lossDescription;
        this.tripDate = tripDate;
        this.tripTime = tripTime;
        this.origin = origin;
        this.destination = destination;
        this.lineNumber = lineNumber;
        this.status = "In Progress"; // סטטוס ברירת מחדל עבור פניות חדשות.
        this.systemComments = DEFAULT_NEW_REQUEST_COMMENT; // הערת מערכת ברירת מחדל.
        this.creationTimestamp = creationTimestamp; // NEW: initialize creationTimestamp
    }

    /**
     * בנאי ליצירת אובייקט {@code Request} קיים, אשר נשלף ממסד הנתונים.
     * בנאי זה מקבל את כל הפרטים, כולל ה-ID, הסטטוס והערות המערכת.
     *
     * @param id מזהה ייחודי של הפנייה.
     * @param username שם המשתמש של המדווח.
     * @param fullName שם מלא של המדווח.
     * @param idCard מספר תעודת זהות של המדווח.
     * @param phoneNumber מספר טלפון של המדווח.
     * @param email כתובת אימייל של המדווח.
     * @param city עיר מגורים של המדווח.
     * @param itemType סוג הפריט שאבד.
     * @param color צבע הפריט.
     * @param brand מותג הפריט.
     * @param ownerName שם הבעלים של הפריט.
     * @param lossDescription תיאור מילולי של האבידה.
     * @param tripDate תאריך הנסיעה שבה אבד הפריט.
     * @param tripTime שעת הנסיעה.
     * @param origin תחנת המוצא של הנסיעה.
     * @param destination יעד הנסיעה.
     * @param lineNumber מספר קו האוטובוס.
     * @param status סטטוס הטיפול בפנייה.
     * @param systemComments הערות מערכת הקשורות לפנייה.
     * @param creationTimestamp חותמת הזמן במילישניות שבה הפנייה נוצרה.
     */
    public Request(int id, String username, String fullName, String idCard, String phoneNumber, String email, String city,
                   String itemType, String color, String brand, String ownerName, String lossDescription,
                   Date tripDate, String tripTime, String origin, String destination, String lineNumber,
                   String status, String systemComments, long creationTimestamp) { // NEW: added creationTimestamp
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.idCard = idCard;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.city = city;
        this.itemType = itemType;
        this.color = color;
        this.brand = brand;
        this.ownerName = ownerName;
        this.lossDescription = lossDescription;
        this.tripDate = tripDate;
        this.tripTime = tripTime;
        this.origin = origin;
        this.destination = destination;
        this.lineNumber = lineNumber;
        this.status = status; // אתחול סטטוס.
        this.systemComments = systemComments; // אתחול הערות מערכת.
        this.creationTimestamp = creationTimestamp; // NEW: initialize creationTimestamp
    }

    // --- מתודות Getters ו-Setters עבור כל מאפייני המחלקה ---

    /**
     * מחזירה את המזהה הייחודי של הפנייה.
     * @return ה-ID של הפנייה.
     */
    public int getId() {
        return id;
    }

    /**
     * מגדירה את המזהה הייחודי של הפנייה.
     * @param id ה-ID החדש של הפנייה.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * מחזירה את שם המשתמש של המדווח.
     * @return שם המשתמש.
     */
    public String getUsername() {
        return username;
    }

    /**
     * מגדירה את שם המשתמש של המדווח.
     * @param username שם המשתמש החדש.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * מחזירה את סוג הפריט שאבד.
     * @return סוג הפריט.
     */
    public String getItemType() {
        return itemType;
    }

    /**
     * מגדירה את סוג הפריט שאבד.
     * @param itemType סוג הפריט החדש.
     */
    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    /**
     * מחזירה את צבע הפריט שאבד.
     * @return צבע הפריט.
     */
    public String getColor() {
        return color;
    }

    /**
     * מגדירה את צבע הפריט שאבד.
     * @param color צבע הפריט החדש.
     */
    public void setColor(String color) {
        this.color = color;
    }

    /**
     * מחזירה את מותג הפריט שאבד.
     * @return מותג הפריט.
     */
    public String getBrand() {
        return brand;
    }

    /**
     * מגדירה את מותג הפריט שאבד.
     * @param brand מותג הפריט החדש.
     */
    public void setBrand(String brand) {
        this.brand = brand;
    }

    /**
     * מחזירה את שם הבעלים של הפריט.
     * @return שם הבעלים.
     */
    public String getOwnerName() {
        return ownerName;
    }

    /**
     * מגדירה את שם הבעלים של הפריט.
     * @param ownerName שם הבעלים החדש.
     */
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    /**
     * מחזירה את תיאור האבידה.
     * @return תיאור האבידה.
     */
    public String getLossDescription() {
        return lossDescription;
    }

    /**
     * מגדירה את תיאור האבידה.
     * @param lossDescription תיאור האבידה החדש.
     */
    public void setLossDescription(String lossDescription) {
        this.lossDescription = lossDescription;
    }

    /**
     * מחזירה את תאריך הנסיעה שבה אבד הפריט.
     * @return אובייקט {@code Date} המייצג את תאריך הנסיעה.
     */
    public Date getTripDate() {
        return tripDate;
    }

    /**
     * מגדירה את תאריך הנסיעה שבה אבד הפריט.
     * @param tripDate אובייקט {@code Date} המייצג את תאריך הנסיעה החדש.
     */
    public void setTripDate(Date tripDate) {
        this.tripDate = tripDate;
    }

    /**
     * מחזירה את שעת הנסיעה.
     * @return שעת הנסיעה.
     */
    public String getTripTime() {
        return tripTime;
    }

    /**
     * מגדירה את שעת הנסיעה.
     * @param tripTime שעת הנסיעה החדשה.
     */
    public void setTripTime(String tripTime) {
        this.tripTime = tripTime;
    }

    /**
     * מחזירה את נקודת המוצא של הנסיעה.
     * @return נקודת המוצא.
     */
    public String getOrigin() {
        return origin;
    }

    /**
     * מגדירה את נקודת המוצא של הנסיעה.
     * @param origin נקודת המוצא החדשה.
     */
    public void setOrigin(String origin) {
        this.origin = origin;
    }

    /**
     * מחזירה את יעד הנסיעה.
     * @return יעד הנסיעה.
     */
    public String getDestination() {
        return destination;
    }

    /**
     * מגדירה את יעד הנסיעה.
     * @param destination יעד הנסיעה החדש.
     */
    public void setDestination(String destination) {
        this.destination = destination;
    }

    /**
     * מחזירה את מספר קו האוטובוס.
     * @return מספר קו האוטובוס.
     */
    public String getLineNumber() {
        return lineNumber;
    }

    /**
     * מגדירה את מספר קו האוטובוס.
     * @param lineNumber מספר קו האוטובוס החדש.
     */
    public void setLineNumber(String lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * מחזירה את השם המלא של המדווח.
     * @return השם המלא.
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * מגדירה את השם המלא של המדווח.
     * @param fullName השם המלא החדש.
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * מחזירה את מספר תעודת הזהות של המדווח.
     * @return מספר תעודת הזהות.
     */
    public String getIdCard() {
        return idCard;
    }

    /**
     * מגדירה את מספר תעודת הזהות של המדווח.
     * @param idCard מספר תעודת הזהות החדש.
     */
    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    /**
     * מחזירה את מספר הטלפון של המדווח.
     * @return מספר הטלפון.
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * מגדירה את מספר הטלפון של המדווח.
     * @param phoneNumber מספר הטלפון החדש.
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * מחזירה את כתובת האימייל של המדווח.
     * @return כתובת האימייל.
     */
    public String getEmail() {
        return email;
    }

    /**
     * מגדירה את כתובת האימייל של המדווח.
     * @param email כתובת האימייל החדשה.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * מחזירה את עיר המגורים של המדווח.
     * @return עיר המגורים.
     */
    public String getCity() {
        return city;
    }

    /**
     * מגדירה את עיר המגורים של המדווח.
     * @param city עיר המגורים החדשה.
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * מחזירה את סטטוס הטיפול בפנייה.
     * @return סטטוס הפנייה (לדוגמה: "In Progress", "Closed", "Found").
     */
    public String getStatus() {
        return status;
    }

    /**
     * מגדירה את סטטוס הטיפול בפנייה.
     * @param status סטטוס הפנייה החדש.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * מחזירה את הערות המערכת/תגובת המנהל הקשורות לפנייה.
     * @return הערות המערכת.
     */
    public String getSystemComments() {
        return systemComments;
    }

    /**
     * מגדירה את הערות המערכת/תגובת המנהל הקשורות לפנייה.
     * @param systemComments הערות המערכת החדשות.
     */
    public void setSystemComments(String systemComments) {
        this.systemComments = systemComments;
    }

    /**
     * מחזירה את חותמת הזמן במילישניות שבה הפנייה נוצרה.
     * @return חותמת הזמן של יצירת הפנייה.
     */
    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    /**
     * מגדירה את חותמת הזמן במילישניות שבה הפנייה נוצרה.
     * @param creationTimestamp חותמת הזמן החדשה.
     */
    public void setCreationTimestamp(long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    /**
     * מתודת {@code toString} מותאמת אישית.
     * היא מחזירה ייצוג מחרוזתי של אובייקט {@code Request},
     * המשמש בדרך כלל להצגה ברכיבי ממשק משתמש כמו {@code ListView}
     * (לדוגמה, על ידי {@code ArrayAdapter}).
     *
     * @return מחרוזת המכילה את ID הפנייה וסוג הפריט.
     */
    @Override
    public String toString() {
        // מעצב את הפלט כך שיציג את ID הפנייה וסוג הפריט.
        return "Case ID: " + id + " | Item Type: " + itemType;
    }
}
