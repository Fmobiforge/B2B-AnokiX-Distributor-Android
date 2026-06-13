package com.anokix.distributor.model;

public class PromotionItem {
    public final String id;
    public final String title;
    public final String status;
    public final String impressions;
    public final String ctr;
    public final int bannerBgRes;
    public final String bannerLabel;

    public PromotionItem(String id, String title, String status, String impressions,
                         String ctr, int bannerBgRes, String bannerLabel) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.impressions = impressions;
        this.ctr = ctr;
        this.bannerBgRes = bannerBgRes;
        this.bannerLabel = bannerLabel;
    }
}
