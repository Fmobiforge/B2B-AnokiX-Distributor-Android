package com.anokix.distributor.model;

/** UI model for a single campaign row on the Promotions screen. */
public class PromotionItem {
    public final String id;
    public final String name;
    public final String campaignType;  // raw, e.g. "best_deal" (used by the local filter)
    public final String typeLabel;     // e.g. "Best Deal"
    public final String status;        // raw, e.g. "pending_approval" (used by the local filter)
    public final String statusLabel;   // e.g. "Pending Approval"
    public final String dateRange;     // e.g. "15 Jun – 30 Jun 2026"
    public final String budget;        // e.g. "R3,000"
    public final String reach;
    public final String clicks;
    public final String ctr;           // e.g. "0.0%"

    public PromotionItem(String id, String name, String campaignType, String typeLabel,
                         String status, String statusLabel, String dateRange, String budget,
                         String reach, String clicks, String ctr) {
        this.id = id;
        this.name = name;
        this.campaignType = campaignType;
        this.typeLabel = typeLabel;
        this.status = status;
        this.statusLabel = statusLabel;
        this.dateRange = dateRange;
        this.budget = budget;
        this.reach = reach;
        this.clicks = clicks;
        this.ctr = ctr;
    }
}
