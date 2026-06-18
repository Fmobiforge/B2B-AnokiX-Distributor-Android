package com.anokix.distributor.model;

public class BusinessSettings {
    public final String businessName;
    public final String registrationNumber;
    public final String vatNumber;
    public final String address;
    public final String city;
    public final Double latitude;
    public final Double longitude;

    public BusinessSettings(String businessName, String registrationNumber, String vatNumber,
                            String address, String city, Double latitude, Double longitude) {
        this.businessName = businessName;
        this.registrationNumber = registrationNumber;
        this.vatNumber = vatNumber;
        this.address = address;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
