package com.anokix.distributorapp.model;

public class OrderItem {
    public final String orderId;
    public final String traderName;
    public final String initials;
    public final String location;
    public final String amount;
    public final String payment;
    public final String time;
    public final String status;
    public final String relativeTime;
    public final String productSummary;
    public final boolean isPaid;

    public OrderItem(String orderId, String traderName, String initials, String location,
                     String amount, String payment, String time, String status) {
        this(orderId, traderName, initials, location, amount, payment, time, status,
                time, "", false);
    }

    public OrderItem(String orderId, String traderName, String initials, String location,
                     String amount, String payment, String time, String status,
                     String relativeTime, String productSummary, boolean isPaid) {
        this.orderId = orderId;
        this.traderName = traderName;
        this.initials = initials;
        this.location = location;
        this.amount = amount;
        this.payment = payment;
        this.time = time;
        this.status = status;
        this.relativeTime = relativeTime;
        this.productSummary = productSummary;
        this.isPaid = isPaid;
    }
}
