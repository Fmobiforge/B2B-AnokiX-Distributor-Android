package com.anokix.distributorapp.network.dto;

import java.util.List;

/**
 * data block of GET {@code api/distributor/finances} — the distributor wallet ledger.
 * Holds the {@link Transaction} list plus a {@link Summary} block that drives the KPI cards.
 *
 * Each transaction carries a {@code type} (order_payment / invoice / payout / credit_note) and a
 * {@code status} (paid / pending / overdue / partial) that drive the badges and local filters.
 */
public class FinancesData {
    public List<Transaction> transactions;
    public Summary summary;

    public static class Transaction {
        public long id;
        public String reference;
        public String type;             // order_payment / invoice / payout / credit_note
        public String trader_name;
        public String trader_phone;
        public String order_number;
        public String description;       // "Order payment · ORD000029"
        public double amount;
        public String currency;          // "ZAR"
        public String status;            // paid / pending / overdue / partial
        public String payment_method;    // not yet returned by the API — reserved
        public String created_at;        // "2026-07-15 13:58:09"

        public String traderName() {
            return trader_name == null || trader_name.trim().isEmpty() ? "Trader" : trader_name;
        }
    }

    public static class Summary {
        public double received;
        public double received_month;
        public double pending;
        public double commission_month;
        public double overdue;      // reserved — API does not yet return it
        public int count;
    }
}
