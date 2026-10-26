package com.anokix.distributorapp.network.dto;

import java.io.Serializable;
import java.util.List;

/**
 * data block of GET /api/distributor/products/external-lookup?barcode=… — the full
 * Hirt &amp; Carter record for one GTIN.
 *
 * {@code available} is false when the catalogue service cannot be reached; {@code found} is
 * false (with a null product) when Hirt &amp; Carter has no data for the barcode.
 *
 * {@link Product} is Serializable so the product screen can receive it as an Intent extra.
 */
public class HcLookupData {
    public boolean available;
    public boolean found;
    public Product product;

    public static class Product implements Serializable {
        public String gtin;
        public List<String> gtins;
        public String name;
        public String brand;
        public String size;
        public String uom;
        public String article_number;
        public String status_text;
        public boolean has_images;
        public List<Image> images;
        public List<Content> content;
    }

    public static class Image implements Serializable {
        public Integer index;
        /** Identifier sent back as {@code hc_image} so the server imports this picture. */
        public String file_name;
        public String mime_type;
        public String angle_name;
        public String angle_code;
        public boolean previewable;
        public String preview_url;
        public boolean importable;
    }

    /** One row of the "Product content" table (sub-brand, dimensions, marketing copy, …). */
    public static class Content implements Serializable {
        public String heading;
        public String name;
        public String value;
    }
}
