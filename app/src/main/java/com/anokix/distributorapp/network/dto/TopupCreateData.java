package com.anokix.distributorapp.network.dto;

/**
 * data block of POST /api/common/topup/create — a freshly opened PayCloud Hosted
 * Checkout for funding the anokiX wallet. The app opens {@link #pay_url} in the
 * browser and then polls {@link TopupStatusData} with {@link #merchant_order_no}.
 */
public class TopupCreateData {
    public String merchant_order_no;
    public String pay_url;
    public double amount;
    public String currency;
}
