package com.anokix.distributorapp.network.dto;

import java.util.List;

/**
 * data block of GET /api/distributor/promotions. Holds the campaign list plus a
 * {@code summary} block whose {@code summary_cards[]} feed the 6 KPI cards at the
 * top of the Promotions screen. Only the fields the mobile screen renders are
 * mapped — the heavy nested {@code selected_products} payload is ignored.
 */
public class PromotionsData {
    public List<Promotion> promotions;
    public int total;
    public Summary summary;

    public static class Promotion {
        public String id;
        public String status;
        public String campaign_type;
        public String name;
        public String start_date;     // yyyy-MM-dd
        public String end_date;
        public double budget;
        public double daily_budget;
        public double products_cost;
        public long reach;
        public long clicks;
        public double revenue;
        public double ctr;
        public BannerFile banner_file;
    }

    public static class BannerFile {
        public String file_url;
    }

    public static class Summary {
        public Currency currency;
        public List<SummaryCard> summary_cards;
    }

    public static class Currency {
        public String code;
        public String symbol;
    }

    public static class SummaryCard {
        public String key;
        public String label;
        public String formatted_value;
        public boolean is_money;
        public Trend trend;
    }

    public static class Trend {
        public String direction;   // "up" | "down"
        public double percentage;
        public String label;       // e.g. "↑ 100.0% vs Last Month"
    }
}
