package com.example.lostfound;

import java.util.Date;

public class Request {
    private String username;
    private String itemType;
    private String color;
    private String brand;
    private String ownerName;
    private String lossDescription;
    private Date tripDate;
    private String tripTime;
    private String origin;
    private String destination;
    private String lineNumber;
    private String fullName;
    private String idCard;
    private String phoneNumber;
    private String email;
    private String city;

    // Constructor
    public Request(String username, String fullName, String idCard, String phoneNumber, String email, String city,
                   String itemType, String color, String brand, String ownerName, String lossDescription,
                   Date tripDate, String tripTime, String origin, String destination, String lineNumber) {
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
    }

    // Getters and setters
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
}