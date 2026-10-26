package com.anokix.distributorapp.network.dto;

import java.util.List;

/**
 * data block of GET /api/distributor/products/hc-library — one page of the Hirt &amp; Carter
 * product catalogue the backend mirrors locally.
 *
 * {@code available} is false when the integration is switched off; {@code library_ready} is
 * false while the first sync is still running. {@code brands} is the full brand list for the
 * filter, returned with every page.
 */
public class HcLibraryData {
    public boolean available;
    public boolean library_ready;
    public int total;
    public int page;
    public int per_page;
    public int total_pages;
    public String last_synced_at;
    public List<String> brands;
    public List<Item> items;

    public static class Item {
        public String gtin;
        public String brand;
        public String name;
        public String size;
        public String uom;
        public boolean has_images;
        public int image_count;
        public String thumb_url;
        /** The distributor already has a product with this barcode. */
        public boolean already_added;
    }
}
