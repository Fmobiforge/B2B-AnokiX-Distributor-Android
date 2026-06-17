package com.anokix.distributor.network.dto;

import java.util.List;

/**
 * data block of GET /api/distributor/dashboard. Wraps a single "dashboard" object.
 * Mirrors the full web-portal payload: summary KPI cards (with sparklines), the
 * overview tiles, revenue overview line chart, top selling categories and recent orders.
 */
public class DashboardData {
    public Dashboard dashboard;

    public static class Dashboard {
        public Meta meta;
        public List<SummaryCard> summary_cards;
        public List<OverviewItem> overview;
        public RevenueOverview revenue_overview;
        public TopCategories top_selling_categories;
        public RecentOrders recent_orders;
    }

    public static class Meta {
        public String from_date;
        public String to_date;
        public String date_range_label;
        public String compare_from_date;
        public String compare_to_date;
        public String compare_period_label;
        public Currency currency;
        public boolean is_dummy_data;
    }

    public static class Currency {
        public String code;
        public String symbol;
    }

    /** One of the four top KPI graph cards (Total Revenue, Total Orders, ...). */
    public static class SummaryCard {
        public String key;
        public String label;
        public double value;
        public String formatted_value;
        public boolean is_money;
        public Trend trend;
        public String compare_label;
        public List<Float> sparkline;
        public String color;
    }

    public static class OverviewItem {
        public String key;
        public String label;
        public long value;
        public String action_label;
        public String action_route;
    }

    public static class RevenueOverview {
        public String title;
        public double total;
        public String formatted_total;
        public Trend trend;
        public Chart chart;
    }

    public static class Trend {
        public String direction;
        public double percentage;
        public String label;
    }

    public static class Chart {
        public List<String> labels;
        public List<Float> values;
        public double max_value;
        public String y_axis_max;
    }

    public static class TopCategories {
        public String title;
        public List<CategoryItem> items;
        public double total;
        public String formatted_total;
    }

    public static class CategoryItem {
        public String name;
        public double amount;
        public String formatted_amount;
        public double percentage;
        public String color;
    }

    public static class RecentOrders {
        public String title;
        public List<RecentOrder> items;
        public int total;
    }

    public static class RecentOrder {
        public String order_id;
        public String trader;
        public double amount;
        public String formatted_amount;
        public String status;
        public String status_label;
        public String status_color;
        public String date;
        public String formatted_date;
    }
}
