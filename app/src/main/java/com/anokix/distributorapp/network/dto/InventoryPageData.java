package com.anokix.distributorapp.network.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * data block of GET /api/distributor/inventory/page — everything the Inventory screen
 * renders in one call: KPI summary, stock rows, the status donut, recent movements and
 * the warehouse list used by the transfer / receive forms.
 *
 * Field names mirror the JSON exactly (the rows block is camelCase, the rest snake_case).
 */
public class InventoryPageData {

    public Summary summary;
    public List<Row> rows;
    public List<ChartSlice> chart;
    public List<StockMovement> movements;
    public List<Warehouse> warehouses;

    public static class Summary {
        public double total_stock_value;
        public long total_units;
        public int low_stock_count;
        public int out_of_stock_count;
        public int movements_this_week;
    }

    public static class Row {
        public long id;
        public Long stockId;
        public long productId;
        public String name;
        public String sku;
        public String barcode;
        public String category;
        public String categoryKey;
        public String brand;
        public String brandKey;
        public String warehouse;
        public String warehouseKey;
        public long units;
        public long capacity;
        public double stockValue;
        /** "in_stock" / "low_stock" / "out_of_stock". */
        public String stockStatus;
        /** Available / Reserved / Packed / Damaged / Expired counts. */
        public Map<String, Integer> breakdown;
        public String batchNumber;
        public String expiryDate;
        public int unitsPerCase;
        public String updatedAt;
        /** Server-picked swatch for the product tile, e.g. "#dc2626". */
        public String imageColor;

        public Map<String, Integer> breakdownOrEmpty() {
            return breakdown == null ? new LinkedHashMap<>() : breakdown;
        }

        public int breakdownValue(String key) {
            Integer value = breakdownOrEmpty().get(key);
            return value == null ? 0 : value;
        }

        public boolean isLowStock() {
            return "low_stock".equalsIgnoreCase(stockStatus);
        }

        public boolean isOutOfStock() {
            return "out_of_stock".equalsIgnoreCase(stockStatus) || units <= 0;
        }

        public boolean hasDamaged() {
            return breakdownValue("Damaged") > 0;
        }

        public boolean hasExpired() {
            return breakdownValue("Expired") > 0;
        }

        /** Human label for the status badge. */
        public String statusLabel() {
            if (isOutOfStock()) {
                return "Out of Stock";
            }
            if (isLowStock()) {
                return "Low Stock";
            }
            return "In Stock";
        }
    }

    public static class ChartSlice {
        public String type;
        public double value;
        public String color;
    }

    public static class Warehouse {
        public String id;
        public String name;
        public String code;
        public String address;
        public String active;
    }
}
