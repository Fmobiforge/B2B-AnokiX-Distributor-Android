package com.anokix.distributorapp.model;

public class ProfileSettings {
    public final String firstName;
    public final String lastName;
    public final String email;
    public final String phone;
    public final String companyName;

    public ProfileSettings(String firstName, String lastName, String email, String phone,
                           String companyName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.companyName = companyName;
    }
}
