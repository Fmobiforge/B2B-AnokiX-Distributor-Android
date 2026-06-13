package com.anokix.distributor.network.dto;

import java.util.List;

/**
 * data block of GET /api/distributor/dashboard. Wraps a single "dashboard" object.
 */
public class DashboardData {
    public Dashboard dashboard;

    public static class Dashboard {
        public Meta meta;
        public List<OverviewItem> overview;
        public RevenueOverview revenue_overview;
        public TopCategories top_selling_categories;
        public RecentOrders recent_orders;
    }

    public static class Meta {
        public String date_range_label;
        public String compare_period_label;
    }

    public static class OverviewItem {
        public String key;
        public String label;
        public long value;
    }

    public static class RevenueOverview {
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
    }

    public static class TopCategories {
        public String title;
        public List<CategoryItem> items;
    }

    public static class CategoryItem {
        public String name;
        public String formatted_amount;
        public double percentage;
        public String color;
    }

    public static class RecentOrders {
        public String title;
        public List<RecentOrder> items;
    }

    public static class RecentOrder {
        public String order_id;
        public String trader;
        public String formatted_amount;
        public String status_label;
        public String status_color;
        public String formatted_date;
    }
}
