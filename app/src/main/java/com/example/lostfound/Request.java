package com.example.lostfound;

import java.util.Date;
// אין צורך ב-SimpleDateFormat ו-Locale כאן, הם לרוב משמשים להצגה חיצונית של תאריכים
// import java.text.SimpleDateFormat;
// import java.util.Locale;

/**
 * המחלקה {@code Request} מייצגת דיווח על אבידה במערכת LostFound.
 * היא מכילה את כל המידע הרלוונטי לדיווח, כולל פרטי המשתמש המדווח,
 * פרטי הפריט שאבד, פרטי הנסיעה שבה האבידה התרחשה, וכן סטטוס הטיפול
 * והערות מערכת.
 * המחלקה כוללת בנאים ליצירת פניות חדשות ולאחזור פניות קיימות ממסד הנתונים,
 * וכן מתודות Getters ו-Setters לגישה ושינוי הנתונים.
 *
 * **עדכון עבור Firebase Firestore:**
 * - נוסף שדה `firestoreId` (String) לאחסון מזהה המסמך הייחודי של Firestore.
 * - נוסף בנאי ריק (no-argument constructor) הנדרש למיפוי אוטומטי של Firestore.
 * - השדה `id` (int) נשמר לתאימות לאחור מ-SQLite אך אינו משמש כמזהה הראשי ב-Firestore.
 */
public class Request {
    // סטטוס הפנייה - כ-ENUM
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
            if (status == null) {
                return IN_PROGRESS; // ברירת מחדל אם הסטטוס ריק
            }
            for (StatusEnum s : StatusEnum.values()) {
                if (s.name().equalsIgnoreCase(status) || s.displayName.equals(status)) {
                    return s;
                }
            }
            return IN_PROGRESS; // ברירת מחדל למקרה שלא נמצאה התאמה
        }
    }

    // מאפייני הפנייה - פרטים כלליים ומזהים.
    private int id; // מזהה ייחודי עבור הפנייה (מוגדר על ידי מסד הנתונים SQLite בעבר). לא בשימוש ישיר כמזהה ב-Firestore.
    private String firestoreId; // NEW: מזהה ייחודי עבור המסמך ב-Firebase Firestore.
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
    private StatusEnum statusEnum; // סטטוס כ-Enum
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
     * NEW: בנאי ריק (no-argument constructor) הנדרש ל-Firebase Firestore.
     * משמש כאשר Firestore ממיר מסמך לאובייקט Request.
     */
    public Request() {
        // Initialize default values if needed, or leave blank for Firebase to populate
        this.id = -1; // Default value for SQLite ID, not used by Firestore as primary ID
        this.statusEnum = StatusEnum.IN_PROGRESS; // Default status
        this.status = statusEnum.name();
        this.systemComments = DEFAULT_NEW_REQUEST_COMMENT; // Default comment
        this.creationTimestamp = System.currentTimeMillis(); // Default creation time
    }

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
        this(); // קריאה לבנאי הריק כדי לאתחל ערכי ברירת מחדל.
        // this.id = -1; // כבר מאותחל בבנאי הריק
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
        // this.statusEnum = StatusEnum.IN_PROGRESS; // כבר מאותחל בבנאי הריק
        // this.status = statusEnum.name(); // כבר מאותחל בבנאי הריק
        // this.systemComments = DEFAULT_NEW_REQUEST_COMMENT; // כבר מאותחל בבנאי הריק
        this.creationTimestamp = creationTimestamp;
        // this.locationAddress = null; // כבר מאותחל בבנאי הריק (או ע"י Firestore)
        // this.latitude = null; // כבר מאותחל בבנאי הריק (או ע"י Firestore)
        // this.longitude = null; // כבר מאותחל בבנאי הריק (או ע"י Firestore)
    }

    /**
     * בנאי מלא עם locationAddress, קואורדינטות וסטטוס ENUM.
     * בנאי זה משמש בעיקר כאשר טוענים נתונים מ-Firestore או בונים אובייקט שלם ידנית.
     * ה-`id` כאן הוא ה-ID מ-SQLite אם עדיין בשימוש, או -1.
     * ה-`firestoreId` יוגדר בנפרד לאחר הטעינה מ-Firestore, או יעבור כפרמטר אם בונים אובייקט קיים.
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
        this.status = statusEnum != null ? statusEnum.name() : null; // ודא סנכרון עם ה-Enum
        this.systemComments = systemComments;
        this.creationTimestamp = creationTimestamp;
        this.locationAddress = locationAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.firestoreId = null; // יש להגדיר אותו לאחר יצירת האובייקט אם נטען מ-Firestore
    }

    /**
     * בנאי ליצירת אובייקט {@code Request} קיים, אשר נשלף ממסד הנתונים (או ממערכת דומה ל-SQLite).
     * בנאי זה מקבל את כל הפרטים, כולל ה-ID, הסטטוס (כמחרוזת), הערות המערכת, כתובת וקואורדינטות.
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
        this.statusEnum = StatusEnum.fromString(status); // המרה ל-Enum
        this.systemComments = systemComments;
        this.creationTimestamp = creationTimestamp;
        this.locationAddress = locationAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.firestoreId = null; // יש להגדיר אותו לאחר יצירת האובייקט אם נטען מ-Firestore
    }

    // --- מתודות Getters ו-Setters עבור כל מאפייני המחלקה ---
    // Firestore משתמש בהן כדי למפות נתונים.

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // NEW: Getter ו-Setter עבור firestoreId
    public String getFirestoreId() {
        return firestoreId;
    }

    public void setFirestoreId(String firestoreId) {
        this.firestoreId = firestoreId;
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
     * @return סטטוס הפנייה כמחרוזת.
     */
    public String getStatus() {
        // תמיד מסונכרן עם ה-Enum. אם ה-Enum הוא null, זה יחזיר את שדה ה-String הישן.
        return statusEnum != null ? statusEnum.name() : status;
    }

    /**
     * מגדירה את סטטוס הטיפול בפנייה (גם ב-Enum וגם במחרוזת).
     * @param status סטטוס הפנייה כמחרוזת.
     */
    public void setStatus(String status) {
        this.status = status;
        this.statusEnum = StatusEnum.fromString(status);
    }

    /**
     * מחזירה את סטטוס הטיפול בפנייה כ-Enum.
     * @return סטטוס הפנייה כ-Enum.
     */
    public StatusEnum getStatusEnum() {
        return statusEnum;
    }

    /**
     * מגדירה את סטטוס הטיפול בפנייה כ-Enum.
     * @param statusEnum סטטוס הפנייה כ-Enum.
     */
    public void setStatusEnum(StatusEnum statusEnum) {
        this.statusEnum = statusEnum;
        this.status = statusEnum != null ? statusEnum.name() : null; // ודא סנכרון עם שדה הסטרינג
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

    public String getLocationAddress() {
        return locationAddress;
    }

    public void setLocationAddress(String locationAddress) {
        this.locationAddress = locationAddress;
    }

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
     * כעת כוללת גם את ה-Firestore ID.
     *
     * @return מחרוזת המכילה את ID הפנייה וסוג הפריט.
     */
    @Override
    public String toString() {
        // מעצב את הפלט כך שיציג את ID הפנייה (עדיפות ל-Firestore ID) וסוג הפריט.
        String idToDisplay = (firestoreId != null && !firestoreId.isEmpty()) ? firestoreId : String.valueOf(id);
        return "Case ID: " + idToDisplay + " | Item Type: " + itemType;
    }
}