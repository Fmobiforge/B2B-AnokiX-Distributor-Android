package com.anokix.distributorapp.network.dto;

/**
 * One row of the stock ledger — used by the inventory page ("movements") and by
 * GET /api/distributor/inventory/history.
 */
public class StockMovement {
    public String id;
    public String warehouse_id;
    public String product_id;
    public String batch_id;
    /** "receive" / "adjust" / "transfer_in" / "transfer_out" / … */
    public String movement_type;
    public String inventory_status;
    public String quantity;
    public String from_status;
    public String to_status;
    public String unit_cost;
    public String reference_type;
    public String notes;
    public String created_at;
    public String warehouse_name;
    public String product_name;
    public String product_sku;
    public String batch_number;
    public String user_email;

    /** "receive" → "Received", "transfer_out" → "Transfer Out", … */
    public String movementLabel() {
        if (movement_type == null || movement_type.isEmpty()) {
            return "Movement";
        }
        if ("receive".equalsIgnoreCase(movement_type)) {
            return "Received";
        }
        String[] parts = movement_type.split("[_\\s]+");
        StringBuilder label = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (label.length() > 0) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return label.toString();
    }
}
