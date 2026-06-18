package com.anokix.distributor.model;

public class AppPreferences {
    public final String language;
    public final String currency;
    public final String timezone;
    public final String dateFormat;

    public AppPreferences(String language, String currency, String timezone, String dateFormat) {
        this.language = language;
        this.currency = currency;
        this.timezone = timezone;
        this.dateFormat = dateFormat;
    }
}
