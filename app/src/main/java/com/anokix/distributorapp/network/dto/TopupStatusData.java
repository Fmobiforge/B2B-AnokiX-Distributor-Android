package com.anokix.distributorapp.network.dto;

/**
 * data block of GET /api/common/topup/status — polled after returning from the PayCloud
 * checkout. {@code status=paid} only means the card was charged; the money is in the
 * wallet when {@code credit_status=credited}.
 */
public class TopupStatusData {
    public String merchant_order_no;
    public String trade_no;
    public double amount;
    public String currency;
    /** pending | paid | failed | cancelled */
    public String status;
    /** none | pending | credited | failed */
    public String credit_status;
    public String credited_at;
    public String created_at;

    public boolean isCredited() {
        return "credited".equalsIgnoreCase(credit_status);
    }

    /** Terminal failure — nothing more to wait for. */
    public boolean isFailed() {
        return "failed".equalsIgnoreCase(status)
                || "cancelled".equalsIgnoreCase(status)
                || "failed".equalsIgnoreCase(credit_status);
    }
}
