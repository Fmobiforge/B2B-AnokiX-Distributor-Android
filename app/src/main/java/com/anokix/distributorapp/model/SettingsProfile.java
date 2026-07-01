package com.anokix.distributorapp.model;

public class SettingsProfile {
    public final String name;
    public final String email;
    public final String initials;
    public final boolean verified;

    public SettingsProfile(String name, String email, String initials, boolean verified) {
        this.name = name;
        this.email = email;
        this.initials = initials;
        this.verified = verified;
    }
}
