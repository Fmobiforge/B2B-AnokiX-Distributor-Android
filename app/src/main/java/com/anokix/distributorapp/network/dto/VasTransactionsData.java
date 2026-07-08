package com.anokix.distributorapp.network.dto;

import java.util.List;

/** {@code data} block of {@code GET api/common/vas/transactions}. */
public class VasTransactionsData {
    public List<Transaction> transactions;
    public Summary summary;
    public Pagination pagination;

    public static class Transaction {
        public String id;
        public String msisdn;
        public String product_id;
        public String sku;
        public String name;
        public String category;
        public String amount;
        public String currency;
        public String commission;
        public String status;
        public String error;
        public String created_at;
        public String updated_at;
    }

    /** Header KPI figures for the VAS dashboard. */
    public static class Summary {
        public double sales_today;
        public double count_today;
        public double commission_month;
        public double wallet_balance;
    }

    public static class Pagination {
        public int page;
        public int per_page;
        public int total;
        public int total_pages;
    }
}
