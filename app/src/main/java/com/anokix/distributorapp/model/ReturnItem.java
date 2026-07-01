package com.anokix.distributorapp.model;

public class ReturnItem {
    public final String id;
    public final String traderName;
    public final String reason;
    public final String amount;
    public final String timestamp;
    public final String status;

    public ReturnItem(String id, String traderName, String reason, String amount,
                      String timestamp, String status) {
        this.id = id;
        this.traderName = traderName;
        this.reason = reason;
        this.amount = amount;
        this.timestamp = timestamp;
        this.status = status;
    }
}
