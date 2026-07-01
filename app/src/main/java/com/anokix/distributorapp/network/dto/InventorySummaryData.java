package com.anokix.distributorapp.network.dto;

/**
 * data block of GET /api/distributor/inventory/summary.
 */
public class InventorySummaryData {
    public Summary summary;

    public static class Summary {
        public double total_stock_value;
        public long total_units;
        public int low_stock_count;
        public int out_of_stock_count;
        public int movements_this_week;
    }
}
