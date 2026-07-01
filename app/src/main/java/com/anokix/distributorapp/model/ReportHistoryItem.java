package com.anokix.distributorapp.model;

public class ReportHistoryItem {
    public final String id;
    public final String name;
    public final String generatedDate;
    public final String format;

    public ReportHistoryItem(String id, String name, String generatedDate, String format) {
        this.id = id;
        this.name = name;
        this.generatedDate = generatedDate;
        this.format = format;
    }
}
