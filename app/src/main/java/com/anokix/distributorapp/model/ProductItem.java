package com.anokix.distributorapp.model;

public class ProductItem {
    public final String id;
    public final String name;
    public final String sku;
    public final String category;
    public final String price;
    public final String stockStatus;
    /** Remote image URL; null/empty falls back to the placeholder icon. */
    public String imageUrl;

    // Extra list metadata — powers the category / brand / status filters and the card.
    public String brand = "";
    public String brandId = "";
    public String categoryId = "";
    public String parentCategoryId = "";
    public String parentCategory = "";
    /** Product record status from the API ("Active" / "Inactive"). */
    public String status = "";
    public String barcode = "";
    public int stockQty;
    public int reorderLevel;

    public ProductItem(String id, String name, String sku, String category,
                       String price, String stockStatus) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.category = category;
        this.price = price;
        this.stockStatus = stockStatus;
    }

    /** Status groups the portal styles differently (green / amber / grey). */
    public enum StatusKind { ACTIVE, PENDING, INACTIVE, OTHER }

    public static final String LABEL_ACTIVE = "Active";
    public static final String LABEL_PENDING = "Pending Review";
    public static final String LABEL_INACTIVE = "InActive";

    /**
     * The portal's status normalisation: "Pending", "pending_review", "Under Review" … are all
     * a pending review; "approved" / "enabled" count as active; "disabled" / "rejected" as
     * inactive. A blank status is active (the list mapper's default).
     */
    public StatusKind statusKind() {
        if (status == null || status.trim().isEmpty()) return StatusKind.ACTIVE;
        String s = status.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[\\s-]+", "_");
        switch (s) {
            case "pending":
            case "pending_review":
            case "pending_approval":
            case "awaiting_approval":
            case "under_review":
                return StatusKind.PENDING;
            case "active":
            case "approved":
            case "enabled":
                return StatusKind.ACTIVE;
            case "inactive":
            case "in_active":
            case "disabled":
            case "rejected":
                return StatusKind.INACTIVE;
            default:
                return StatusKind.OTHER;
        }
    }

    /** Badge text, as on the portal: "Active", "Pending Review", "InActive", or the raw status. */
    public String statusLabel() {
        switch (statusKind()) {
            case ACTIVE: return LABEL_ACTIVE;
            case PENDING: return LABEL_PENDING;
            case INACTIVE: return LABEL_INACTIVE;
            default:
                StringBuilder sb = new StringBuilder();
                for (String word : status.trim().replace('_', ' ').split("\\s+")) {
                    if (word.isEmpty()) continue;
                    if (sb.length() > 0) sb.append(' ');
                    sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
                }
                return sb.toString();
        }
    }

    public boolean isActive() {
        return statusKind() == StatusKind.ACTIVE;
    }

    public boolean isOutOfStock() {
        return stockQty <= 0;
    }

    /** In stock but at or below the reorder level the distributor configured. */
    public boolean isLowStock() {
        return stockQty > 0 && reorderLevel > 0 && stockQty <= reorderLevel;
    }
}
