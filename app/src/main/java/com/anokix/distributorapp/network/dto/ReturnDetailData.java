package com.anokix.distributorapp.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Detail wrapper for {@code api/distributor/grns/get-by-id} (and the same shape returned by
 * the approve / reject actions) — the return with its {@code items[]}.
 */
public class ReturnDetailData {
    /** {@code return} is a Java keyword, so the JSON member is bound explicitly. */
    @SerializedName("return")
    public Return aReturn;

    public static class Return {
        public long id;
        public String grn_number;
        public Long source_grv_id;
        public long trader_id;
        public String trader_name;
        public String trader_phone;
        public String reason;
        public String reason_label;
        public String status;            // pending / approved / rejected
        public String raw_status;
        public int item_count;
        public int total_quantity;
        public String note;
        public String decision_note;
        public String credit_note_number;
        public double credit_note_total;
        public String approved_at;       // "yyyy-MM-dd HH:mm:ss"
        public String created_at;        // "dd/MM/yyyy HH:mm:ss"
        public List<Item> items;

        public String traderName() {
            return trader_name == null || trader_name.trim().isEmpty() ? "Trader" : trader_name;
        }
    }

    public static class Item {
        public long id;
        public long product_id;
        public String name;
        public int quantity;
        public double unit_cost;
        public double line_total;
        public String reason;
    }
}
