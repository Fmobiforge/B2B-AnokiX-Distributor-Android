package com.anokix.distributor.model;

public class DeliveryItem {
    public final String id;
    public final String traderName;
    public final String driverName;
    public final String eta;
    public final String status;

    public DeliveryItem(String id, String traderName, String driverName, String eta,
                        String status) {
        this.id = id;
        this.traderName = traderName;
        this.driverName = driverName;
        this.eta = eta;
        this.status = status;
    }
}
