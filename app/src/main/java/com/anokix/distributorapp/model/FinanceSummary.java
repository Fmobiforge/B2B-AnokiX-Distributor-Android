package com.anokix.distributorapp.model;

public class FinanceSummary {
    public final String title;
    public final String amount;
    public final String trend;
    public final boolean trendUp;
    public final int valueColorRes;
    public final int trendColorRes;

    public FinanceSummary(String title, String amount, String trend, boolean trendUp,
                          int valueColorRes, int trendColorRes) {
        this.title = title;
        this.amount = amount;
        this.trend = trend;
        this.trendUp = trendUp;
        this.valueColorRes = valueColorRes;
        this.trendColorRes = trendColorRes;
    }
}
