package com.example.lostfound;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * המחלקה {@code Request} מייצגת דיווח על אבידה במערכת LostFound.
 * היא מכילה את כל המידע הרלוונטי לדיווח, כולל פרטי המשתמש המדווח,
 * פרטי הפריט שאבד, פרטי הנסיעה שבה האבידה התרחשה, וכן סטטוס הטיפול
 * והערות מערכת.
 * המחלקה כוללת בנאים ליצירת פניות חדשות ולאחזור פניות קיימות ממסד הנתונים,
 * וכן מתודות Getters ו-Setters לגישה ושינוי הנתונים.
 */
public class Request {
    // סטטוס הפנייה - כ-ENUM (חדש)
    public enum StatusEnum {
        IN_PROGRESS("פנייה בטיפול"),
        FOUND("אבידה נמצאה"),
        NOT_FOUND("אבידה לא נמצאה"),
        REJECTED("פנייה נדחתה");

        private final String displayName;

        StatusEnum(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static StatusEnum fromString(String status) {
            for (StatusEnum s : StatusEnum.values()) {
                if (s.name().equalsIgnoreCase(status) || s.displayName.equals(status)) {
                    return s;
                }
            }
            return IN_PROGRESS;
        }
    }

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
    private String status; // סטטוס הטיפול בפנייה (מתוך תאימות לאחור - ישתנה אוטומטית לפי ה-Enum)
    private StatusEnum statusEnum; // סטטוס כ-Enum (חדש)
    private String systemComments; // הערות/תגובה ממנהל המערכת לגבי הטיפול בפנייה.
    private long creationTimestamp; // חותמת זמן של יצירת הפנייה (במילישניות).

    // שדה חדש: כתובת מחלקת האבידות
    private String locationAddress;

    // שדות חדשים: קואורדינטות מיקום מחלקת האבידות
    private Double latitude;
    private Double longitude;

    /**
     * קבוע המגדיר את הערת המערכת הראשונית עבור פניות חדשות.
     * הודעה זו מוצגת למשתמש מיד לאחר פתיחת פנייה.
     */
    public static final String DEFAULT_NEW_REQUEST_COMMENT = "Thank you for contacting us. Your request has been forwarded to the lost and found department. We will respond to your request within 3 business days.";

    /**
     * בנאי ליצירת אובייקט {@code Request} חדש (שעדיין לא נשמר במסד הנתונים).
     * ה-ID מאותחל ל--1 כדי לציין שהפנייה עדיין לא קיבלה ID ממסד הנתונים.
     * הסטטוס והערות המערכת מאותחלים לערכי ברירת מחדל.
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
                   long creationTimestamp) {
        this.id = -1;
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
        this.statusEnum = StatusEnum.IN_PROGRESS;
        this.status = statusEnum.name(); // תאימות לאחור
        this.systemComments = DEFAULT_NEW_REQUEST_COMMENT;
        this.creationTimestamp = creationTimestamp;
        this.locationAddress = null;
        this.latitude = null;
        this.longitude = null;
    }

    /**
     * בנאי מלא עם locationAddress, קואורדינטות וסטטוס ENUM (חדש).
     */
    public Request(int id, String username, String fullName, String idCard, String phoneNumber, String email, String city,
                   String itemType, String color, String brand, String ownerName, String lossDescription,
                   Date tripDate, String tripTime, String origin, String destination, String lineNumber,
                   StatusEnum statusEnum, String systemComments, long creationTimestamp, String locationAddress,
                   Double latitude, Double longitude) {
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
        this.statusEnum = statusEnum;
        this.status = statusEnum.name(); // תאימות לאחור
        this.systemComments = systemComments;
        this.creationTimestamp = creationTimestamp;
        this.locationAddress = locationAddress;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * בנאי ליצירת אובייקט {@code Request} קיים, אשר נשלף ממסד הנתונים.
     * בנאי זה מקבל את כל הפרטים, כולל ה-ID, הסטטוס, הערות המערכת, כתובת וקואורדינטות.
     */
    public Request(int id, String username, String fullName, String idCard, String phoneNumber, String email, String city,
                   String itemType, String color, String brand, String ownerName, String lossDescription,
                   Date tripDate, String tripTime, String origin, String destination, String lineNumber,
                   String status, String systemComments, long creationTimestamp, String locationAddress,
                   Double latitude, Double longitude) {
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
        this.status = status;
        this.statusEnum = StatusEnum.fromString(status);
        this.systemComments = systemComments;
        this.creationTimestamp = creationTimestamp;
        this.locationAddress = locationAddress;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // --- מתודות Getters ו-Setters עבור כל מאפייני המחלקה ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getLossDescription() {
        return lossDescription;
    }

    public void setLossDescription(String lossDescription) {
        this.lossDescription = lossDescription;
    }

    public Date getTripDate() {
        return tripDate;
    }

    public void setTripDate(Date tripDate) {
        this.tripDate = tripDate;
    }

    public String getTripTime() {
        return tripTime;
    }

    public void setTripTime(String tripTime) {
        this.tripTime = tripTime;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(String lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    /**
     * מחזירה את סטטוס הטיפול בפנייה כמחרוזת (תאימות לאחור).
     */
    public String getStatus() {
        // תמיד מסונכרן עם ה-Enum
        return statusEnum != null ? statusEnum.name() : status;
    }

    /**
     * מגדירה את סטטוס הטיפול בפנייה (גם ב-Enum וגם במחרוזת).
     */
    public void setStatus(String status) {
        this.status = status;
        this.statusEnum = StatusEnum.fromString(status);
    }

    /**
     * מחזירה את סטטוס הטיפול בפנייה כ-Enum.
     */
    public StatusEnum getStatusEnum() {
        return statusEnum;
    }

    /**
     * מגדירה את סטטוס הטיפול בפנייה כ-Enum.
     */
    public void setStatusEnum(StatusEnum statusEnum) {
        this.statusEnum = statusEnum;
        this.status = statusEnum.name();
    }

    public String getSystemComments() {
        return systemComments;
    }

    public void setSystemComments(String systemComments) {
        this.systemComments = systemComments;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    // שדה חדש: כתובת מחלקת האבידות
    public String getLocationAddress() {
        return locationAddress;
    }

    public void setLocationAddress(String locationAddress) {
        this.locationAddress = locationAddress;
    }

    // שדות חדשים: קואורדינטות
    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
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