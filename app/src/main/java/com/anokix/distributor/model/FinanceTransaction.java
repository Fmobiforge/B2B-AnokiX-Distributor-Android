package com.anokix.distributor.model;

public class FinanceTransaction {
    public final String id;
    public final String title;
    public final String invoiceId;
    public final String date;
    public final String amount;
    public final boolean isIncome;
    public final String status;
    public final int iconRes;

    public FinanceTransaction(String id, String title, String invoiceId, String date,
                              String amount, boolean isIncome, String status, int iconRes) {
        this.id = id;
        this.title = title;
        this.invoiceId = invoiceId;
        this.date = date;
        this.amount = amount;
        this.isIncome = isIncome;
        this.status = status;
        this.iconRes = iconRes;
    }
}
