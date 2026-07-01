package com.anokix.distributorapp.network.dto;

import java.util.List;

/**
 * data block of GET /api/distributor/traders.
 */
public class TradersData {
    public List<Trader> traders;
    public int total;
    public Summary summary;

    public static class Trader {
        public String id;
        public String name;
        public String business_name;
        public String trading_name;
        public String owner_full_name;
        public String phone_number;
        public String country_code;
        public String email;
        public String address;
        public String store_type;
        public String trader_code;
        public String status;
        public String registration_status;
        public String credit_status;
    }

    public static class Summary {
        public int total_traders;
        public String total_traders_month_change_label;
        public int active_traders;
        public String active_traders_percent_label;
        public int new_this_month;
        public String new_this_month_change_label;
        public int high_value_traders;
        public String high_value_traders_percent_label;
    }
}
