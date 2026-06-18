package com.anokix.distributor.model;

public class BusinessSettings {
    public final String businessName;
    public final String registrationNumber;
    public final String vatNumber;
    public final String address;
    public final String city;

    public BusinessSettings(String businessName, String registrationNumber, String vatNumber,
                            String address, String city) {
        this.businessName = businessName;
        this.registrationNumber = registrationNumber;
        this.vatNumber = vatNumber;
        this.address = address;
        this.city = city;
    }
}
